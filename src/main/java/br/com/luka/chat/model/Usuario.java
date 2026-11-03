package br.com.luka.chat.model;

import java.util.Locale;

/**
 * Usuario do chat. Imutavel: pode circular entre threads sem bloqueio.
 * A igualdade ignora maiusculas/minusculas para garantir nickname unico.
 */
public final class Usuario {

    public static final int TAMANHO_MAXIMO_NOME = 20;

    private final String nome;

    public Usuario(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome nao pode ser vazio.");
        }

        String nomeLimpo = nome.trim();

        if (nomeLimpo.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("O nome nao pode conter espacos.");
        }

        if (nomeLimpo.length() > TAMANHO_MAXIMO_NOME) {
            throw new IllegalArgumentException(
                "O nome deve ter no maximo " + TAMANHO_MAXIMO_NOME + " caracteres."
            );
        }

        this.nome = nomeLimpo;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Usuario usuario
            && nome.equalsIgnoreCase(usuario.nome);
    }

    @Override
    public int hashCode() {
        return nome.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return nome;
    }
}
