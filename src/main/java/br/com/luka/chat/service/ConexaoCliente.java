package br.com.luka.chat.service;

import br.com.luka.chat.model.Usuario;

/**
 * Alguem conectado que pode receber texto. Permite testar o servico
 * sem rede: o ClienteHandler implementa; nos testes, FakeConexao.
 */
public interface ConexaoCliente {

    Usuario getUsuario();

    /**
     * Envia uma linha ao cliente.
     *
     * @return false se a entrega falhou (conexao quebrada).
     */
    boolean enviar(String linha);
}

