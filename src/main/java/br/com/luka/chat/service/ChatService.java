package br.com.luka.chat.service;

import br.com.luka.chat.model.Mensagem;
import br.com.luka.chat.model.MensagemRepository;
import br.com.luka.chat.model.Usuario;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras do chat: conectar, desconectar, enviar, broadcast e historico.
 * Nao conhece Socket nem SQL; recebe as dependencias pelo construtor.
 */
public class ChatService {

    public static final String PREFIXO_SISTEMA = "* ";

    private final MensagemRepository repository;
    private final ClientRegistry registry;
    private final Clock relogio;
    private final int tamanhoHistorico;

    /**
     * Trava de ordenacao: salvar + broadcast formam uma secao critica unica,
     * para que a ordem do banco seja a mesma vista pelos clientes.
     * Trade-off: envios de mensagens sao serializados (menos paralelismo).
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
        return true;
    }

    public void desconectar(ConexaoCliente conexao) {
        if (registry.remover(conexao)) {
            broadcast(PREFIXO_SISTEMA + conexao.getUsuario().getNome() + " saiu do chat");
        }
    }

    /** Cria, valida, salva e transmite. Lanca MensagemInvalidaException. */
    public Mensagem enviarMensagem(Usuario remetente, String texto) {
        Mensagem nova = new Mensagem(remetente, texto, LocalDateTime.now(relogio));
        synchronized (travaEnvio) {
            Mensagem salva = repository.salvar(nova);
            broadcast(salva.formatar());
            return salva;
        }
    }

    /**
     * Envia a linha para um snapshot das conexoes, sem lock do registry.
     * Conexao que falha e removida, sem interromper as demais.
     */
    public void broadcast(String linha) {
        for (ConexaoCliente conexao : registry.snapshot()) {
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
    }

    public List<Mensagem> buscarHistorico() {
        return repository.buscarUltimas(tamanhoHistorico);
    }

    public List<Usuario> usuariosOnline() {
        return registry.usuariosOnline();
    }
}

