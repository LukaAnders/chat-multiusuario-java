package br.com.luka.chat.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Gerencia a conexao TCP com o servidor para clientes visuais.
 * Isola sockets e streams da camada de apresentacao (GUI).
 */
public class ConexaoRedeCliente implements Closeable {

    private final String host;
    private final int porta;
    private final Consumer<String> ouvinteMensagens;
    private final Consumer<String> ouvinteStatus;

    private Socket socket;
    private PrintWriter escritor;
    private Thread threadLeitura;
    private volatile boolean conectado;

    public ConexaoRedeCliente(String host, int porta,
                              Consumer<String> ouvinteMensagens,
                              Consumer<String> ouvinteStatus) {
        this.host = Objects.requireNonNull(host, "host nao pode ser nulo");
        this.porta = porta;
        this.ouvinteMensagens = Objects.requireNonNull(ouvinteMensagens, "ouvinteMensagens nao pode ser nulo");
        this.ouvinteStatus = Objects.requireNonNull(ouvinteStatus, "ouvinteStatus nao pode ser nulo");
    }

    /**
     * Conecta ao servidor e envia o nickname como handshake.
     */
    public synchronized void conectar(String nickname) throws IOException {
        if (conectado) {
            return;
        }

        socket = new Socket(host, porta);
        escritor = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

        // Primeiro envio obrigatorio pelo protocolo: nickname
        escritor.println(nickname);

        conectado = true;
        iniciarThreadLeitura();
    }

    private void iniciarThreadLeitura() {
        threadLeitura = new Thread(() -> {
            try (BufferedReader leitor = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                String linha;
                while (conectado && (linha = leitor.readLine()) != null) {
                    ouvinteMensagens.accept(linha);
                }
            } catch (IOException e) {
                if (conectado) {
                    ouvinteStatus.accept("Conexao interrompida: " + e.getMessage());
                }
            } finally {
                desconectar();
                ouvinteStatus.accept("Desconectado do servidor.");
            }
        }, "rede-cliente-leitura");

        threadLeitura.setDaemon(true);
        threadLeitura.start();
    }

    /**
     * Envia uma linha ao servidor.
     */
    public synchronized void enviar(String linha) {
        if (conectado && escritor != null) {
            escritor.println(linha);
        }
    }

    public synchronized boolean isConectado() {
        return conectado && socket != null && !socket.isClosed();
    }

    public synchronized void desconectar() {
        if (!conectado) {
            return;
        }
        conectado = false;
        try {
            if (escritor != null) {
                escritor.println("/sair");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void close() {
        desconectar();
    }
}
