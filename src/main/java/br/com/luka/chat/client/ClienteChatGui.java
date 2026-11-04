package br.com.luka.chat.client;

import br.com.luka.chat.server.Protocolo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * Cliente com Interface Grafica e seletor dinamico de temas para o Chat Multiusuario.
 */
public class ClienteChatGui extends JFrame {

    public enum Tema {
        ESCURO("🌙 Escuro (Discord)",
                new Color(30, 31, 34), new Color(43, 45, 49), new Color(56, 58, 64),
                new Color(242, 243, 245), new Color(148, 155, 164),
                new Color(88, 101, 242), new Color(71, 82, 196),
                "#2b2d31", "#f2f3f5", "#57f287", "#00b0f4", "#949ba4", "#fee75c", "#ed4245"),

        CLARO("☀️ Claro (Clean)",
                new Color(242, 243, 245), new Color(255, 255, 255), new Color(230, 233, 238),
                new Color(30, 31, 34), new Color(90, 95, 105),
                new Color(0, 102, 204), new Color(0, 82, 163),
                "#ffffff", "#23272a", "#1b873f", "#0066cc", "#72767d", "#b58105", "#d83a56"),

        DRACULA("🧛 Drácula (Roxo)",
                new Color(40, 42, 54), new Color(50, 52, 66), new Color(68, 71, 90),
                new Color(248, 248, 242), new Color(189, 147, 249),
                new Color(255, 121, 198), new Color(220, 100, 170),
                "#323442", "#f8f8f2", "#50fa7b", "#8be9fd", "#6272a4", "#f1fa8c", "#ff5555"),

        ESMERALDA("🌲 Esmeralda",
                new Color(20, 35, 30), new Color(28, 48, 40), new Color(38, 65, 55),
                new Color(235, 250, 240), new Color(150, 200, 180),
                new Color(46, 160, 110), new Color(38, 135, 92),
                "#1c3028", "#ebfaf0", "#7bf0a5", "#4ee2c0", "#7aa090", "#e8d860", "#ff6b6b");

        final String nome;
        final Color fundoJanela;
        final Color fundoChat;
        final Color fundoEntrada;
        final Color textoBranco;
        final Color textoSecundario;
        final Color botaoPrimario;
        final Color botaoHover;

        final String cssFundoChat;
        final String cssTexto;
        final String cssUserMe;
        final String cssUserOther;
        final String cssTime;
        final String cssSystem;
        final String cssError;

        Tema(String nome, Color fundoJanela, Color fundoChat, Color fundoEntrada,
             Color textoBranco, Color textoSecundario, Color botaoPrimario, Color botaoHover,
             String cssFundoChat, String cssTexto, String cssUserMe, String cssUserOther,
             String cssTime, String cssSystem, String cssError) {
            this.nome = nome;
            this.fundoJanela = fundoJanela;
            this.fundoChat = fundoChat;
            this.fundoEntrada = fundoEntrada;
            this.textoBranco = textoBranco;
            this.textoSecundario = textoSecundario;
            this.botaoPrimario = botaoPrimario;
            this.botaoHover = botaoHover;
            this.cssFundoChat = cssFundoChat;
            this.cssTexto = cssTexto;
            this.cssUserMe = cssUserMe;
            this.cssUserOther = cssUserOther;
            this.cssTime = cssTime;
            this.cssSystem = cssSystem;
            this.cssError = cssError;
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    private static final Color COR_STATUS_ONLINE = new Color(35, 165, 89);

    private final JPanel painelTopo;
    private final JPanel painelInferior;
    private final JScrollPane scrollChat;
    private final JTextPane areaChat;
    private final HTMLEditorKit kitHtml;
    private final HTMLDocument docHtml;
    private final JTextField campoTexto;
    private final JButton botaoEnviar;
    private final JLabel labelStatus;
    private final JComboBox<Tema> comboTemas;

    private Tema temaAtual = Tema.ESCURO;
    private ConexaoRedeCliente conexao;
    private String nickname;

    public ClienteChatGui() {
        super("Chat Multiusuário POO");

        setSize(720, 560);
        setMinimumSize(new Dimension(520, 420));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Painel Superior (Header com Status e Seletor de Tema)
        painelTopo = new JPanel(new BorderLayout());
        painelTopo.setBorder(new EmptyBorder(10, 16, 10, 16));

        labelStatus = new JLabel("● Desconectado");
        labelStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        painelTopo.add(labelStatus, BorderLayout.WEST);

        // Painel à direita com seletor de tema
        JPanel painelDireitaTopo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        painelDireitaTopo.setOpaque(false);

        JLabel labelTema = new JLabel("Tema:");
        labelTema.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        labelTema.setForeground(temaAtual.textoSecundario);

        comboTemas = new JComboBox<>(Tema.values());
        comboTemas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboTemas.setFocusable(false);
        comboTemas.addActionListener(e -> {
            Tema selecionado = (Tema) comboTemas.getSelectedItem();
            if (selecionado != null && selecionado != temaAtual) {
                aplicarTema(selecionado);
            }
        });

        painelDireitaTopo.add(labelTema);
        painelDireitaTopo.add(comboTemas);
        painelTopo.add(painelDireitaTopo, BorderLayout.EAST);

        add(painelTopo, BorderLayout.NORTH);

        // 2. Painel Central (Mensagens HTML)
        areaChat = new JTextPane();
        areaChat.setEditable(false);
        areaChat.setContentType("text/html");

        kitHtml = new HTMLEditorKit();
        areaChat.setEditorKit(kitHtml);
        docHtml = (HTMLDocument) areaChat.getDocument();

        scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        scrollChat.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollChat, BorderLayout.CENTER);

        // 3. Painel Inferior (Barra de Envio)
        painelInferior = new JPanel(new BorderLayout(10, 0));
        painelInferior.setBorder(new EmptyBorder(12, 16, 14, 16));

        campoTexto = new JTextField();
        campoTexto.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campoTexto.setEnabled(false);

        botaoEnviar = new JButton("Enviar");
        botaoEnviar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        botaoEnviar.setFocusPainted(false);
        botaoEnviar.setBorderPainted(false);
        botaoEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botaoEnviar.setBorder(new EmptyBorder(8, 20, 8, 20));
        botaoEnviar.setEnabled(false);

        botaoEnviar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (botaoEnviar.isEnabled()) {
                    botaoEnviar.setBackground(temaAtual.botaoHover);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (botaoEnviar.isEnabled()) {
                    botaoEnviar.setBackground(temaAtual.botaoPrimario);
                }
            }
        });

