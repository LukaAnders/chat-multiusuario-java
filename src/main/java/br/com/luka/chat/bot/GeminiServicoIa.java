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
 * Integracao com a API REST oficial do Google Gemini usando HttpClient nativo do Java 21.
 * Tenta automaticamente os modelos atuais do Gemini Flash em ordem de prioridade.
 */
public class GeminiServicoIa implements ServicoIa {

    private static final List<String> MODELOS_GEMINI = List.of(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
    );

    private static final String URL_PREFIXO =
            "https://generativelanguage.googleapis.com/v1beta/models/";

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
        String promptContextualizado = "Você é o assistente virtual inteligente (Gemini IA) integrado ao Chat Multiusuário. "
                + "O usuário @" + remetente + " enviou: \"" + pergunta + "\". "
                + "Responda de forma prestativa, clara, natural e concisa em português do Brasil (até 400 caracteres, ideal para mensagem de chat).";

        String jsonCorpo = "{"
                + "\"contents\": [{"
                + "  \"parts\": [{\"text\": \"" + escaparJson(promptContextualizado) + "\"}]"
                + "}]"
                + "}";

        int ultimoStatus = 0;
        for (String modelo : MODELOS_GEMINI) {
            String url = URL_PREFIXO + modelo + ":generateContent?key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonCorpo))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                ultimoStatus = response.statusCode();

                if (ultimoStatus == 200) {
                    return extrairTextoJson(response.body());
                }
                // Se for 404 (modelo descontinuado/indisponivel), tenta o proximo modelo da lista
                if (ultimoStatus != 404) {
                    break;
                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                return "⚠ Erro de conexão com a API Gemini: " + e.getMessage();
            }
        }

        return "⚠ Erro na API Gemini (HTTP " + ultimoStatus + "): verifique se sua API Key é válida.";
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
