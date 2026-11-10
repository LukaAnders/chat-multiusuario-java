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

    @Test
    void botDeveDividirRespostasMultilinhasOuLongasRespeitandoLimiteDoProtocolo() {
        String respostaMultilinha = "Linha 1 da explicação.\n\nLinha 2 da explicação.";
        var blocos = BotIaCliente.dividirRespostaParaProtocolo(respostaMultilinha);

        assertEquals(2, blocos.size());
        assertEquals("Linha 1 da explicação.", blocos.get(0));
        assertEquals("Linha 2 da explicação.", blocos.get(1));

        String respostaMuitoLonga = "a".repeat(600);
        var blocosLongos = BotIaCliente.dividirRespostaParaProtocolo(respostaMuitoLonga);
        assertEquals(2, blocosLongos.size());
        assertTrue(blocosLongos.get(0).length() <= 450);
        assertTrue(blocosLongos.get(1).length() <= 450);
    }
}

