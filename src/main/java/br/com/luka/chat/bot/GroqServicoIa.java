package br.com.luka.chat.bot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
            "openai/gpt-oss-120b",
            "openai/gpt-oss-20b",
            "qwen/qwen3.8-27b",
            "llama-3.3-70b-versatile"
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
                + "Responda de forma prestativa, clara, natural e concisa em português do Brasil (até 380 caracteres, sem markdown complexo, ideal para mensagem de chat).";
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
                    + "\"max_tokens\": 1024"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_COMPLETIONS))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonCorpo, StandardCharsets.UTF_8))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );
                ultimoStatus = response.statusCode();

                if (ultimoStatus == 200) {
                    String extraido = extrairTextoJson(response.body());
                    if (!extraido.isBlank()) {
                        return extraido;
                    }
                }
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
