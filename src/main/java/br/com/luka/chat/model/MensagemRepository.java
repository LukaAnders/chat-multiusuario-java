package br.com.luka.chat.model;

import java.util.List;

/**
 * Contrato de persistencia de mensagens. Fica no dominio (inversao de
 * dependencia); a implementacao JDBC fica no pacote persistence.
 */
public interface MensagemRepository {

    /** Salva e devolve uma nova instancia com o id gerado. */
    Mensagem salvar(Mensagem mensagem);

    /** Devolve as ultimas mensagens da sala #geral em ordem cronologica. */
    List<Mensagem> buscarUltimas(int quantidade);

    /** Devolve as ultimas mensagens de uma sala/grupo especifico (ex: #geral, #trabalho). */
    List<Mensagem> buscarPorDestino(String destino, int quantidade);

    /** Devolve as ultimas mensagens privadas trocadas entre dois usuarios. */
    List<Mensagem> buscarPrivadas(Usuario usuarioA, Usuario usuarioB, int quantidade);

    /** Remove todas as mensagens de um destino/grupo especifico. */
    void removerPorDestino(String destino);
}
