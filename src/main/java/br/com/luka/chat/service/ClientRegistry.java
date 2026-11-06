package br.com.luka.chat.service;

import br.com.luka.chat.model.Mensagem;
import br.com.luka.chat.model.Usuario;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dono da colecao compartilhada de conexoes e dos grupos de conversa.
 *
 * Concorrencia:
 * - CopyOnWriteArrayList para conexoes: o broadcast (leitura) e muito
 *   mais frequente que conectar/desconectar (escrita).
 * - ConcurrentHashMap com Sets concorrentes para os grupos e seus membros.
 */
public class ClientRegistry {

    private final CopyOnWriteArrayList<ConexaoCliente> conexoes =
        new CopyOnWriteArrayList<>();
    private final Map<String, Set<Usuario>> grupos = new ConcurrentHashMap<>();
    private final Object travaCadastro = new Object();

    /** @return false se o nickname ja estiver em uso. */
    public boolean adicionar(ConexaoCliente conexao) {
        synchronized (travaCadastro) {
            if (buscarPorUsuario(conexao.getUsuario()).isPresent()) {
                return false;
            }
            conexoes.add(conexao);
            return true;
        }
    }

    public boolean remover(ConexaoCliente conexao) {
        return conexoes.remove(conexao);
    }

    /** Copia imutavel para iterar sem segurar nenhum lock. */
    public List<ConexaoCliente> snapshot() {
        return List.copyOf(conexoes);
    }

    public int quantidade() {
        return conexoes.size();
    }

    public Optional<ConexaoCliente> buscarPorUsuario(Usuario usuario) {
        return conexoes.stream()
            .filter(c -> c.getUsuario().equals(usuario))
            .findFirst();
    }

    public Optional<ConexaoCliente> buscarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return Optional.empty();
        }
        String limpo = nome.startsWith("@") ? nome.substring(1).trim() : nome.trim();
        return conexoes.stream()
            .filter(c -> c.getUsuario().getNome().equalsIgnoreCase(limpo))
            .findFirst();
    }

    public List<Usuario> usuariosOnline() {
        return conexoes.stream().map(ConexaoCliente::getUsuario).toList();
    }

    /** Normaliza o nome do grupo (ex: "trabalho" -> "#trabalho") e adiciona o membro atomicamente. */
    public String adicionarAoGrupo(String nomeGrupo, Usuario usuario) {
        String chave = normalizarGrupo(nomeGrupo);
        grupos.computeIfAbsent(chave, k -> ConcurrentHashMap.newKeySet()).add(usuario);
        return chave;
    }

    /** Cria ou atualiza um grupo adicionando o criador e todos os membros selecionados. */
    public String criarGrupoComMembros(String nomeGrupo, Usuario criador, Collection<Usuario> convidados) {
        String chave = normalizarGrupo(nomeGrupo);
        Set<Usuario> membros = grupos.computeIfAbsent(chave, k -> ConcurrentHashMap.newKeySet());
        membros.add(criador);
        if (convidados != null) {
            membros.addAll(convidados);
        }
        return chave;
    }

    /** Adiciona novos participantes a um grupo ja existente. */
    public String adicionarMembrosAoGrupo(String nomeGrupo, Collection<Usuario> novosMembros) {
        String chave = normalizarGrupo(nomeGrupo);
        Set<Usuario> membros = grupos.computeIfAbsent(chave, k -> ConcurrentHashMap.newKeySet());
        if (novosMembros != null) {
            membros.addAll(novosMembros);
        }
        return chave;
    }

    /**
     * Remove um usuario do grupo de forma atomica. Se o grupo ficar vazio,
     * ele e removido automaticamente do mapa (computeIfPresent).
     */
    public boolean sairDoGrupo(String nomeGrupo, Usuario usuario) {
        String chave = normalizarGrupo(nomeGrupo);
        final boolean[] removido = {false};
        grupos.computeIfPresent(chave, (k, membros) -> {
            removido[0] = membros.remove(usuario);
            return membros.isEmpty() ? null : membros;
        });
        return removido[0];
    }

    /**
     * Exclui o grupo completamente e devolve o conjunto de usuarios que faziam
     * parte dele no momento da exclusao.
     */
    public Set<Usuario> apagarGrupo(String nomeGrupo) {
        String chave = normalizarGrupo(nomeGrupo);
        Set<Usuario> antigos = grupos.remove(chave);
        return antigos == null ? Set.of() : Set.copyOf(antigos);
    }

    public Set<Usuario> membrosDoGrupo(String nomeGrupo) {
        String chave = normalizarGrupo(nomeGrupo);
        Set<Usuario> membros = grupos.get(chave);
        return membros == null ? Set.of() : Set.copyOf(membros);
    }

    public List<String> gruposDisponiveis() {
        return grupos.keySet().stream().sorted().toList();
    }

    /** Devolve apenas os grupos dos quais o usuario faz parte. */
    public List<String> gruposDoUsuario(Usuario usuario) {
        return grupos.entrySet().stream()
            .filter(e -> e.getValue().contains(usuario))
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
    }

    public static String normalizarGrupo(String nomeGrupo) {
        if (nomeGrupo == null || nomeGrupo.isBlank()) {
            return Mensagem.DESTINO_GERAL;
        }
        String limpo = nomeGrupo.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        return limpo.startsWith("#") ? limpo : "#" + limpo;
    }
}
