package br.com.luka.chat.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MensagemTest {

    private static final Usuario LUKA = new Usuario("Luka");
    private static final LocalDateTime HORARIO =
        LocalDateTime.of(2026, 10, 6, 15, 30);

    @Test
    void deveRecusarRemetenteNulo() {
        assertThrows(MensagemInvalidaException.class, () ->
            new Mensagem(null, "Ola!", HORARIO)
        );
    }

    @Test
    void deveRecusarConteudoVazio() {
        assertThrows(MensagemInvalidaException.class, () ->
            new Mensagem(LUKA, "   ", HORARIO)
        );
    }

    @Test
    void deveRecusarConteudoAcimaDoLimite() {
        String grande = "x".repeat(Mensagem.TAMANHO_MAXIMO_CONTEUDO + 1);

        assertThrows(MensagemInvalidaException.class, () ->
            new Mensagem(LUKA, grande, HORARIO)
        );
    }

    @Test
    void deveAplicarTrimNoConteudo() {
        Mensagem mensagem = new Mensagem(LUKA, "  Ola!  ", HORARIO);

        assertEquals("Ola!", mensagem.getConteudo());
    }

    @Test
    void deveFormatarMensagemParaExibicao() {
        Mensagem mensagem = new Mensagem(LUKA, "Olá!", HORARIO);

        assertEquals("[06/10 15:30] Luka: Olá!", mensagem.formatar());
    }

    @Test
    void deveNascerSemIdEGerarNovaInstanciaComId() {
        Mensagem original = new Mensagem(LUKA, "Ola!", HORARIO);
        Mensagem salva = original.comId(7L);

        assertNull(original.getId());
        assertEquals(7L, salva.getId());
        assertEquals(original.getConteudo(), salva.getConteudo());
        assertNotSame(original, salva);
    }
}
