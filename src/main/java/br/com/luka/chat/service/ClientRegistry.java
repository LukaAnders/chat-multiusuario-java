package br.com.luka.chat.service;

import br.com.luka.chat.model.Mensagem;
import br.com.luka.chat.model.Usuario;

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
 * - ConcurrentHashMap com Sets concorrentes para os grupos.
 */
public class ClientRegistry {

    private final CopyOnWriteArrayList<ConexaoCliente> conexoes =
        new CopyOnWriteArrayList<>();
    private final Map<String, Set<Usuario>> grupos = new ConcurrentHashMap<>();
    private final Object travaCadastro = new Object();

    public ClientRegistry() {
        grupos.put(Mensagem.DESTINO_GERAL, ConcurrentHashMap.newKeySet());
    }

    /** @return false se o nickname ja estiver em uso. */
    public boolean adicionar(ConexaoCliente conexao) {
        synchronized (travaCadastro) {
            if (buscarPorUsuario(conexao.getUsuario()).isPresent()) {
                return false;
            }
            conexoes.add(conexao);
            adicionarAoGrupo(Mensagem.DESTINO_GERAL, conexao.getUsuario());
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

    public Set<Usuario> membrosDoGrupo(String nomeGrupo) {
        String chave = normalizarGrupo(nomeGrupo);
        Set<Usuario> membros = grupos.get(chave);
        return membros == null ? Set.of() : Set.copyOf(membros);
    }

    public List<String> gruposDisponiveis() {
        return grupos.keySet().stream().sorted().toList();
    }

    public List<String> gruposDoUsuario(Usuario usuario) {
        return grupos.entrySet().stream()
            .filter(e -> e.getKey().equals(Mensagem.DESTINO_GERAL) || e.getValue().contains(usuario))
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
