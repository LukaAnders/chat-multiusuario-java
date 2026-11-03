package br.com.luka.chat.model;

/**
 * Lancada quando uma mensagem viola as regras do dominio.
 */
public class MensagemInvalidaException extends IllegalArgumentException {

    public MensagemInvalidaException(String motivo) {
        super(motivo);
    }
}
