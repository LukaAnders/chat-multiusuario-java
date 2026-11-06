package br.com.luka.chat.bot;

import br.com.luka.chat.client.ConexaoRedeCliente;
import br.com.luka.chat.service.ChatService;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cliente autônomo de IA que conecta ao chat e responde às perguntas
 * direcionadas a ele (no privado, em grupos com @gemini ou no console).
 *
 * Executa as chamadas de IA de forma assincrona para nao bloquear a recepcao de rede.
 */
public class BotIaCliente {

    public static final String NICKNAME_PADRAO = "Gemini";
    private static final String GATILHO_MACAO = "@gemini";
    private static final String GATILHO_IA = "@ia";
    private static final String GATILHO_COMANDO = "/ia";

    private final ConexaoRedeCliente conexao;
    private final ServicoIa servicoIa;
    private final ExecutorService poolProcessamento = Executors.newCachedThreadPool();

    public BotIaCliente(String host, int porta, ServicoIa servicoIa) {
        this.servicoIa = servicoIa;
        this.conexao = new ConexaoRedeCliente(
                host,
                porta,
                this::processarMensagemRecebida,
                status -> System.out.println("[Bot Status] " + status)
        );
    }

    public void iniciar(String nickname) throws IOException {
        System.out.println("Iniciando Bot IA '" + nickname + "'...");
        if (servicoIa.isReal()) {
            System.out.println("Modo: IA REAL ATIVADA (Google Gemini).");
        } else {
            System.out.println("Modo: SIMULAÇÃO / DEMONSTRAÇÃO (sem chave de API configurada).");
        }

        conexao.conectar(nickname);
        System.out.println("Bot conectado com sucesso ao chat!");
    }

    private void processarMensagemRecebida(String linha) {
        if (linha.startsWith("*") || linha.startsWith("!") || linha.startsWith(ChatService.PREFIXO_CONTATOS)) {
            return;
        }

        // 1. Mensagem Privada direta para o Bot: ~PRIV|remetente|Gemini|[dd/MM HH:mm] nome: texto
        if (linha.startsWith(ChatService.PREFIXO_PRIVADA)) {
            String[] partes = linha.substring(ChatService.PREFIXO_PRIVADA.length()).split("\\|", 3);
            if (partes.length == 3) {
                String remetente = partes[0].trim();
                if (remetente.equalsIgnoreCase(NICKNAME_PADRAO)) {
                    return;
                }
                String conteudo = extrairConteudoFormatado(partes[2]);
                responderAssincrono(remetente, limparGatilhos(conteudo), "/msg " + remetente + " ");
            }
            return;
        }

        // 2. Mensagem em Grupo: ~GRUPO|#grupo|[dd/MM HH:mm] nome: texto
        if (linha.startsWith(ChatService.PREFIXO_GRUPO)) {
            String resto = linha.substring(ChatService.PREFIXO_GRUPO.length());
            int pipe = resto.indexOf('|');
            if (pipe != -1) {
                String grupo = resto.substring(0, pipe).trim();
                String msgFormatada = resto.substring(pipe + 1);
                if (msgFormatada.startsWith("*")) {
                    return;
                }
                String remetente = extrairRemetenteFormatado(msgFormatada);
                String conteudo = extrairConteudoFormatado(msgFormatada);
                if (remetente != null && !remetente.equalsIgnoreCase(NICKNAME_PADRAO) && contemGatilho(conteudo)) {
                    responderAssincrono(remetente, limparGatilhos(conteudo), "/grupo " + grupo + " ");
                }
            }
            return;
        }

        // 3. Mensagem do console legado: [dd/MM HH:mm] remetente: conteudo
        String remetente = extrairRemetenteFormatado(linha);
        String conteudo = extrairConteudoFormatado(linha);
        if (remetente != null && !remetente.equalsIgnoreCase(NICKNAME_PADRAO) && contemGatilho(conteudo)) {
            responderAssincrono(remetente, limparGatilhos(conteudo), "");
        }
    }

    private void responderAssincrono(String remetente, String perguntaLimpa, String prefixoComando) {
        final String perguntaFinal = perguntaLimpa.isBlank() ? "olá" : perguntaLimpa;
        poolProcessamento.submit(() -> {
            try {
                String resposta = servicoIa.responder(remetente, perguntaFinal);
                conexao.enviar(prefixoComando + resposta);
            } catch (Exception e) {
                conexao.enviar(prefixoComando + "Desculpe, tive um problema ao processar sua pergunta.");
            }
        });
    }

    private boolean contemGatilho(String conteudo) {
        if (conteudo == null) return false;
        String lower = conteudo.toLowerCase();
        return lower.contains(GATILHO_MACAO)
                || lower.contains(GATILHO_IA)
                || lower.startsWith(GATILHO_COMANDO);
    }

    private String extrairRemetenteFormatado(String msgFormatada) {
        int posFimHorario = msgFormatada.indexOf("] ");
        if (posFimHorario == -1) return null;
        String resto = msgFormatada.substring(posFimHorario + 2);
        int posDoisPontos = resto.indexOf(": ");
        if (posDoisPontos == -1) return null;
        return resto.substring(0, posDoisPontos).trim();
    }

    private String extrairConteudoFormatado(String msgFormatada) {
        int posFimHorario = msgFormatada.indexOf("] ");
        if (posFimHorario == -1) return msgFormatada;
        String resto = msgFormatada.substring(posFimHorario + 2);
        int posDoisPontos = resto.indexOf(": ");
        if (posDoisPontos == -1) return resto;
        return resto.substring(posDoisPontos + 2).trim();
    }

    private String limparGatilhos(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("(?i)" + GATILHO_MACAO, "")
                    .replaceAll("(?i)" + GATILHO_IA, "")
                    .replaceAll("(?i)^" + GATILHO_COMANDO, "")
                    .trim();
    }

    public static ServicoIa resolverServicoIa() {
        String chave = System.getenv("GEMINI_API_KEY");
        if (chave != null && !chave.isBlank()) {
            return new GeminiServicoIa(chave.trim());
        }

        Path arquivoConfig = Path.of("chat.local.properties");
        if (Files.exists(arquivoConfig)) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(arquivoConfig.toFile())) {
                props.load(fis);
                String chaveProp = props.getProperty("gemini.api.key");
                if (chaveProp != null && !chaveProp.isBlank()) {
                    return new GeminiServicoIa(chaveProp.trim());
                }
            } catch (IOException ignored) {
            }
        }

        return new SimuladoServicoIa();
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        String nick = args.length > 2 ? args[2] : NICKNAME_PADRAO;

        ServicoIa servico = resolverServicoIa();
        BotIaCliente bot = new BotIaCliente(host, porta, servico);
        bot.iniciar(nick);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando Bot IA...");
            bot.conexao.desconectar();
        }));
    }
}
