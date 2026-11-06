package br.com.luka.chat.support;

import br.com.luka.chat.model.Mensagem;
import br.com.luka.chat.model.MensagemRepository;
import br.com.luka.chat.model.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Repositorio em memoria para testes, sem banco real. */
public class InMemoryMensagemRepository implements MensagemRepository {

    private final List<Mensagem> mensagens = new ArrayList<>();
    private final AtomicLong sequencia = new AtomicLong();

    @Override
    public synchronized Mensagem salvar(Mensagem mensagem) {
        Mensagem salva = mensagem.comId(sequencia.incrementAndGet());
        mensagens.add(salva);
        return salva;
    }

    @Override
    public synchronized List<Mensagem> buscarUltimas(int quantidade) {
        return buscarPorDestino(Mensagem.DESTINO_GERAL, quantidade);
    }

    @Override
    public synchronized List<Mensagem> buscarPorDestino(String destino, int quantidade) {
        String alvo = destino.trim().toLowerCase(Locale.ROOT);
        List<Mensagem> filtradas = mensagens.stream()
            .filter(m -> m.getDestino().equalsIgnoreCase(alvo))
            .toList();
        int inicio = Math.max(0, filtradas.size() - quantidade);
        return List.copyOf(filtradas.subList(inicio, filtradas.size()));
    }

    @Override
    public synchronized List<Mensagem> buscarPrivadas(Usuario usuarioA, Usuario usuarioB, int quantidade) {
        String destA = "@" + usuarioA.getNome().toLowerCase(Locale.ROOT);
        String destB = "@" + usuarioB.getNome().toLowerCase(Locale.ROOT);

        List<Mensagem> filtradas = mensagens.stream()
            .filter(m -> (m.getRemetente().equals(usuarioA) && m.getDestino().equalsIgnoreCase(destB))
                      || (m.getRemetente().equals(usuarioB) && m.getDestino().equalsIgnoreCase(destA)))
            .toList();
        int inicio = Math.max(0, filtradas.size() - quantidade);
        return List.copyOf(filtradas.subList(inicio, filtradas.size()));
    }

    @Override
    public synchronized void removerPorDestino(String destino) {
        if (destino == null) return;
        String alvo = destino.trim().toLowerCase(Locale.ROOT);
        mensagens.removeIf(m -> m.getDestino().equalsIgnoreCase(alvo));
    }

    public synchronized List<Mensagem> todas() {
        return List.copyOf(mensagens);
    }
}
