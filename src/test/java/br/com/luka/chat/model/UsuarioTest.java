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
    void deveRecusarNomeNulo() {
        assertThrows(IllegalArgumentException.class, () ->
            new Usuario(null)
        );
    }

    @Test
    void deveGuardarNomeSemEspacosNasExtremidades() {
        Usuario usuario = new Usuario("  Luka  ");

        assertEquals("Luka", usuario.getNome());
    }

    @Test
    void deveRecusarNomeComEspacoInterno() {
        assertThrows(IllegalArgumentException.class, () ->
            new Usuario("Luka Cruz")
        );
    }

    @Test
    void deveRecusarNomeAcimaDoLimite() {
        String nomeGrande = "a".repeat(Usuario.TAMANHO_MAXIMO_NOME + 1);

        assertThrows(IllegalArgumentException.class, () ->
            new Usuario(nomeGrande)
        );
    }

    @Test
    void deveConsiderarNomesIguaisSemDistinguirMaiusculas() {
        assertEquals(new Usuario("Ana"), new Usuario("ana"));
        assertEquals(new Usuario("Ana").hashCode(), new Usuario("ANA").hashCode());
    }
}
