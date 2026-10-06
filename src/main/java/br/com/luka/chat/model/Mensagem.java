package br.com.luka.chat.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Mensagem {

    private final String remetente;
    private final String conteudo;
    private final LocalDateTime horario;

    public Mensagem(
        String remetente,
        String conteudo,
        LocalDateTime horario
    ) {
        if (remetente == null || remetente.isBlank()) {
            throw new IllegalArgumentException(
                "O remetente nao pode ser vazio."
            );
        }

        if (conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException(
                "O conteudo nao pode ser vazio."
            );
        }

        if (horario == null) {
            throw new IllegalArgumentException(
                "O horario nao pode ser nulo."
            );
        }

        this.remetente = remetente.trim();
        this.conteudo = conteudo;
        this.horario = horario;
    }

    public String getRemetente() {
        return remetente;
    }

    public String getConteudo() {
        return conteudo;
    }

    public LocalDateTime getHorario() {
        return horario;
    }

    public String formatar() {
        DateTimeFormatter formato =
            DateTimeFormatter.ofPattern("HH:mm");

        return "[" + horario.format(formato) + "] "
            + remetente + ": " + conteudo;
    }
}