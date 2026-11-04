package br.com.luka.chat.client;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ConexaoRedeClienteTest {

    @Test
    void deveConectarEnviarNicknameETransmitirMensagens() throws Exception {
        BlockingQueue<String> recebidasPeloServidor = new LinkedBlockingQueue<>();
        BlockingQueue<String> recebidasPeloCliente = new LinkedBlockingQueue<>();
        BlockingQueue<String> statusCliente = new LinkedBlockingQueue<>();

        try (ServerSocket servidorFake = new ServerSocket(0)) {
            int porta = servidorFake.getLocalPort();

            Thread servidorThread = new Thread(() -> {
                try (Socket socket = servidorFake.accept();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                     PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

                    String nick = reader.readLine();
                    recebidasPeloServidor.offer(nick);
                    writer.println("* bem-vindo " + nick);

                    String msg = reader.readLine();
                    if (msg != null) {
                        recebidasPeloServidor.offer(msg);
                    }
                } catch (Exception ignored) {
                }
            });
            servidorThread.start();

            try (ConexaoRedeCliente cliente = new ConexaoRedeCliente(
                    "localhost", porta, recebidasPeloCliente::offer, statusCliente::offer)) {

                cliente.conectar("ana");

                assertEquals("ana", recebidasPeloServidor.poll(2, TimeUnit.SECONDS));
                assertEquals("* bem-vindo ana", recebidasPeloCliente.poll(2, TimeUnit.SECONDS));

                cliente.enviar("olá teste");
                assertEquals("olá teste", recebidasPeloServidor.poll(2, TimeUnit.SECONDS));
            }
        }
    }
}
