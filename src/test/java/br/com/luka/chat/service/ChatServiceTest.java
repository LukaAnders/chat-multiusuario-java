package br.com.luka.chat.service;

import br.com.luka.chat.model.Mensagem;
import br.com.luka.chat.model.MensagemInvalidaException;
import br.com.luka.chat.model.Usuario;
import br.com.luka.chat.support.FakeConexao;
import br.com.luka.chat.support.InMemoryMensagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChatServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO =
        Clock.fixed(Instant.parse("2026-10-06T18:30:00Z"), ZONA);

    private InMemoryMensagemRepository repository;
    private ClientRegistry registry;
    private ChatService service;

    @BeforeEach
    void preparar() {
        repository = new InMemoryMensagemRepository();
        registry = new ClientRegistry();
        service = new ChatService(repository, registry, RELOGIO, 3);
    }

    @Test
    void enviarMensagemDeveSalvarNoRepository() {
        FakeConexao ana = new FakeConexao("ana");
        service.conectar(ana);

        Mensagem salva = service.enviarMensagem(ana.getUsuario(), "olá");

        assertNotNull(salva.getId());
        assertEquals(1, repository.todas().size());
        assertEquals("olá", repository.todas().get(0).getConteudo());
    }

    @Test
    void broadcastDeveEntregarATodosOsConectados() {
        FakeConexao ana = new FakeConexao("ana");
        FakeConexao bruno = new FakeConexao("bruno");
        FakeConexao carlos = new FakeConexao("carlos");
        service.conectar(ana);
        service.conectar(bruno);
        service.conectar(carlos);

        service.enviarMensagem(ana.getUsuario(), "oi");

        String esperado = "[06/10 15:30] ana: oi";
        assertTrue(ana.getRecebidas().contains(esperado));
        assertTrue(bruno.getRecebidas().contains(esperado));
        assertTrue(carlos.getRecebidas().contains(esperado));
    }

    @Test
    void clienteRemovidoNaoDeveReceberMaisMensagens() {
        FakeConexao ana = new FakeConexao("ana");
        FakeConexao bruno = new FakeConexao("bruno");
        service.conectar(ana);
        service.conectar(bruno);

        service.desconectar(bruno);
        int antes = bruno.getRecebidas().size();
        service.enviarMensagem(ana.getUsuario(), "alguem ai?");

        assertEquals(antes, bruno.getRecebidas().size());
    }

    @Test
    void mensagemInvalidaNaoDeveSerSalvaNemTransmitida() {
        FakeConexao ana = new FakeConexao("ana");
        FakeConexao bruno = new FakeConexao("bruno");
        service.conectar(ana);
        service.conectar(bruno);
        int antes = bruno.getRecebidas().size();

        assertThrows(MensagemInvalidaException.class, () ->
            service.enviarMensagem(ana.getUsuario(), "   ")
        );
        assertTrue(repository.todas().isEmpty());
        assertEquals(antes, bruno.getRecebidas().size());
    }

    @Test
    void buscarHistoricoDeveDevolverUltimasEmOrdemCronologica() {
        Usuario ana = new Usuario("ana");
        for (int i = 1; i <= 5; i++) {
            service.enviarMensagem(ana, "m" + i);
        }

        List<Mensagem> historico = service.buscarHistorico();

        assertEquals(List.of("m3", "m4", "m5"),
            historico.stream().map(Mensagem::getConteudo).toList());
    }

    @Test
    void aoConectarDeveReceberHistorico() {
        service.enviarMensagem(new Usuario("ana"), "antiga");
        FakeConexao bruno = new FakeConexao("bruno");

        service.conectar(bruno);

        assertTrue(bruno.getRecebidas().contains("[06/10 15:30] ana: antiga"));
    }

    @Test
    void conectarDeveRecusarNicknameEmUso() {
        assertTrue(service.conectar(new FakeConexao("ana")));
        assertFalse(service.conectar(new FakeConexao("Ana")));
    }

    @Test
    void outrosDevemSerAvisadosDaEntradaESaida() {
        FakeConexao ana = new FakeConexao("ana");
        FakeConexao bruno = new FakeConexao("bruno");
        service.conectar(ana);

        service.conectar(bruno);
        service.desconectar(bruno);

        assertTrue(ana.getRecebidas().contains("* bruno entrou no chat"));
        assertTrue(ana.getRecebidas().contains("* bruno saiu do chat"));
    }

    @Test
    void falhaAoEnviarParaUmaConexaoNaoDeveImpedirAsDemais() {
        FakeConexao ana = new FakeConexao("ana");
        FakeConexao quebrada = new FakeConexao("quebrada");
        FakeConexao carlos = new FakeConexao("carlos");
        service.conectar(ana);
        service.conectar(quebrada);
        service.conectar(carlos);
        quebrada.falhando();

        service.enviarMensagem(ana.getUsuario(), "oi");

        assertTrue(carlos.getRecebidas().contains("[06/10 15:30] ana: oi"));
        assertFalse(registry.snapshot().contains(quebrada));
    }

    @Test
    void mensagemPrivadaDeveEntregarApenasAoRemetenteEAoDestinatario() {
        FakeConexao ana = new FakeConexao("ana");
        FakeConexao bruno = new FakeConexao("bruno");
        FakeConexao carlos = new FakeConexao("carlos");
        service.conectar(ana);
        service.conectar(bruno);
        service.conectar(carlos);
        carlos.getRecebidas().clear();

        service.enviarMensagemPrivada(ana.getUsuario(), "bruno", "segredo");

        assertTrue(ana.getRecebidas().stream().anyMatch(l -> l.contains("segredo")));
        assertTrue(bruno.getRecebidas().stream().anyMatch(l -> l.contains("segredo")));
        assertTrue(carlos.getRecebidas().isEmpty());
    }

    @Test
    void mensagemDeGrupoDeveEntregarApenasAosMembrosDoGrupo() {
        FakeConexao ana = new FakeConexao("ana");
        FakeConexao bruno = new FakeConexao("bruno");
        FakeConexao carlos = new FakeConexao("carlos");
        service.conectar(ana);
        service.conectar(bruno);
        service.conectar(carlos);

        service.entrarNoGrupo(ana, "#devs");
        service.entrarNoGrupo(bruno, "#devs");
        carlos.getRecebidas().clear();

        service.enviarMensagemGrupo(ana.getUsuario(), "#devs", "codigo pronto");

        assertTrue(ana.getRecebidas().stream().anyMatch(l -> l.contains("codigo pronto")));
        assertTrue(bruno.getRecebidas().stream().anyMatch(l -> l.contains("codigo pronto")));
        assertFalse(carlos.getRecebidas().stream().anyMatch(l -> l.contains("codigo pronto")));
    }

    @Test
    void broadcastConcorrenteNaoDevePerderNemCorromperMensagens() throws Exception {
        int clientes = 10;
        int mensagensPorCliente = 50;
        List<FakeConexao> conexoes = new java.util.ArrayList<>();
        for (int i = 0; i < clientes; i++) {
            FakeConexao c = new FakeConexao("user" + i);
            conexoes.add(c);
            service.conectar(c);
        }
        conexoes.forEach(c -> c.getRecebidas().clear());

        ExecutorService executor = Executors.newFixedThreadPool(clientes);
        CountDownLatch largada = new CountDownLatch(1);
        for (FakeConexao c : conexoes) {
            executor.submit(() -> {
                largada.await();
                for (int j = 0; j < mensagensPorCliente; j++) {
                    service.enviarMensagem(c.getUsuario(), "msg" + j);
                }
                return null;
            });
        }
        largada.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        int total = clientes * mensagensPorCliente;
        assertEquals(total, repository.todas().size());
        List<String> ordemDoBanco = repository.todas().stream()
            .map(Mensagem::formatar).toList();
        for (FakeConexao c : conexoes) {
            List<String> apenasMensagens = c.getRecebidas().stream()
                .filter(l -> l.startsWith("["))
                .toList();
            assertEquals(ordemDoBanco, apenasMensagens);
        }
    }
}
