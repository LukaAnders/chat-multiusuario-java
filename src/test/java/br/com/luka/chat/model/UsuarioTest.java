package br.com.luka.chat.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void deveRecusarNomeVazio() {
        assertThrows(IllegalArgumentException.class, () ->
            new Usuario("   ")
        );
    }

    @Test
    void deveGuardarNomeSemEspacosNasExtremidades() {
        Usuario usuario = new Usuario("  Luka  ");

        assertEquals("Luka", usuario.getNome());
    }
}