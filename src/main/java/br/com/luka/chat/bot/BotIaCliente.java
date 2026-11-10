package br.com.luka.chat.bot;

import br.com.luka.chat.client.ConexaoRedeCliente;
import br.com.luka.chat.service.ChatService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final int LIMITE_BLOCO_PROTOCOLO = 450;
    private static final Path ARQUIVO_CONFIG_LOCAL = Path.of("chat.local.properties");
    private static final AtomicBoolean BOT_EM_BACKGROUND_INICIADO = new AtomicBoolean(false);

    private final ConexaoRedeCliente conexao;
    private volatile ServicoIa servicoIa;
    private final ExecutorService poolProcessamento = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "bot-ia-worker");
        t.setDaemon(true);
        return t;
    });

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

    /**
     * Inicia uma instancia daemon do Bot IA em segundo plano caso ainda nao esteja ativa nesta JVM.
     * Se outro processo ja tiver registrado o nickname 'Gemini' no servidor, o servidor ignora a duplicata.
     */
    public static void garantirBotAtivoEmBackground(String host, int porta) {
        if (!BOT_EM_BACKGROUND_INICIADO.compareAndSet(false, true)) {
            return;
        }
        Thread threadBot = new Thread(() -> {
            try {
                BotIaCliente bot = new BotIaCliente(host, porta, resolverServicoIa());
                bot.conexao.conectar(NICKNAME_PADRAO);
            } catch (IOException ignored) {
                BOT_EM_BACKGROUND_INICIADO.set(false);
            }
        }, "bot-ia-background");
        threadBot.setDaemon(true);
        threadBot.start();
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
                // Atualiza dinamicamente caso o usuario tenha acabado de salvar a API Key
                ServicoIa servicoAtual = resolverServicoIa();
                if (servicoAtual.isReal() || this.servicoIa == null) {
                    this.servicoIa = servicoAtual;
                }
                String resposta = this.servicoIa.responder(remetente, perguntaFinal);
                for (String bloco : dividirRespostaParaProtocolo(resposta)) {
                    conexao.enviar(prefixoComando + bloco);
                }
            } catch (Exception e) {
                conexao.enviar(prefixoComando + "Desculpe, tive um problema ao processar sua pergunta.");
            }
        });
    }

    /**
     * Divide respostas multilinhas ou longas em blocos seguros para o protocolo TCP (orientado a linhas
     * e com limite de 500 caracteres por Mensagem).
     */
    public static List<String> dividirRespostaParaProtocolo(String resposta) {
        List<String> blocos = new ArrayList<>();
        if (resposta == null || resposta.isBlank()) {
            return blocos;
        }

        String[] linhas = resposta.replace("\r", "").split("\n+");
        for (String linha : linhas) {
            String limpa = linha.trim();
            if (limpa.isEmpty()) {
                continue;
            }
            while (limpa.length() > LIMITE_BLOCO_PROTOCOLO) {
                int corte = limpa.lastIndexOf(' ', LIMITE_BLOCO_PROTOCOLO);
                if (corte <= 0) {
                    corte = LIMITE_BLOCO_PROTOCOLO;
                }
                blocos.add(limpa.substring(0, corte).trim());
                limpa = limpa.substring(corte).trim();
            }
            if (!limpa.isEmpty()) {
                blocos.add(limpa);
            }
        }
        return blocos;
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

    public static String lerChaveConfigurada() {
        String chaveGroqEnv = System.getenv("GROQ_API_KEY");
        if (chaveGroqEnv != null && !chaveGroqEnv.isBlank()) {
            return chaveGroqEnv.trim();
        }
        String chaveGeminiEnv = System.getenv("GEMINI_API_KEY");
        if (chaveGeminiEnv != null && !chaveGeminiEnv.isBlank()) {
            return chaveGeminiEnv.trim();
        }
        if (Files.exists(ARQUIVO_CONFIG_LOCAL)) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(ARQUIVO_CONFIG_LOCAL.toFile())) {
                props.load(fis);
                String[] propriedadesBusca = {"groq.api.key", "ia.api.key", "gemini.api.key"};
                for (String nomeProp : propriedadesBusca) {
                    String valor = props.getProperty(nomeProp);
                    if (valor != null && !valor.isBlank()
                            && !valor.trim().equalsIgnoreCase("COLE_SUA_CHAVE_AQUI")) {
                        return valor.trim();
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public static boolean possuiChaveConfigurada() {
        return lerChaveConfigurada() != null;
    }

    /**
     * Salva a API Key localmente em chat.local.properties (ignorado pelo Git).
     */
    public static void salvarChaveLocal(String novaChave) throws IOException {
        Properties props = new Properties();
        if (Files.exists(ARQUIVO_CONFIG_LOCAL)) {
            try (FileInputStream fis = new FileInputStream(ARQUIVO_CONFIG_LOCAL.toFile())) {
                props.load(fis);
            } catch (IOException ignored) {
            }
        }
        String limpa = novaChave != null ? novaChave.trim() : "";
        props.setProperty("groq.api.key", limpa);
        props.setProperty("gemini.api.key", limpa);
        try (FileOutputStream fos = new FileOutputStream(ARQUIVO_CONFIG_LOCAL.toFile())) {
            props.store(fos, "Configuracao local da API Key de IA (Groq / Gemini - nao versionado no Git)");
        }
    }

    /**
     * Instancia automaticamente GroqServicoIa (padrao / chaves gsk_...) ou GeminiServicoIa (chaves AIza...).
     */
    public static ServicoIa criarServicoPorChave(String chave) {
        if (chave == null || chave.isBlank()) {
            return new SimuladoServicoIa();
        }
        String limpa = chave.trim();
        if (limpa.startsWith("AIza")) {
            return new GeminiServicoIa(limpa);
        }
        return new GroqServicoIa(limpa);
    }

    public static ServicoIa resolverServicoIa() {
        String chave = lerChaveConfigurada();
        return criarServicoPorChave(chave);
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
