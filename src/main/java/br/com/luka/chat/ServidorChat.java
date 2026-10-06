package br.com.luka.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServidorChat {

    public static void main(String[] args) {
        int porta = 5000;

        try (ServerSocket servidor = new ServerSocket(porta)) {
            System.out.println("Servidor iniciado na porta " + porta);
            System.out.println("Aguardando um cliente...");

            try (
                    Socket cliente = servidor.accept();

                    BufferedReader entrada = new BufferedReader(
                            new InputStreamReader(
                                    cliente.getInputStream(),
                                    StandardCharsets.UTF_8));

                    PrintWriter saida = new PrintWriter(
                            cliente.getOutputStream(),
                            true,
                            StandardCharsets.UTF_8)) {
                System.out.println("Cliente conectado!");

                String mensagem;

                while ((mensagem = entrada.readLine()) != null) {
                    if (mensagem.equalsIgnoreCase("/sair")) {
                        saida.println("Conexao encerrada. Ate mais!");
                        break;
                    }

                    System.out.println("Cliente disse: " + mensagem);
                    saida.println("Servidor recebeu: " + mensagem);
                }

                System.out.println("Cliente desconectado.");
            }

        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        }
    }
}