package br.com.luka.chat.model;

import java.util.List;

/**
 * Contrato de persistencia de mensagens. Fica no dominio (inversao de
 * dependencia); a implementacao JDBC fica no pacote persistence.
 */
public interface MensagemRepository {

    /** Salva e devolve uma nova instancia com o id gerado. */
    Mensagem salvar(Mensagem mensagem);

    /** Devolve as ultimas mensagens em ordem cronologica (mais antiga primeiro). */
    List<Mensagem> buscarUltimas(int quantidade);
}

