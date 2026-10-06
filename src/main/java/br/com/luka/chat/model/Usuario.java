package br.com.luka.chat.model;

public final class Usuario {

    private final String nome;

    public Usuario(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                "O nome nao pode ser vazio."
            );
        }

        this.nome = nome.trim();
    }

    public String getNome() {
        return nome;
    }
}