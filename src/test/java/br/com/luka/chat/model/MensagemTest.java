package br.com.luka.chat.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MensagemTest {

    @Test
    void deveRecusarRemetenteVazio() {
        assertThrows(IllegalArgumentException.class, () ->
            new Mensagem("   ", "Ola!", LocalDateTime.now())
        );
    }

    @Test
    void deveRecusarConteudoVazio() {
        assertThrows(IllegalArgumentException.class, () ->
            new Mensagem("Luka", "   ", LocalDateTime.now())
        );
    }

    @Test
    void deveFormatarMensagemParaExibicao() {
        LocalDateTime horario =
            LocalDateTime.of(2026, 10, 6, 15, 30);

        Mensagem mensagem =
            new Mensagem("Luka", "Ola!", horario);

        assertEquals(
            "[15:30] Luka: Ola!",
            mensagem.formatar()
        );
    }
}