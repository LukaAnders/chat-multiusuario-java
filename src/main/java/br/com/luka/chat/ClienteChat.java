package br.com.luka.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClienteChat {

    public static void main(String[] args) {
        String endereco = "localhost";
        int porta = 5000;

        try (
                Socket conexao = new Socket(endereco, porta);

                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(
                                conexao.getInputStream(),
                                StandardCharsets.UTF_8));

                PrintWriter saida = new PrintWriter(
                        conexao.getOutputStream(),
                        true,
                        StandardCharsets.UTF_8);

                Scanner teclado = new Scanner(System.in)) {
            System.out.println("Conectado ao servidor!");
            System.out.println("Digite /sair para encerrar.");

            while (true) {
                System.out.print("Voce: ");

                if (!teclado.hasNextLine()) {
                    break;
                }

                String mensagem = teclado.nextLine();
                saida.println(mensagem);

                String resposta = entrada.readLine();

                if (resposta == null) {
                    System.out.println("O servidor encerrou a conexao.");
                    break;
                }

                System.out.println(resposta);

                if (mensagem.equalsIgnoreCase("/sair")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Erro no cliente: " + e.getMessage());
        }
    }
}