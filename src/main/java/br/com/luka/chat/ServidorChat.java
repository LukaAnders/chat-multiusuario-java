package br.com.luka.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorChat {

    private static final Set<AtendimentoCliente> clientes =
        ConcurrentHashMap.newKeySet();

    public static void adicionarCliente(AtendimentoCliente cliente) {
        clientes.add(cliente);
    }

    public static void removerCliente(AtendimentoCliente cliente) {
        clientes.remove(cliente);
    }

    public static void transmitir(String mensagem) {
        for (AtendimentoCliente cliente : clientes) {
            cliente.enviar(mensagem);
        }
    }

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(5000)) {
            System.out.println("Servidor iniciado na porta 5000");

            while (true) {
                Socket conexao = servidor.accept();

                AtendimentoCliente atendimento =
                    new AtendimentoCliente(conexao);

                new Thread(atendimento).start();
            }

        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        }
    }
}