package br.com.luka.chat.persistence;

import br.com.luka.chat.model.Mensagem;
import br.com.luka.chat.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Teste de INTEGRACAO: usa H2 em memoria (nao e teste unitario). */
class JdbcMensagemRepositoryIntegracaoTest {

    private JdbcMensagemRepository repository;

    @BeforeEach
    void preparar() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        ConnectionFactory fabrica = new ConnectionFactory(url, "sa", "");
        fabrica.criarTabelas();
        repository = new JdbcMensagemRepository(fabrica);
    }

    @Test
    void deveSalvarEGerarId() {
        Mensagem salva = repository.salvar(
            new Mensagem(new Usuario("ana"), "olá, açaí", LocalDateTime.of(2026, 10, 6, 21, 15)));

        assertNotNull(salva.getId());
        Mensagem lida = repository.buscarUltimas(1).get(0);
        assertEquals("olá, açaí", lida.getConteudo());
        assertEquals(new Usuario("ana"), lida.getRemetente());
        assertEquals("#geral", lida.getDestino());
        assertEquals(salva.getHorario(), lida.getHorario());
    }

    @Test
    void deveBuscarUltimasEmOrdemCronologicaFiltrandoApenasGeral() {
        Usuario ana = new Usuario("ana");
        LocalDateTime t = LocalDateTime.of(2026, 10, 6, 21, 0);
        for (int i = 1; i <= 5; i++) {
            repository.salvar(new Mensagem(ana, "m" + i, t.plusMinutes(i)));
        }
        // Mensagem de outro grupo nao deve aparecer no #geral
        repository.salvar(new Mensagem(null, ana, "#trabalho", "secreto", t.plusMinutes(6)));

        List<String> conteudos = repository.buscarUltimas(3).stream()
            .map(Mensagem::getConteudo).toList();

        assertEquals(List.of("m3", "m4", "m5"), conteudos);
    }

    @Test
    void deveBuscarHistoricoDeGrupoEspecifico() {
        Usuario ana = new Usuario("ana");
        LocalDateTime t = LocalDateTime.of(2026, 10, 6, 21, 0);
        repository.salvar(new Mensagem(null, ana, "#java", "msg grupo 1", t));
        repository.salvar(new Mensagem(null, ana, "#geral", "msg geral", t.plusMinutes(1)));
        repository.salvar(new Mensagem(null, ana, "#java", "msg grupo 2", t.plusMinutes(2)));

        List<String> grupoJava = repository.buscarPorDestino("#java", 10).stream()
            .map(Mensagem::getConteudo).toList();

        assertEquals(List.of("msg grupo 1", "msg grupo 2"), grupoJava);
    }

    @Test
    void deveBuscarHistoricoPrivadoEntreDoisUsuariosBidirecionalmente() {
        Usuario ana = new Usuario("ana");
        Usuario bruno = new Usuario("bruno");
        Usuario carlos = new Usuario("carlos");
        LocalDateTime t = LocalDateTime.of(2026, 10, 6, 21, 0);

        repository.salvar(new Mensagem(null, ana, "@bruno", "oi bruno", t));
        repository.salvar(new Mensagem(null, bruno, "@ana", "oi ana, tudo bem?", t.plusMinutes(1)));
        repository.salvar(new Mensagem(null, carlos, "@ana", "oi ana sou o carlos", t.plusMinutes(2)));

        List<String> conversaAnaBruno = repository.buscarPrivadas(ana, bruno, 10).stream()
            .map(Mensagem::getConteudo).toList();

        assertEquals(List.of("oi bruno", "oi ana, tudo bem?"), conversaAnaBruno);
    }
}
