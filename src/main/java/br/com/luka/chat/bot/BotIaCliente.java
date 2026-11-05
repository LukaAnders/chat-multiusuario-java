package br.com.luka.chat.bot;

import br.com.luka.chat.client.ConexaoRedeCliente;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cliente autônomo de IA que conecta ao chat e responde às perguntas
 * direcionadas a ele (@gemini ou /ia).
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
        // Ignora avisos do sistema (* ...) ou mensagens de erro (! ...)
        if (linha.startsWith("*") || linha.startsWith("!")) {
            return;
        }

        // Formato esperado do servidor: [dd/MM HH:mm] remetente: conteudo
        int posFimHorario = linha.indexOf("] ");
        if (posFimHorario == -1) {
            return;
        }

        String resto = linha.substring(posFimHorario + 2);
        int posDoisPontos = resto.indexOf(": ");
        if (posDoisPontos == -1) {
            return;
        }

        String remetente = resto.substring(0, posDoisPontos).trim();
        String conteudo = resto.substring(posDoisPontos + 2).trim();

        // Evita que o bot responda a si mesmo (loop infinito)
        if (remetente.equalsIgnoreCase(NICKNAME_PADRAO)) {
            return;
        }

        String conteudoLower = conteudo.toLowerCase();
        boolean deveResponder = conteudoLower.contains(GATILHO_MACAO)
                || conteudoLower.contains(GATILHO_IA)
                || conteudoLower.startsWith(GATILHO_COMANDO);

        if (deveResponder) {
            String pergunta = limparGatilhos(conteudo);
            if (pergunta.isBlank()) {
                pergunta = "olá";
            }

            final String perguntaFinal = pergunta;
            // Executa em thread separada para não bloquear a leitura do socket
            poolProcessamento.submit(() -> {
                try {
                    String resposta = servicoIa.responder(remetente, perguntaFinal);
                    conexao.enviar("@" + remetente + " " + resposta);
                } catch (Exception e) {
                    conexao.enviar("@" + remetente + " Desculpe, tive um problema ao processar sua pergunta.");
                }
            });
        }
    }

    private String limparGatilhos(String texto) {
        return texto.replaceAll("(?i)" + GATILHO_MACAO, "")
                    .replaceAll("(?i)" + GATILHO_IA, "")
                    .replaceAll("(?i)^" + GATILHO_COMANDO, "")
                    .trim();
    }

    public static ServicoIa resolverServicoIa() {
        // 1. Tenta obter da variavel de ambiente GEMINI_API_KEY
        String chave = System.getenv("GEMINI_API_KEY");
        if (chave != null && !chave.isBlank()) {
            return new GeminiServicoIa(chave.trim());
        }

        // 2. Tenta ler do arquivo chat.local.properties (ignorado no git)
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

        // 3. Fallback: modo simulado seguro para avaliacao sem configuracao externa
        return new SimuladoServicoIa();
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        String nick = args.length > 2 ? args[2] : NICKNAME_PADRAO;

        ServicoIa servico = resolverServicoIa();
        BotIaCliente bot = new BotIaCliente(host, porta, servico);
        bot.iniciar(nick);

        // Mantem a thread principal viva ouvindo o processo
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando Bot IA...");
            bot.conexao.desconectar();
        }));
    }
}

