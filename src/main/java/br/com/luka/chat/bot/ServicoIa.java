package br.com.luka.chat.bot;

/**
 * Contrato para servicos de geracao de respostas com IA.
 */
public interface ServicoIa {

    /**
     * Gera uma resposta para o prompt do usuario.
     *
     * @param remetente quem fez a pergunta
     * @param pergunta texto enviado
     * @return resposta gerada pela IA ou fallback
     */
    String responder(String remetente, String pergunta);

    /**
     * @return true se esta conectado a uma API real com chave ativa
     */
    boolean isReal();
}

