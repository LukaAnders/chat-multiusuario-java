package br.com.luka.chat.service;

import br.com.luka.chat.model.Mensagem;
import br.com.luka.chat.model.MensagemRepository;
import br.com.luka.chat.model.Usuario;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regras do chat: conectar, desconectar, enviar no geral, enviar em grupos,
 * enviar mensagens privadas e recuperar historicos por conversa.
 * Nao conhece Socket nem SQL; recebe as dependencias pelo construtor.
 */
public class ChatService {

    public static final String PREFIXO_SISTEMA = "* ";
    public static final String PREFIXO_GRUPO = "~GRUPO|";
    public static final String PREFIXO_PRIVADA = "~PRIV|";
    public static final String PREFIXO_CONTATOS = "~CONTATOS|";

    private final MensagemRepository repository;
    private final ClientRegistry registry;
    private final Clock relogio;
    private final int tamanhoHistorico;

    /**
     * Trava de ordenacao: salvar + broadcast formam uma secao critica unica,
     * para que a ordem do banco seja a mesma vista pelos clientes.
     */
    private final Object travaEnvio = new Object();

    public ChatService(MensagemRepository repository, ClientRegistry registry,
                       Clock relogio, int tamanhoHistorico) {
        this.repository = repository;
        this.registry = registry;
        this.relogio = relogio;
        this.tamanhoHistorico = tamanhoHistorico;
    }

    /** @return false se o nickname ja estiver em uso. */
    public boolean conectar(ConexaoCliente conexao) {
        if (!registry.adicionar(conexao)) {
            return false;
        }
        for (Mensagem antiga : buscarHistorico()) {
            conexao.enviar(antiga.formatar());
        }
        broadcast(PREFIXO_SISTEMA + conexao.getUsuario().getNome() + " entrou no chat");
        notificarContatosEGrupos();
        return true;
    }

    public void desconectar(ConexaoCliente conexao) {
        if (registry.remover(conexao)) {
            broadcast(PREFIXO_SISTEMA + conexao.getUsuario().getNome() + " saiu do chat");
            notificarContatosEGrupos();
        }
    }

    /** Envia mensagem na sala publica #geral. */
    public Mensagem enviarMensagem(Usuario remetente, String texto) {
        Mensagem nova = new Mensagem(null, remetente, Mensagem.DESTINO_GERAL, texto, LocalDateTime.now(relogio));
        synchronized (travaEnvio) {
            Mensagem salva = repository.salvar(nova);
            broadcast(salva.formatar());
            return salva;
        }
    }

    /** Envia mensagem para um grupo especifico (ex: #trabalho). */
    public Mensagem enviarMensagemGrupo(Usuario remetente, String nomeGrupo, String texto) {
        String grupo = ClientRegistry.normalizarGrupo(nomeGrupo);
        if (grupo.equals(Mensagem.DESTINO_GERAL)) {
            return enviarMensagem(remetente, texto);
        }

        registry.adicionarAoGrupo(grupo, remetente);
        Mensagem nova = new Mensagem(null, remetente, grupo, texto, LocalDateTime.now(relogio));
        synchronized (travaEnvio) {
            Mensagem salva = repository.salvar(nova);
            String linha = PREFIXO_GRUPO + grupo + "|" + salva.formatar();
            Set<Usuario> membros = registry.membrosDoGrupo(grupo);
            for (ConexaoCliente conexao : registry.snapshot()) {
                if (membros.contains(conexao.getUsuario())) {
                    entregarComSeguranca(conexao, linha);
                }
            }
            return salva;
        }
    }

    /** Envia mensagem privada entre dois usuarios. */
    public Mensagem enviarMensagemPrivada(Usuario remetente, String nomeDestinatario, String texto) {
        String limpo = nomeDestinatario.startsWith("@")
            ? nomeDestinatario.substring(1).trim()
            : nomeDestinatario.trim();
        Usuario destinoUser = new Usuario(limpo);
        String chaveDestino = "@" + destinoUser.getNome().toLowerCase(Locale.ROOT);

        Mensagem nova = new Mensagem(null, remetente, chaveDestino, texto, LocalDateTime.now(relogio));
        synchronized (travaEnvio) {
            Mensagem salva = repository.salvar(nova);
            String linha = PREFIXO_PRIVADA + remetente.getNome() + "|" + destinoUser.getNome() + "|" + salva.formatar();

            registry.buscarPorUsuario(remetente).ifPresent(c -> entregarComSeguranca(c, linha));
            if (!remetente.equals(destinoUser)) {
                registry.buscarPorUsuario(destinoUser).ifPresent(c -> entregarComSeguranca(c, linha));
            }
            return salva;
        }
    }

    /** Cria ou adiciona o cliente a um grupo e envia o historico daquele grupo. */
    public String entrarNoGrupo(ConexaoCliente conexao, String nomeGrupo) {
        String grupo = registry.adicionarAoGrupo(nomeGrupo, conexao.getUsuario());
        notificarContatosEGrupos();
        enviarHistoricoConversa(conexao, grupo);
        return grupo;
    }

    /** Envia o historico de uma conversa (#grupo ou @usuario) para o cliente solicitante. */
    public void enviarHistoricoConversa(ConexaoCliente conexao, String alvo) {
        if (alvo == null || alvo.isBlank()) {
            return;
        }
        String limpo = alvo.trim();
        if (limpo.startsWith("@")) {
            Usuario outro = new Usuario(limpo.substring(1));
            List<Mensagem> privadas = repository.buscarPrivadas(conexao.getUsuario(), outro, tamanhoHistorico);
            for (Mensagem m : privadas) {
                String destNome = m.getDestino().substring(1);
                String linha = PREFIXO_PRIVADA + m.getRemetente().getNome() + "|" + destNome + "|" + m.formatar();
                entregarComSeguranca(conexao, linha);
            }
        } else {
            String grupo = ClientRegistry.normalizarGrupo(limpo);
            List<Mensagem> mensagens = repository.buscarPorDestino(grupo, tamanhoHistorico);
            for (Mensagem m : mensagens) {
                if (grupo.equals(Mensagem.DESTINO_GERAL)) {
                    entregarComSeguranca(conexao, m.formatar());
                } else {
                    entregarComSeguranca(conexao, PREFIXO_GRUPO + grupo + "|" + m.formatar());
                }
            }
        }
    }

    /** Envia para todos a lista de usuarios online e grupos disponiveis. */
    public void notificarContatosEGrupos() {
        String usuarios = registry.usuariosOnline().stream()
            .map(Usuario::getNome)
            .collect(Collectors.joining(","));
        String grupos = String.join(",", registry.gruposDisponiveis());
        broadcast(PREFIXO_CONTATOS + usuarios + "|" + grupos);
    }

    /**
     * Envia a linha para um snapshot das conexoes, sem lock do registry.
     * Conexao que falha e removida, sem interromper as demais.
     */
    public void broadcast(String linha) {
        for (ConexaoCliente conexao : registry.snapshot()) {
            entregarComSeguranca(conexao, linha);
        }
    }

    private void entregarComSeguranca(ConexaoCliente conexao, String linha) {
        boolean entregue;
        try {
            entregue = conexao.enviar(linha);
        } catch (RuntimeException e) {
            entregue = false;
        }
        if (!entregue) {
            registry.remover(conexao);
        }
    }

    public List<Mensagem> buscarHistorico() {
        return repository.buscarUltimas(tamanhoHistorico);
    }

    public List<Usuario> usuariosOnline() {
        return registry.usuariosOnline();
    }
}
