package br.com.luka.chat.service;

import br.com.luka.chat.support.FakeConexao;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ClientRegistryTest {

    @Test
    void deveAdicionarERemoverConexoes() {
        ClientRegistry registry = new ClientRegistry();
        FakeConexao ana = new FakeConexao("ana");

        assertTrue(registry.adicionar(ana));
        assertEquals(1, registry.quantidade());

        registry.remover(ana);
        assertEquals(0, registry.quantidade());
    }

    @Test
    void deveRecusarNicknameDuplicadoSemDistinguirMaiusculas() {
        ClientRegistry registry = new ClientRegistry();

        assertTrue(registry.adicionar(new FakeConexao("ana")));
        assertFalse(registry.adicionar(new FakeConexao("ANA")));
        assertEquals(1, registry.quantidade());
    }

    @Test
    void snapshotNaoDeveMudarQuandoRegistryMuda() {
        ClientRegistry registry = new ClientRegistry();
        registry.adicionar(new FakeConexao("ana"));

        var snapshot = registry.snapshot();
        registry.adicionar(new FakeConexao("bruno"));

        assertEquals(1, snapshot.size());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @Test
    void apenasUmaThreadDeveConseguirRegistrarOMesmoNickname() throws Exception {
        ClientRegistry registry = new ClientRegistry();
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                largada.await();
                if (registry.adicionar(new FakeConexao("ana"))) {
                    sucessos.incrementAndGet();
                }
                return null;
            });
        }
        largada.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, sucessos.get());
        assertEquals(1, registry.quantidade());
    }
}

