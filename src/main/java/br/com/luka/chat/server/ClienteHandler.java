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
import java.util.Locale;

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
                    tratarComando(linha.trim());
                    continue;
                }
                tratarMensagem(linha);
            }
        } catch (IOException e) {
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

    private void tratarComando(String comando) {
        String cmdLower = comando.toLowerCase(Locale.ROOT);
        try {
            if (cmdLower.startsWith(Protocolo.COMANDO_MSG)) {
                String resto = comando.substring(Protocolo.COMANDO_MSG.length()).trim();
                int espaco = resto.indexOf(' ');
                if (espaco == -1) {
                    enviar(Protocolo.erro("Uso: /msg <usuario> <mensagem>"));
                    return;
                }
                String destino = resto.substring(0, espaco).trim();
                String texto = resto.substring(espaco + 1).trim();
                chatService.enviarMensagemPrivada(usuario, destino, texto);
                return;
            }

            if (cmdLower.startsWith(Protocolo.COMANDO_GRUPO)) {
                String resto = comando.substring(Protocolo.COMANDO_GRUPO.length()).trim();
                int espaco = resto.indexOf(' ');
                if (espaco == -1) {
                    enviar(Protocolo.erro("Uso: /grupo <nomeGrupo> <mensagem>"));
                    return;
                }
                String nomeGrupo = resto.substring(0, espaco).trim();
                String texto = resto.substring(espaco + 1).trim();
                chatService.enviarMensagemGrupo(usuario, nomeGrupo, texto);
                return;
            }

            if (cmdLower.startsWith(Protocolo.COMANDO_CRIAR_GRUPO)) {
                String resto = comando.substring(Protocolo.COMANDO_CRIAR_GRUPO.length()).trim();
                if (resto.isEmpty()) {
                    enviar(Protocolo.erro("Uso: /criargrupo <nomeGrupo> [user1,user2,...]"));
                    return;
                }
                String[] partes = resto.split("\\s+", 2);
                String nomeGrupo = partes[0];
                java.util.List<Usuario> convidados = new java.util.ArrayList<>();
                if (partes.length > 1 && !partes[1].isBlank()) {
                    for (String nomeUser : partes[1].split(",")) {
                        String limpo = nomeUser.trim();
                        if (!limpo.isEmpty()) {
                            convidados.add(new Usuario(limpo));
                        }
                    }
                }
                chatService.criarGrupoComMembros(this, nomeGrupo, convidados);
                return;
            }

            if (cmdLower.startsWith(Protocolo.COMANDO_ENTRAR) || cmdLower.startsWith("/criar ")) {
                int espaco = comando.indexOf(' ');
                String nomeGrupo = comando.substring(espaco + 1).trim();
                if (nomeGrupo.isEmpty()) {
                    enviar(Protocolo.erro("Informe o nome do grupo."));
                    return;
                }
                chatService.entrarNoGrupo(this, nomeGrupo);
                return;
            }

            if (cmdLower.startsWith(Protocolo.COMANDO_HISTORICO)) {
                String alvo = comando.substring(Protocolo.COMANDO_HISTORICO.length()).trim();
                chatService.enviarHistoricoConversa(this, alvo);
                return;
            }

            enviar(Protocolo.erro("Comandos: /msg <user> <txt>, /grupo <sala> <txt>, /entrar <sala>, /sair"));
        } catch (IllegalArgumentException e) {
            enviar(Protocolo.erro(e.getMessage()));
        } catch (PersistenciaException e) {
            System.out.println("Falha no banco: " + e.getCause());
            enviar(Protocolo.erro("Operacao falhou ao acessar o banco."));
        }
    }

    private void tratarMensagem(String linha) {
        try {
            chatService.enviarMensagem(usuario, linha);
        } catch (MensagemInvalidaException e) {
            enviar(Protocolo.erro(e.getMessage()));
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