        painelInferior.add(campoTexto, BorderLayout.CENTER);
        painelInferior.add(botaoEnviar, BorderLayout.EAST);
        add(painelInferior, BorderLayout.SOUTH);

        // Ações de envio
        botaoEnviar.addActionListener(e -> enviarMensagem());
        campoTexto.addActionListener(e -> enviarMensagem());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fecharAplicacao();
            }
        });

        aplicarTema(Tema.ESCURO);
    }

    private void aplicarTema(Tema novoTema) {
        this.temaAtual = novoTema;

        getContentPane().setBackground(temaAtual.fundoJanela);
        painelTopo.setBackground(temaAtual.fundoJanela);
        painelInferior.setBackground(temaAtual.fundoJanela);

        scrollChat.setBackground(temaAtual.fundoJanela);
        scrollChat.getViewport().setBackground(temaAtual.fundoChat);
        areaChat.setBackground(temaAtual.fundoChat);

        campoTexto.setBackground(temaAtual.fundoEntrada);
        campoTexto.setForeground(temaAtual.textoBranco);
        campoTexto.setCaretColor(temaAtual.textoBranco);
        campoTexto.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(temaAtual.fundoEntrada.darker(), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        botaoEnviar.setBackground(temaAtual.botaoPrimario);
        botaoEnviar.setForeground(Color.WHITE);

        comboTemas.setBackground(temaAtual.fundoEntrada);
        comboTemas.setForeground(temaAtual.textoBranco);

        // Atualiza as regras CSS das mensagens dinamicamente
        kitHtml.getStyleSheet().addRule("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 10px; background-color: " + temaAtual.cssFundoChat + "; color: " + temaAtual.cssTexto + "; font-size: 13px; }");
        kitHtml.getStyleSheet().addRule(".msg-box { margin-bottom: 8px; line-height: 1.4; }");
        kitHtml.getStyleSheet().addRule(".time { color: " + temaAtual.cssTime + "; font-size: 11px; margin-right: 6px; }");
        kitHtml.getStyleSheet().addRule(".user-me { color: " + temaAtual.cssUserMe + "; font-weight: bold; }");
        kitHtml.getStyleSheet().addRule(".user-other { color: " + temaAtual.cssUserOther + "; font-weight: bold; }");
        kitHtml.getStyleSheet().addRule(".content { color: " + temaAtual.cssTexto + "; }");
        kitHtml.getStyleSheet().addRule(".system { color: " + temaAtual.cssSystem + "; font-style: italic; margin-bottom: 6px; font-size: 12px; }");
        kitHtml.getStyleSheet().addRule(".error { color: " + temaAtual.cssError + "; font-weight: bold; margin-bottom: 6px; font-size: 12px; }");

        areaChat.repaint();
        repaint();
    }

    public void iniciar() {
        setVisible(true);

        JPanel painelDialogo = new JPanel(new GridLayout(4, 1, 6, 6));
        painelDialogo.setPreferredSize(new Dimension(280, 140));

        JTextField campoNick = new JTextField();
        JTextField campoHost = new JTextField("localhost");
        JTextField campoPorta = new JTextField("5000");

        painelDialogo.add(new JLabel("Seu Nickname:"));
        painelDialogo.add(campoNick);
        painelDialogo.add(new JLabel("Servidor (Host:Porta):"));

        JPanel painelRede = new JPanel(new GridLayout(1, 2, 4, 0));
        painelRede.add(campoHost);
        painelRede.add(campoPorta);
        painelDialogo.add(painelRede);

        int res = JOptionPane.showConfirmDialog(
                this,
                painelDialogo,
                "Conectar ao Chat",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (res != JOptionPane.OK_OPTION) {
            dispose();
            System.exit(0);
            return;
        }

        nickname = campoNick.getText().trim();
        if (nickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "O nickname não pode ser vazio.", "Aviso", JOptionPane.WARNING_MESSAGE);
            iniciar();
            return;
        }

        int porta = 5000;
        try {
            porta = Integer.parseInt(campoPorta.getText().trim());
        } catch (NumberFormatException ignored) {
        }
        String host = campoHost.getText().trim();

        conectarAoServidor(host, porta, nickname);
    }

    private void conectarAoServidor(String host, int porta, String nick) {
        labelStatus.setText("● Conectando a " + host + ":" + porta + "...");
        labelStatus.setForeground(temaAtual.textoSecundario);

        conexao = new ConexaoRedeCliente(
                host,
                porta,
                this::receberLinhaDoServidor,
                this::atualizarStatus
        );

        new Thread(() -> {
            try {
                conexao.conectar(nick);
                SwingUtilities.invokeLater(() -> {
                    setTitle("Chat Multiusuário — @" + nick);
                    labelStatus.setText("● Online como @" + nick);
                    labelStatus.setForeground(COR_STATUS_ONLINE);
                    campoTexto.setEnabled(true);
                    botaoEnviar.setEnabled(true);
                    campoTexto.requestFocusInWindow();
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    labelStatus.setText("● Desconectado");
                    labelStatus.setForeground(new Color(237, 66, 69));
                    JOptionPane.showMessageDialog(
                            ClienteChatGui.this,
                            "Não foi possível conectar ao servidor:\n" + e.getMessage(),
                            "Erro de Conexão",
                            JOptionPane.ERROR_MESSAGE
                    );
                    iniciar();
                });
            }
        }, "thread-conectar-gui").start();
    }

    private void receberLinhaDoServidor(String linha) {
        SwingUtilities.invokeLater(() -> {
            String htmlAppend;

            if (linha.startsWith(Protocolo.PREFIXO_ERRO)) {
                String msgErro = escaparHtml(linha.substring(Protocolo.PREFIXO_ERRO.length()));
                htmlAppend = "<div class='error'>⚠ " + msgErro + "</div>";
            } else if (linha.startsWith("* ")) {
                String msgSistema = escaparHtml(linha);
                htmlAppend = "<div class='system'>" + msgSistema + "</div>";
            } else if (linha.startsWith("[") && linha.contains("] ")) {
                int fecharColchete = linha.indexOf("] ");
                String horario = escaparHtml(linha.substring(1, fecharColchete));
                String resto = linha.substring(fecharColchete + 2);

                int doisPontos = resto.indexOf(": ");
                if (doisPontos != -1) {
                    String remetente = resto.substring(0, doisPontos);
                    String conteudo = escaparHtml(resto.substring(doisPontos + 2));

                    boolean souEu = nickname != null && nickname.equalsIgnoreCase(remetente.trim());
                    String classeUser = souEu ? "user-me" : "user-other";
                    String tagRemetente = souEu ? "Você" : escaparHtml(remetente);

                    htmlAppend = "<div class='msg-box'>"
                            + "<span class='time'>" + horario + "</span>"
                            + "<span class='" + classeUser + "'>" + tagRemetente + ": </span>"
                            + "<span class='content'>" + conteudo + "</span>"
                            + "</div>";
                } else {
                    htmlAppend = "<div class='msg-box'><span class='content'>" + escaparHtml(linha) + "</span></div>";
                }
            } else {
                htmlAppend = "<div class='msg-box'><span class='content'>" + escaparHtml(linha) + "</span></div>";
            }

            try {
                kitHtml.insertHTML(docHtml, docHtml.getLength(), htmlAppend, 0, 0, null);
                areaChat.setCaretPosition(docHtml.getLength());
            } catch (Exception ignored) {
            }
        });
    }

    private String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    private void atualizarStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            labelStatus.setText("● " + status);
            labelStatus.setForeground(new Color(237, 66, 69));
            try {
                kitHtml.insertHTML(docHtml, docHtml.getLength(),
                        "<div class='system'>[ " + escaparHtml(status) + " ]</div>", 0, 0, null);
                areaChat.setCaretPosition(docHtml.getLength());
            } catch (Exception ignored) {
            }
            campoTexto.setEnabled(false);
            botaoEnviar.setEnabled(false);
        });
    }

    private void enviarMensagem() {
        String texto = campoTexto.getText().trim();
        if (texto.isEmpty()) {
            return;
        }

        if (texto.equalsIgnoreCase(Protocolo.COMANDO_SAIR)) {
            fecharAplicacao();
            return;
        }

        campoTexto.setText("");
        new Thread(() -> conexao.enviar(texto), "thread-envio-gui").start();
    }

    private void fecharAplicacao() {
        if (conexao != null) {
            conexao.desconectar();
        }
        dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteChatGui gui = new ClienteChatGui();
            gui.iniciar();
        });
    }
}
