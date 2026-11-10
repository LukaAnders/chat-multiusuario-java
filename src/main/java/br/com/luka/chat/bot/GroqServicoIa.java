package br.com.luka.chat.bot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integracao com a API REST oficial da Groq (OpenAI-compatible Chat Completions)
 * usando HttpClient nativo do Java 21.
 */
public class GroqServicoIa implements ServicoIa {

    private static final String URL_COMPLETIONS =
            "https://api.groq.com/openai/v1/chat/completions";

    private static final List<String> MODELOS_GROQ = List.of(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "gemma2-9b-it"
    );

    private static final Pattern PADRAO_CONTEUDO_RESPOSTA =
            Pattern.compile("\"content\":\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL);

    private final String apiKey;
    private final HttpClient httpClient;

    public GroqServicoIa(String apiKey) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey nao pode ser nula").trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String responder(String remetente, String pergunta) {
        String systemPrompt = "Você é o assistente virtual inteligente integrado ao Chat Multiusuário. "
                + "Responda de forma prestativa, clara, natural e concisa em português do Brasil (até 400 caracteres, ideal para mensagem de chat).";
        String userPrompt = "O usuário @" + remetente + " disse: \"" + pergunta + "\"";

        int ultimoStatus = 0;
        for (String modelo : MODELOS_GROQ) {
            String jsonCorpo = "{"
                    + "\"model\": \"" + modelo + "\","
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"" + escaparJson(systemPrompt) + "\"},"
                    + "  {\"role\": \"user\", \"content\": \"" + escaparJson(userPrompt) + "\"}"
                    + "],"
                    + "\"temperature\": 0.7,"
                    + "\"max_tokens\": 250"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_COMPLETIONS))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonCorpo))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                ultimoStatus = response.statusCode();

                if (ultimoStatus == 200) {
                    return extrairTextoJson(response.body());
                }
                // Se o modelo nao estiver disponivel (400/404/429), tenta o proximo modelo de fallback
                if (ultimoStatus == 401 || ultimoStatus == 403) {
                    break;
                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                return "⚠ Erro de conexão com a API Groq: " + e.getMessage();
            }
        }

        return "⚠ Erro na API Groq (HTTP " + ultimoStatus + "): verifique se sua API Key (gsk_...) é válida.";
    }

    @Override
    public boolean isReal() {
        return true;
    }

    static String extrairTextoJson(String json) {
        Matcher matcher = PADRAO_CONTEUDO_RESPOSTA.matcher(json);
        if (matcher.find()) {
            String texto = matcher.group(1);
            return desescaparJson(texto).trim();
        }
        return "Não consegui interpretar a resposta da IA.";
    }

    private static String escaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private static String desescaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\n", "\n")
                    .replace("\\r", "")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
    }
}
