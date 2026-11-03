package br.com.luka.chat.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mensagem publica do chat. Imutavel: atributos privados e finais.
 * Nasce com id nulo; o repositorio devolve uma nova instancia com o id.
 * O horario vem de fora (Clock no servico) para manter testes deterministicos.
 */
public final class Mensagem {

    public static final int TAMANHO_MAXIMO_CONTEUDO = 500;

    private static final DateTimeFormatter FORMATO =
        DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final Long id;
    private final Usuario remetente;
    private final String conteudo;
    private final LocalDateTime horario;

    public Mensagem(Usuario remetente, String conteudo, LocalDateTime horario) {
        this(null, remetente, conteudo, horario);
    }

    public Mensagem(Long id, Usuario remetente, String conteudo, LocalDateTime horario) {
        if (remetente == null) {
            throw new MensagemInvalidaException("O remetente nao pode ser nulo.");
        }
        if (conteudo == null || conteudo.isBlank()) {
            throw new MensagemInvalidaException("O conteudo nao pode ser vazio.");
        }
        String conteudoLimpo = conteudo.trim();
        if (conteudoLimpo.length() > TAMANHO_MAXIMO_CONTEUDO) {
            throw new MensagemInvalidaException(
                "O conteudo deve ter no maximo " + TAMANHO_MAXIMO_CONTEUDO + " caracteres."
            );
        }
        if (horario == null) {
            throw new MensagemInvalidaException("O horario nao pode ser nulo.");
        }

        this.id = id;
        this.remetente = remetente;
        this.conteudo = conteudoLimpo;
        this.horario = horario;
    }

    public Mensagem comId(Long novoId) {
        return new Mensagem(novoId, remetente, conteudo, horario);
    }

    public Long getId() {
        return id;
    }

    public Usuario getRemetente() {
        return remetente;
    }

    public String getConteudo() {
        return conteudo;
    }

    public LocalDateTime getHorario() {
        return horario;
    }

    public String formatar() {
        return "[" + horario.format(FORMATO) + "] "
            + remetente.getNome() + ": " + conteudo;
    }
}
