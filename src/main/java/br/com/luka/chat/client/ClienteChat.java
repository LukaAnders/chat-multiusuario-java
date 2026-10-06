package br.com.luka.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClienteChat {

    public static void main(String[] args) {
        try (Scanner teclado = new Scanner(System.in)) {
            System.out.print("Digite seu nome: ");
            String nome = teclado.nextLine().trim();

            if (nome.isBlank()) {
                System.out.println("O nome nao pode ficar vazio.");
                return;
            }

            try (
                Socket conexao = new Socket("localhost", 5000);

                BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(
                        conexao.getInputStream(),
                        StandardCharsets.UTF_8
                    )
                );

                PrintWriter saida = new PrintWriter(
                    conexao.getOutputStream(),
                    true,
                    StandardCharsets.UTF_8
                )
            ) {
                saida.println(nome);

                Thread recebimento = new Thread(() -> {
                    try {
                        String mensagem;

                        while ((mensagem = entrada.readLine()) != null) {
                            System.out.println(mensagem);
                        }

                        System.out.println(
                            "Conexao encerrada. Digite /sair para finalizar."
                        );
                    } catch (IOException e) {
                        if (!conexao.isClosed()) {
                            System.out.println(
                                "Conexao interrompida. Digite /sair para finalizar."
                            );
                        }
                    }
                });

                recebimento.setDaemon(true);
                recebimento.start();

                System.out.println("Digite mensagens ou /sair para encerrar.");

                while (teclado.hasNextLine()) {
                    String mensagem = teclado.nextLine();

                    saida.println(mensagem);

                    if (mensagem.equalsIgnoreCase("/sair")
                            || saida.checkError()) {
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Erro no cliente: " + e.getMessage());
        }
    }
}