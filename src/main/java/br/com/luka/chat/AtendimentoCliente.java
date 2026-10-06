package br.com.luka.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class AtendimentoCliente implements Runnable {

    private final Socket cliente;
    private PrintWriter saida;
    private String nome;

    public AtendimentoCliente(Socket cliente) {
        this.cliente = cliente;
    }

    public synchronized void enviar(String mensagem) {
        saida.println(mensagem);
    }

    @Override
    public void run() {
        boolean registrado = false;

        try (
            Socket conexao = cliente;

            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(
                    conexao.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );

            PrintWriter escritor = new PrintWriter(
                conexao.getOutputStream(),
                true,
                StandardCharsets.UTF_8
            )
        ) {
            saida = escritor;

            // A primeira linha enviada pelo cliente sera o nome.
            nome = entrada.readLine();

            if (nome == null || nome.isBlank()) {
                return;
            }

            nome = nome.trim();

            ServidorChat.adicionarCliente(this);
            registrado = true;

            ServidorChat.transmitir(nome + " entrou no chat.");
            System.out.println(nome + " conectado.");

            String mensagem;

            while ((mensagem = entrada.readLine()) != null) {
                if (mensagem.equalsIgnoreCase("/sair")) {
                    break;
                }

                if (!mensagem.isBlank()) {
                    ServidorChat.transmitir(nome + ": " + mensagem);
                }
            }

        } catch (IOException e) {
            System.out.println("Erro na conexao: " + e.getMessage());
        } finally {
            if (registrado) {
                ServidorChat.removerCliente(this);
                ServidorChat.transmitir(nome + " saiu do chat.");
                System.out.println(nome + " desconectado.");
            }
        }
    }
}