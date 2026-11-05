package br.com.luka.chat.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServicoIaTest {

    @Test
    void servicoSimuladoDeveResponderSaudacaoEManterModoOffline() {
        SimuladoServicoIa simulado = new SimuladoServicoIa();

        assertFalse(simulado.isReal());
        String resposta = simulado.responder("ana", "olá, tudo bem?");
        assertTrue(resposta.contains("@ana"));
    }

    @Test
    void geminiServicoIaDeveExtrairTextoDeRespostaJsonCorretamente() {
        String jsonFalso = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "Olá! Sou o Gemini.\\nComo posso te ajudar?"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        String textoExtraido = GeminiServicoIa.extrairTextoJson(jsonFalso);
        assertEquals("Olá! Sou o Gemini.\nComo posso te ajudar?", textoExtraido);
    }
}
