package br.com.luka.chat.bot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integracao com a API REST oficial do Google Gemini usando HttpClient nativo do Java 21.
 */
public class GeminiServicoIa implements ServicoIa {

    private static final String URL_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private static final Pattern PADRAO_TEXTO_RESPOSTA =
            Pattern.compile("\"text\":\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL);

    private final String apiKey;
    private final HttpClient httpClient;

    public GeminiServicoIa(String apiKey) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey nao pode ser nula").trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String responder(String remetente, String pergunta) {
        String promptContextualizado = "Você é o assistente virtual do Chat Multiusuário UniCesumar. "
                + "O usuário @" + remetente + " disse: \"" + pergunta + "\". "
                + "Responda de forma concisa, educada e direta em português do Brasil (máximo 3 frases, ideal para chat).";

        String jsonCorpo = "{"
                + "\"contents\": [{"
                + "  \"parts\": [{\"text\": \"" + escaparJson(promptContextualizado) + "\"}]"
                + "}]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + apiKey))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonCorpo))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extrairTextoJson(response.body());
            } else {
                return "⚠ Erro na API Gemini (HTTP " + response.statusCode() + "): verifique se sua API Key é válida.";
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return "⚠ Erro de conexão com a API Gemini: " + e.getMessage();
        }
    }

    @Override
    public boolean isReal() {
        return true;
    }

    static String extrairTextoJson(String json) {
        Matcher matcher = PADRAO_TEXTO_RESPOSTA.matcher(json);
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

