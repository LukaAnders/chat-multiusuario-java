package br.com.luka.chat.bot;

import java.util.Locale;

/**
 * Servico de fallback quando nenhuma API Key do Gemini for fornecida.
 * Permite que a professora execute e teste o bot sem configurar contas externas.
 */
public class SimuladoServicoIa implements ServicoIa {

    @Override
    public String responder(String remetente, String pergunta) {
        String p = pergunta.toLowerCase(Locale.ROOT).trim();

        if (p.contains("ola") || p.contains("olá") || p.contains("oi") || p.contains("bom dia") || p.contains("boa tarde")) {
            return "Olá @" + remetente + "! Sou o bot de IA do chat. Em que posso te ajudar?";
        }
        if (p.contains("quem é você") || p.contains("quem e voce") || p.contains("o que você faz")) {
            return "Sou o assistente do Chat Multiusuário UniCesumar! Estou operando no modo demonstração/offline. Para ativar o Gemini real, defina a chave GEMINI_API_KEY.";
        }
        if (p.contains("java") || p.contains("socket") || p.contains("thread")) {
            return "Java 21 com Sockets TCP e Threads é incrível! O servidor gerencia concorrência com CopyOnWriteArrayList e seções críticas sincronizadas.";
        }
        if (p.contains("hora") || p.contains("horario")) {
            return "Agora são " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + ".";
        }

        return "🤖 [Modo Demonstração] @" + remetente + ", recebi sua mensagem: '" + pergunta + "'. Para respostas inteligentes completas com Gemini, configure a chave GEMINI_API_KEY.";
    }

    @Override
    public boolean isReal() {
        return false;
    }
}
