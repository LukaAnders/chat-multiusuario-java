package br.com.luka.chat.server;

import br.com.luka.chat.model.MensagemInvalidaException;
import br.com.luka.chat.model.PersistenciaException;
import br.com.luka.chat.model.Usuario;
import br.com.luka.chat.service.ChatService;
import br.com.luka.chat.service.ConexaoCliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Atende UMA conexao em sua propria thread. Le linhas e delega ao
 * ChatService. Nao contem SQL nem regras de negocio.
 */
public class ClienteHandler implements Runnable, ConexaoCliente {

    private final Socket socket;
    private final ChatService chatService;
    private final PrintWriter saida;
    private volatile Usuario usuario;

    public ClienteHandler(Socket socket, ChatService chatService) throws IOException {
        this.socket = socket;
        this.chatService = chatService;
        this.saida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
    }

    @Override
    public Usuario getUsuario() {
        return usuario;
    }

    /**
     * Chamado por varias threads durante o broadcast: synchronized evita
     * que linhas de mensagens diferentes se intercalem no mesmo stream.
     */
    @Override
    public synchronized boolean enviar(String linha) {
        saida.println(linha);
        return !saida.checkError();
    }

    @Override
    public void run() {
        boolean conectado = false;
        try (BufferedReader entrada = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            conectado = identificar(entrada);
            if (!conectado) {
                return;
            }
            System.out.println(usuario + " conectado.");

            String linha;
            while ((linha = entrada.readLine()) != null) {
                if (Protocolo.ehSair(linha)) {
                    break;
                }
                if (Protocolo.ehComando(linha)) {
                    enviar(Protocolo.erro("Comando desconhecido. Use /sair."));
                    continue;
                }
                tratarMensagem(linha);
            }
        } catch (IOException e) {
            // Desconexao abrupta: segue para o finally sem afetar os demais.
            System.out.println("Conexao perdida com " + usuario + ": " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Erro inesperado com " + usuario + ": " + e);
        } finally {
            if (conectado) {
                chatService.desconectar(this);
                System.out.println(usuario + " desconectado.");
            }
            fecharSocket();
        }
    }

    /** Handshake: le nicknames ate um valido e livre. */
    private boolean identificar(BufferedReader entrada) throws IOException {
        String nome;
        while ((nome = entrada.readLine()) != null) {
            try {
                usuario = new Usuario(nome);
            } catch (IllegalArgumentException e) {
                enviar(Protocolo.erro(e.getMessage() + " Digite outro nome:"));
                continue;
            }
            if (chatService.conectar(this)) {
                return true;
            }
            enviar(Protocolo.erro("Nome em uso. Digite outro nome:"));
        }
        return false;
    }

    private void tratarMensagem(String linha) {
        try {
            chatService.enviarMensagem(usuario, linha);
        } catch (MensagemInvalidaException e) {
            enviar(Protocolo.erro(e.getMessage())); // so para o remetente
        } catch (PersistenciaException e) {
            System.out.println("Falha no banco: " + e.getCause());
            enviar(Protocolo.erro("Mensagem nao enviada: falha ao salvar."));
        }
    }

    public void fecharSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Falha ao fechar socket: " + e.getMessage());
        }
    }
}
