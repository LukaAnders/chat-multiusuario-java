package br.com.luka.chat.support;

import br.com.luka.chat.model.Mensagem;
import br.com.luka.chat.model.MensagemRepository;

import java.util.ArrayList;
import java.util.List;
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
        int inicio = Math.max(0, mensagens.size() - quantidade);
        return List.copyOf(mensagens.subList(inicio, mensagens.size()));
    }

    public synchronized List<Mensagem> todas() {
        return List.copyOf(mensagens);
    }
}
