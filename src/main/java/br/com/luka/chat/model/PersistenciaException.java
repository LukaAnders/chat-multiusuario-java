package br.com.luka.chat.model;

/**
 * Falha ao acessar o armazenamento de mensagens. Evita que SQLException
 * (java.sql) vaze para as camadas de servico e rede.
 */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
