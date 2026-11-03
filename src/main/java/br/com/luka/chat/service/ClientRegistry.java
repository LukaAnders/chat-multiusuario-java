package br.com.luka.chat.service;

import br.com.luka.chat.model.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dono da colecao compartilhada de conexoes.
 *
 * Concorrencia: CopyOnWriteArrayList porque o broadcast (leitura) e muito
 * mais frequente que conectar/desconectar (escrita). A iteracao ocorre sobre
 * uma copia, entao nao ha ConcurrentModificationException nem lock durante
 * a escrita em sockets. O check-then-act de nickname unico usa um lock
 * proprio para ser atomico.
 */
public class ClientRegistry {

    private final CopyOnWriteArrayList<ConexaoCliente> conexoes =
        new CopyOnWriteArrayList<>();
    private final Object travaCadastro = new Object();

    /** @return false se o nickname ja estiver em uso. */
    public boolean adicionar(ConexaoCliente conexao) {
        synchronized (travaCadastro) {
            if (buscarPorUsuario(conexao.getUsuario()).isPresent()) {
                return false;
            }
            conexoes.add(conexao);
            return true;
        }
    }

    public boolean remover(ConexaoCliente conexao) {
        return conexoes.remove(conexao);
    }

    /** Copia imutavel para iterar sem segurar nenhum lock. */
    public List<ConexaoCliente> snapshot() {
        return List.copyOf(conexoes);
    }

    public int quantidade() {
        return conexoes.size();
    }

    public Optional<ConexaoCliente> buscarPorUsuario(Usuario usuario) {
        return conexoes.stream()
            .filter(c -> c.getUsuario().equals(usuario))
            .findFirst();
    }

    public List<Usuario> usuariosOnline() {
        return conexoes.stream().map(ConexaoCliente::getUsuario).toList();
    }
}

