package br.com.luka.chat.server;

import br.com.luka.chat.persistence.ConnectionFactory;
import br.com.luka.chat.persistence.JdbcMensagemRepository;
import br.com.luka.chat.service.ChatService;
import br.com.luka.chat.service.ClientRegistry;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Clock;

/**
 * Aceita conexoes e cria uma thread por cliente. O main e o unico ponto
 * de montagem das implementacoes concretas.
 */
public class Servidor {

    public static final int PORTA_PADRAO = 5000;
    public static final int TAMANHO_HISTORICO = 20;

    private final int porta;
    private final ChatService chatService;
    private volatile ServerSocket serverSocket;

    public Servidor(int porta, ChatService chatService) {
        this.porta = porta;
        this.chatService = chatService;
    }

    public void iniciar() throws IOException {
        serverSocket = new ServerSocket(porta);
        System.out.println("Servidor iniciado na porta " + serverSocket.getLocalPort());

        while (!serverSocket.isClosed()) {
            Socket conexao;
            try {
                conexao = serverSocket.accept();
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    break; // encerramento solicitado
                }
                System.out.println("Falha no accept: " + e.getMessage());
                continue;
            }
            try {
                ClienteHandler handler = new ClienteHandler(conexao, chatService);
                Thread thread = new Thread(handler, "cliente-" + conexao.getPort());
                thread.start();
            } catch (IOException e) {
                System.out.println("Falha ao preparar cliente: " + e.getMessage());
                conexao.close();
            }
        }
    }

    public void parar() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    public static void main(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : PORTA_PADRAO;

        ConnectionFactory fabrica = new ConnectionFactory("jdbc:h2:./data/chat", "sa", "");
        fabrica.criarTabelas();

        ChatService chatService = new ChatService(
            new JdbcMensagemRepository(fabrica),
            new ClientRegistry(),
            Clock.systemDefaultZone(),
            TAMANHO_HISTORICO
        );

        new Servidor(porta, chatService).iniciar();
    }
}
