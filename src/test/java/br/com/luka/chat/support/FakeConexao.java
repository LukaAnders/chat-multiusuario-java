package br.com.luka.chat.support;

import br.com.luka.chat.model.Usuario;
import br.com.luka.chat.service.ConexaoCliente;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Conexao falsa: acumula as linhas recebidas, sem rede. */
public class FakeConexao implements ConexaoCliente {

    private final Usuario usuario;
    private final List<String> recebidas = new CopyOnWriteArrayList<>();
    private volatile boolean falharAoEnviar;

    public FakeConexao(String nome) {
        this.usuario = new Usuario(nome);
    }

    public FakeConexao falhando() {
        this.falharAoEnviar = true;
        return this;
    }

    @Override
    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public boolean enviar(String linha) {
        if (falharAoEnviar) {
            return false;
        }
        recebidas.add(linha);
        return true;
    }

    public List<String> getRecebidas() {
        return recebidas;
    }
}
