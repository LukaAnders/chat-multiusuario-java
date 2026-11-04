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
 * Cliente com Interface Grafica Moderna (Dark Theme) para o Chat Multiusuario.
 * 
 * Regra de concorrencia: a thread de rede NUNCA altera componentes
 * graficos diretamente; todas as atualizacoes sao despachadas via
 * SwingUtilities.invokeLater.
 */
public class ClienteChatGui extends JFrame {

    // Paleta de Cores Moderna (Estilo Discord / Dark Theme)
    private static final Color COR_FUNDO_JANELA = new Color(30, 31, 34);
    private static final Color COR_FUNDO_CHAT = new Color(43, 45, 49);
    private static final Color COR_FUNDO_ENTRADA = new Color(56, 58, 64);
    private static final Color COR_TEXTO_BRANCO = new Color(242, 243, 245);
    private static final Color COR_TEXTO_SECUNDARIO = new Color(148, 155, 164);
    private static final Color COR_BOTAO_PRIMARIO = new Color(88, 101, 242);
    private static final Color COR_BOTAO_HOVER = new Color(71, 82, 196);
    private static final Color COR_STATUS_ONLINE = new Color(35, 165, 89);

    private final JTextPane areaChat;
    private final HTMLEditorKit kitHtml;
    private final HTMLDocument docHtml;
    private final JTextField campoTexto;
    private final JButton botaoEnviar;
    private final JLabel labelStatus;

    private ConexaoRedeCliente conexao;
    private String nickname;

    public ClienteChatGui() {
        super("Chat Multiusuário POO");

        setSize(700, 550);
        setMinimumSize(new Dimension(500, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(COR_FUNDO_JANELA);
        setLayout(new BorderLayout());

        // 1. Painel Superior (Header com Status)
        JPanel painelTopo = new JPanel(new BorderLayout());
        painelTopo.setBackground(COR_FUNDO_JANELA);
        painelTopo.setBorder(new EmptyBorder(12, 16, 12, 16));

        labelStatus = new JLabel("● Desconectado");
        labelStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelStatus.setForeground(COR_TEXTO_SECUNDARIO);
        painelTopo.add(labelStatus, BorderLayout.WEST);

        JLabel labelTitulo = new JLabel("Chat UniCesumar • POO");
        labelTitulo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelTitulo.setForeground(new Color(110, 115, 125));
        painelTopo.add(labelTitulo, BorderLayout.EAST);

        add(painelTopo, BorderLayout.NORTH);

        // 2. Painel Central (Mensagens em HTML estilizado)
        areaChat = new JTextPane();
        areaChat.setEditable(false);
        areaChat.setContentType("text/html");
        areaChat.setBackground(COR_FUNDO_CHAT);

        kitHtml = new HTMLEditorKit();
        areaChat.setEditorKit(kitHtml);
        docHtml = (HTMLDocument) areaChat.getDocument();
        configurarEstilosHtml();

        JScrollPane scroll = new JScrollPane(areaChat);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        scroll.setBackground(COR_FUNDO_JANELA);
        scroll.getViewport().setBackground(COR_FUNDO_CHAT);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // 3. Painel Inferior (Barra de Envio)
        JPanel painelInferior = new JPanel(new BorderLayout(10, 0));
        painelInferior.setBackground(COR_FUNDO_JANELA);
        painelInferior.setBorder(new EmptyBorder(12, 16, 14, 16));

        campoTexto = new JTextField();
        campoTexto.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campoTexto.setBackground(COR_FUNDO_ENTRADA);
        campoTexto.setForeground(COR_TEXTO_BRANCO);
        campoTexto.setCaretColor(COR_TEXTO_BRANCO);
        campoTexto.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(65, 68, 75), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        campoTexto.setEnabled(false);

        botaoEnviar = new JButton("Enviar");
        botaoEnviar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        botaoEnviar.setBackground(COR_BOTAO_PRIMARIO);
        botaoEnviar.setForeground(Color.WHITE);
        botaoEnviar.setFocusPainted(false);
        botaoEnviar.setBorderPainted(false);
        botaoEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botaoEnviar.setBorder(new EmptyBorder(8, 20, 8, 20));
        botaoEnviar.setEnabled(false);

        botaoEnviar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (botaoEnviar.isEnabled()) {
                    botaoEnviar.setBackground(COR_BOTAO_HOVER);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (botaoEnviar.isEnabled()) {
                    botaoEnviar.setBackground(COR_BOTAO_PRIMARIO);
                }
            }
        });

        painelInferior.add(campoTexto, BorderLayout.CENTER);
        painelInferior.add(botaoEnviar, BorderLayout.EAST);
        add(painelInferior, BorderLayout.SOUTH);

        // Ações
        botaoEnviar.addActionListener(e -> enviarMensagem());
        campoTexto.addActionListener(e -> enviarMensagem());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fecharAplicacao();
            }
        });
    }

    private void configurarEstilosHtml() {
        kitHtml.getStyleSheet().addRule("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 10px; background-color: #2b2d31; color: #f2f3f5; font-size: 13px; }");
        kitHtml.getStyleSheet().addRule(".msg-box { margin-bottom: 8px; line-height: 1.4; }");
        kitHtml.getStyleSheet().addRule(".time { color: #949ba4; font-size: 11px; margin-right: 6px; }");
        kitHtml.getStyleSheet().addRule(".user-me { color: #57f287; font-weight: bold; }");
        kitHtml.getStyleSheet().addRule(".user-other { color: #00b0f4; font-weight: bold; }");
        kitHtml.getStyleSheet().addRule(".content { color: #dbdee1; }");
        kitHtml.getStyleSheet().addRule(".system { color: #fee75c; font-style: italic; margin-bottom: 6px; font-size: 12px; }");
        kitHtml.getStyleSheet().addRule(".error { color: #ed4245; font-weight: bold; margin-bottom: 6px; font-size: 12px; }");
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
        labelStatus.setForeground(COR_TEXTO_SECUNDARIO);

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
                // Erro: ! mensagem
                String msgErro = escaparHtml(linha.substring(Protocolo.PREFIXO_ERRO.length()));
                htmlAppend = "<div class='error'>⚠ " + msgErro + "</div>";
            } else if (linha.startsWith("* ")) {
                // Mensagem de sistema: * fulano entrou/saiu
                String msgSistema = escaparHtml(linha);
                htmlAppend = "<div class='system'>" + msgSistema + "</div>";
            } else if (linha.startsWith("[") && linha.contains("] ")) {
                // Mensagem normal formatada: [dd/MM HH:mm] remetente: conteudo
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
