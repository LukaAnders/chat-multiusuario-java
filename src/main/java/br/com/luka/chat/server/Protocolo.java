package br.com.luka.chat.server;

/**
 * Constantes e utilitarios do protocolo de texto (uma mensagem por linha, UTF-8).
 * Handshake: a primeira linha do cliente e o nickname.
 */
public final class Protocolo {

    public static final String COMANDO_SAIR = "/sair";
    public static final String COMANDO_MSG = "/msg ";
    public static final String COMANDO_GRUPO = "/grupo ";
    public static final String COMANDO_ENTRAR = "/entrar ";
    public static final String COMANDO_HISTORICO = "/historico ";
    public static final String PREFIXO_COMANDO = "/";
    public static final String PREFIXO_ERRO = "! ";
    public static final String PREFIXO_CONTROLE = "~";

    private Protocolo() {
    }

    public static boolean ehSair(String linha) {
        String limpa = linha.trim();
        return limpa.equalsIgnoreCase(COMANDO_SAIR) || limpa.equalsIgnoreCase("/quit");
    }

    public static boolean ehComando(String linha) {
        return linha.trim().startsWith(PREFIXO_COMANDO);
    }

    public static String erro(String motivo) {
        return PREFIXO_ERRO + motivo;
    }
}
