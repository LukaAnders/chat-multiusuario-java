package br.com.luka.chat.client;

import br.com.luka.chat.server.Protocolo;
import br.com.luka.chat.service.ChatService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cliente com Interface Grafica Estilo WhatsApp (Apenas Conversas Privadas
 * e Grupos com participantes selecionados + Seletor Dinamico de Temas).
 *
 * Regra de concorrencia: a thread de rede NUNCA altera componentes
 * graficos diretamente; todas as atualizacoes sao despachadas via
 * SwingUtilities.invokeLater.
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
    private final JPanel painelSidebar;
    private final JPanel painelTopoSidebar;
    private final JPanel painelInferior;
    private final JScrollPane scrollChat;
    private final JScrollPane scrollSidebar;
    private final JTextPane areaChat;
    private final HTMLEditorKit kitHtml;
    private HTMLDocument docHtml;
    private final JTextField campoTexto;
    private final JButton botaoEnviar;
    private final JButton botaoNovoGrupo;
    private final JLabel labelStatus;
    private final JLabel labelConversaAtiva;
    private final JComboBox<Tema> comboTemas;

    private final DefaultListModel<String> modeloConversas = new DefaultListModel<>();
    private final JList<String> listaConversas;

    // Estado das conversas (manipulado exclusivamente na EDT do Swing)
    private String conversaAtiva = null;
    private final Map<String, List<String>> mensagensPorConversa = new HashMap<>();
    private final Map<String, Integer> naoLidasPorConversa = new HashMap<>();
    private final Set<String> conversasComHistoricoCarregado = new HashSet<>();
    private final Set<String> meusGrupos = new LinkedHashSet<>();
    private final Set<String> usuariosConhecidos = new LinkedHashSet<>();

    private Tema temaAtual = Tema.ESCURO;
    private ConexaoRedeCliente conexao;
    private String nickname;

    public ClienteChatGui() {
        super("Chat Multiusuário POO");

        setSize(880, 600);
        setMinimumSize(new Dimension(680, 450));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Painel Superior (Header com Status, Conversa Ativa e Seletor de Tema)
        painelTopo = new JPanel(new BorderLayout());
        painelTopo.setBorder(new EmptyBorder(10, 16, 10, 16));

        labelStatus = new JLabel("● Desconectado");
        labelStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        painelTopo.add(labelStatus, BorderLayout.WEST);

        labelConversaAtiva = new JLabel("Selecione um contato ou grupo ao lado", SwingConstants.CENTER);
        labelConversaAtiva.setFont(new Font("Segoe UI", Font.BOLD, 14));
        painelTopo.add(labelConversaAtiva, BorderLayout.CENTER);

        JPanel painelDireitaTopo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        painelDireitaTopo.setOpaque(false);

        JLabel labelTema = new JLabel("Tema:");
        labelTema.setFont(new Font("Segoe UI", Font.PLAIN, 12));

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

        // 2. Painel Lateral Esquerdo (Sidebar Estilo WhatsApp)
        painelSidebar = new JPanel(new BorderLayout(0, 8));
        painelSidebar.setPreferredSize(new Dimension(240, 0));
        painelSidebar.setBorder(new EmptyBorder(8, 12, 14, 8));

        painelTopoSidebar = new JPanel(new BorderLayout(6, 6));
        painelTopoSidebar.setOpaque(false);

        JLabel tituloSidebar = new JLabel("CONVERSAS & GRUPOS");
        tituloSidebar.setFont(new Font("Segoe UI", Font.BOLD, 11));

        botaoNovoGrupo = new JButton("+ Novo Grupo");
        botaoNovoGrupo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botaoNovoGrupo.setFocusPainted(false);
        botaoNovoGrupo.setBorderPainted(false);
        botaoNovoGrupo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botaoNovoGrupo.setBorder(new EmptyBorder(7, 10, 7, 10));
        botaoNovoGrupo.setEnabled(false);
        botaoNovoGrupo.addActionListener(e -> dialogCriarGrupo());

        painelTopoSidebar.add(tituloSidebar, BorderLayout.NORTH);
        painelTopoSidebar.add(botaoNovoGrupo, BorderLayout.CENTER);
        painelSidebar.add(painelTopoSidebar, BorderLayout.NORTH);

        listaConversas = new JList<>(modeloConversas);
        listaConversas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaConversas.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        listaConversas.setFixedCellHeight(36);
        listaConversas.setCellRenderer(new RenderizadorConversa());
        listaConversas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selecionada = listaConversas.getSelectedValue();
                if (selecionada != null && !selecionada.equals(conversaAtiva)) {
                    selecionarConversa(selecionada);
                }
            }
        });

        scrollSidebar = new JScrollPane(listaConversas);
        scrollSidebar.setBorder(BorderFactory.createEmptyBorder());
        painelSidebar.add(scrollSidebar, BorderLayout.CENTER);

        add(painelSidebar, BorderLayout.WEST);

        // 3. Painel Central (Mensagens HTML da conversa selecionada)
        areaChat = new JTextPane();
        areaChat.setEditable(false);
        areaChat.setContentType("text/html");

        kitHtml = new HTMLEditorKit();
        areaChat.setEditorKit(kitHtml);
        docHtml = (HTMLDocument) areaChat.getDocument();

        scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 16));
        scrollChat.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollChat, BorderLayout.CENTER);

        // 4. Painel Inferior (Barra de Envio)
        painelInferior = new JPanel(new BorderLayout(10, 0));
        painelInferior.setBorder(new EmptyBorder(10, 8, 14, 16));

        campoTexto = new JTextField();
        campoTexto.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        campoTexto.setEnabled(false);

        botaoEnviar = new JButton("Enviar");
        botaoEnviar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        botaoEnviar.setFocusPainted(false);
        botaoEnviar.setBorderPainted(false);
        botaoEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botaoEnviar.setBorder(new EmptyBorder(8, 22, 8, 22));
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

        JPanel painelDireito = new JPanel(new BorderLayout());
        painelDireito.setOpaque(false);
        painelDireito.add(scrollChat, BorderLayout.CENTER);
        painelDireito.add(painelInferior, BorderLayout.SOUTH);
        add(painelDireito, BorderLayout.CENTER);

        // Ações de envio
        botaoEnviar.addActionListener(e -> enviarMensagem());
        campoTexto.addActionListener(e -> enviarMensagem());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fecharAplicacao();
            }
        });

        atualizarModeloSidebar();
        aplicarTema(Tema.ESCURO);
    }

    /**
     * Abre uma janela permitindo digitar o nome do grupo e marcar com checkboxes
     * quais pessoas conectadas serao adicionadas ao grupo.
     */
    private void dialogCriarGrupo() {
        JPanel painel = new JPanel(new BorderLayout(8, 10));
        painel.setPreferredSize(new Dimension(320, 260));

        JPanel painelNome = new JPanel(new BorderLayout(4, 4));
        painelNome.add(new JLabel("Nome do Grupo:"), BorderLayout.NORTH);
        JTextField campoNomeGrupo = new JTextField();
        painelNome.add(campoNomeGrupo, BorderLayout.CENTER);
        painel.add(painelNome, BorderLayout.NORTH);

        JPanel painelPessoas = new JPanel();
        painelPessoas.setLayout(new BoxLayout(painelPessoas, BoxLayout.Y_AXIS));
        List<JCheckBox> checkboxes = new ArrayList<>();

        for (String user : usuariosConhecidos) {
            if (nickname == null || !user.equalsIgnoreCase(nickname)) {
                JCheckBox cb = new JCheckBox("@" + user);
                cb.putClientProperty("username", user);
                cb.setSelected(true); // por padrao ja vem marcado para facilitar
                checkboxes.add(cb);
                painelPessoas.add(cb);
            }
        }

        if (checkboxes.isEmpty()) {
            JLabel vazio = new JLabel("<html><i>Nenhum outro usuário online no momento.<br>Você ainda pode criar o grupo!</i></html>");
            vazio.setBorder(new EmptyBorder(8, 4, 8, 4));
            painelPessoas.add(vazio);
        }

        JScrollPane scrollPessoas = new JScrollPane(painelPessoas);
        scrollPessoas.setBorder(BorderFactory.createTitledBorder("Escolha as pessoas para adicionar:"));
        painel.add(scrollPessoas, BorderLayout.CENTER);

        int res = JOptionPane.showConfirmDialog(
                this,
                painel,
                "Novo Grupo",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (res != JOptionPane.OK_OPTION) {
            return;
        }

        String nome = campoNomeGrupo.getText().trim();
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe um nome para o grupo.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String grupo = nome.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (!grupo.startsWith("#")) {
            grupo = "#" + grupo;
        }

        List<String> escolhidos = new ArrayList<>();
        for (JCheckBox cb : checkboxes) {
            if (cb.isSelected()) {
                escolhidos.add((String) cb.getClientProperty("username"));
            }
        }

        meusGrupos.add(grupo);
        conversasComHistoricoCarregado.add(grupo);
        atualizarModeloSidebar();
        selecionarConversa(grupo);

        final String cmd = Protocolo.COMANDO_CRIAR_GRUPO + grupo + " " + String.join(",", escolhidos);
        new Thread(() -> conexao.enviar(cmd), "thread-criar-grupo").start();
    }

    private void selecionarConversa(String alvo) {
        conversaAtiva = alvo;
        naoLidasPorConversa.remove(alvo);
        listaConversas.setSelectedValue(alvo, true);
        listaConversas.repaint();

        if (alvo.startsWith("#")) {
            labelConversaAtiva.setText("👥 Grupo: " + alvo);
        } else if (alvo.startsWith("@")) {
            labelConversaAtiva.setText("💬 Conversa com " + alvo);
        }

        boolean podeEnviar = conexao != null && conexao.isConectado();
        campoTexto.setEnabled(podeEnviar);
        botaoEnviar.setEnabled(podeEnviar);

        recarregarHtmlDaConversaAtiva();

        // Na primeira vez que abre a conversa, carrega o historico do banco H2
        if (podeEnviar && conversasComHistoricoCarregado.add(alvo)) {
            new Thread(() -> conexao.enviar("/historico " + alvo), "thread-hist").start();
        }
        campoTexto.requestFocusInWindow();
    }

    private void recarregarHtmlDaConversaAtiva() {
        areaChat.setText("");
        docHtml = (HTMLDocument) areaChat.getDocument();
        aplicarRegrasCss();

        if (conversaAtiva == null) {
            try {
                String boasVindas = "<div style='text-align:center; margin-top: 120px;'>"
                        + "<h2 style='color:" + temaAtual.cssUserOther + ";'>💬 Suas Conversas</h2>"
                        + "<p style='color:" + temaAtual.cssTime + ";'>Clique em um <b>@usuário</b> na barra lateral para conversar no privado<br>"
                        + "ou clique em <b>+ Novo Grupo</b> para criar um grupo escolhendo os participantes.</p>"
                        + "</div>";
                kitHtml.insertHTML(docHtml, docHtml.getLength(), boasVindas, 0, 0, null);
            } catch (Exception ignored) {
            }
            return;
        }

        List<String> blocos = mensagensPorConversa.getOrDefault(conversaAtiva, List.of());
        for (String html : blocos) {
            try {
                kitHtml.insertHTML(docHtml, docHtml.getLength(), html, 0, 0, null);
            } catch (Exception ignored) {
            }
        }
        areaChat.setCaretPosition(docHtml.getLength());
    }

    private void adicionarMensagemNaConversa(String chaveConversa, String html) {
        if (chaveConversa == null) {
            return;
        }
        List<String> lista = mensagensPorConversa.computeIfAbsent(chaveConversa, k -> new ArrayList<>());
        if (!lista.isEmpty() && lista.get(lista.size() - 1).equals(html)) {
            return;
        }
        lista.add(html);

        if (chaveConversa.equalsIgnoreCase(conversaAtiva)) {
            try {
                kitHtml.insertHTML(docHtml, docHtml.getLength(), html, 0, 0, null);
                areaChat.setCaretPosition(docHtml.getLength());
            } catch (Exception ignored) {
            }
        } else {
            naoLidasPorConversa.merge(chaveConversa, 1, Integer::sum);
            listaConversas.repaint();
        }
    }

    private void atualizarModeloSidebar() {
        String selecionadaAntes = conversaAtiva;
        modeloConversas.clear();

        for (String grupo : meusGrupos) {
            modeloConversas.addElement(grupo);
        }
        for (String user : usuariosConhecidos) {
            if (nickname == null || !user.equalsIgnoreCase(nickname)) {
                modeloConversas.addElement("@" + user);
            }
        }

        if (selecionadaAntes != null && modeloConversas.contains(selecionadaAntes)) {
            listaConversas.setSelectedValue(selecionadaAntes, false);
        } else if (selecionadaAntes == null && !modeloConversas.isEmpty()) {
            // Seleciona automaticamente a primeira conversa quando aparecer
            selecionarConversa(modeloConversas.getElementAt(0));
        }
    }

    private void aplicarTema(Tema novoTema) {
        this.temaAtual = novoTema;

        getContentPane().setBackground(temaAtual.fundoJanela);
        painelTopo.setBackground(temaAtual.fundoJanela);
        painelTopo.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, temaAtual.fundoEntrada),
                new EmptyBorder(10, 16, 10, 16)
        ));
        painelSidebar.setBackground(temaAtual.fundoJanela);
        painelInferior.setBackground(temaAtual.fundoJanela);

        labelConversaAtiva.setForeground(temaAtual.textoBranco);

        botaoNovoGrupo.setBackground(temaAtual.botaoPrimario);
        botaoNovoGrupo.setForeground(Color.WHITE);

        listaConversas.setBackground(temaAtual.fundoChat);
        listaConversas.setForeground(temaAtual.textoBranco);
        scrollSidebar.getViewport().setBackground(temaAtual.fundoChat);
        scrollSidebar.setBorder(new LineBorder(temaAtual.fundoEntrada, 1, true));

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

        aplicarRegrasCss();
        recarregarHtmlDaConversaAtiva();
        repaint();
    }

    private void aplicarRegrasCss() {
        kitHtml.getStyleSheet().addRule("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 10px; background-color: " + temaAtual.cssFundoChat + "; color: " + temaAtual.cssTexto + "; font-size: 13px; }");
        kitHtml.getStyleSheet().addRule(".msg-box { margin-bottom: 8px; line-height: 1.4; }");
        kitHtml.getStyleSheet().addRule(".time { color: " + temaAtual.cssTime + "; font-size: 11px; margin-right: 6px; }");
        kitHtml.getStyleSheet().addRule(".user-me { color: " + temaAtual.cssUserMe + "; font-weight: bold; }");
        kitHtml.getStyleSheet().addRule(".user-other { color: " + temaAtual.cssUserOther + "; font-weight: bold; }");
        kitHtml.getStyleSheet().addRule(".content { color: " + temaAtual.cssTexto + "; }");
        kitHtml.getStyleSheet().addRule(".system { color: " + temaAtual.cssSystem + "; font-style: italic; margin-bottom: 6px; font-size: 12px; }");
        kitHtml.getStyleSheet().addRule(".error { color: " + temaAtual.cssError + "; font-weight: bold; margin-bottom: 6px; font-size: 12px; }");
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
                    botaoNovoGrupo.setEnabled(true);
                    if (conversaAtiva != null) {
                        campoTexto.setEnabled(true);
                        botaoEnviar.setEnabled(true);
                        campoTexto.requestFocusInWindow();
                    }
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
            // 1. Atualizacao de contatos e grupos: ~CONTATOS|u1,u2|#devs
            if (linha.startsWith(ChatService.PREFIXO_CONTATOS)) {
                processarAtualizacaoContatos(linha.substring(ChatService.PREFIXO_CONTATOS.length()));
                return;
            }

            // 2. Mensagem de grupo especifico: ~GRUPO|#devs|[dd/MM HH:mm] nome: texto
            if (linha.startsWith(ChatService.PREFIXO_GRUPO)) {
                String resto = linha.substring(ChatService.PREFIXO_GRUPO.length());
                int pipe = resto.indexOf('|');
                if (pipe != -1) {
                    String grupo = resto.substring(0, pipe).trim().toLowerCase(Locale.ROOT);
                    String msgFormatada = resto.substring(pipe + 1);
                    if (meusGrupos.add(grupo)) {
                        atualizarModeloSidebar();
                    }
                    adicionarMensagemNaConversa(grupo, formatarLinhaEmHtml(msgFormatada));
                    return;
                }
            }

            // 3. Mensagem privada: ~PRIV|remetente|destinatario|[dd/MM HH:mm] nome: texto
            if (linha.startsWith(ChatService.PREFIXO_PRIVADA)) {
                String resto = linha.substring(ChatService.PREFIXO_PRIVADA.length());
                String[] partes = resto.split("\\|", 3);
                if (partes.length == 3) {
                    String rem = partes[0].trim();
                    String dest = partes[1].trim();
                    String msgFormatada = partes[2];

                    String outroUsuario = (nickname != null && nickname.equalsIgnoreCase(rem)) ? dest : rem;
                    String chavePrivada = "@" + outroUsuario.toLowerCase(Locale.ROOT);
                    if (usuariosConhecidos.add(outroUsuario.toLowerCase(Locale.ROOT))) {
                        atualizarModeloSidebar();
                    }
                    adicionarMensagemNaConversa(chavePrivada, formatarLinhaEmHtml(msgFormatada));
                    return;
                }
            }

            // 4. Erros do protocolo (! ...)
            if (linha.startsWith(Protocolo.PREFIXO_ERRO)) {
                String msgErro = linha.substring(Protocolo.PREFIXO_ERRO.length());
                if (conversaAtiva != null) {
                    adicionarMensagemNaConversa(conversaAtiva, "<div class='error'>⚠ " + escaparHtml(msgErro) + "</div>");
                } else {
                    JOptionPane.showMessageDialog(this, msgErro, "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }
            // Ignora mensagens do antigo #geral na GUI
        });
    }

    private void processarAtualizacaoContatos(String payload) {
        String[] blocos = payload.split("\\|", -1);
        if (blocos.length >= 1 && !blocos[0].isBlank()) {
            for (String u : blocos[0].split(",")) {
                String nome = u.trim().toLowerCase(Locale.ROOT);
                if (!nome.isEmpty() && (nickname == null || !nome.equalsIgnoreCase(nickname))) {
                    usuariosConhecidos.add(nome);
                }
            }
        }
        if (blocos.length >= 2 && !blocos[1].isBlank()) {
            for (String g : blocos[1].split(",")) {
                String grupo = g.trim().toLowerCase(Locale.ROOT);
                if (!grupo.isEmpty()) {
                    meusGrupos.add(grupo);
                }
            }
        }
        atualizarModeloSidebar();
    }

    private String formatarLinhaEmHtml(String linha) {
        if (linha.startsWith("* ")) {
            return "<div class='system'>" + escaparHtml(linha) + "</div>";
        }
        if (linha.startsWith("[") && linha.contains("] ")) {
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

                return "<div class='msg-box'>"
                        + "<span class='time'>" + horario + "</span>"
                        + "<span class='" + classeUser + "'>" + tagRemetente + ": </span>"
                        + "<span class='content'>" + conteudo + "</span>"
                        + "</div>";
            }
        }
        return "<div class='msg-box'><span class='content'>" + escaparHtml(linha) + "</span></div>";
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
            if (conversaAtiva != null) {
                adicionarMensagemNaConversa(conversaAtiva, "<div class='system'>[ " + escaparHtml(status) + " ]</div>");
            }
            campoTexto.setEnabled(false);
            botaoEnviar.setEnabled(false);
            botaoNovoGrupo.setEnabled(false);
        });
    }

    private void enviarMensagem() {
        String texto = campoTexto.getText().trim();
        if (texto.isEmpty() || conversaAtiva == null) {
            return;
        }

        if (texto.equalsIgnoreCase(Protocolo.COMANDO_SAIR)) {
            fecharAplicacao();
            return;
        }

        campoTexto.setText("");
        final String destinoAtual = conversaAtiva;

        new Thread(() -> {
            if (texto.startsWith("/")) {
                conexao.enviar(texto);
                return;
            }
            if (destinoAtual.startsWith("#")) {
                conexao.enviar("/grupo " + destinoAtual + " " + texto);
            } else if (destinoAtual.startsWith("@")) {
                conexao.enviar("/msg " + destinoAtual.substring(1) + " " + texto);
            }
        }, "thread-envio-gui").start();
    }

    private void fecharAplicacao() {
        if (conexao != null) {
            conexao.desconectar();
        }
        dispose();
        System.exit(0);
    }

    /** Renderizador customizado para os itens da Sidebar (estilo WhatsApp). */
    private class RenderizadorConversa extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String item = String.valueOf(value);

            String icone = item.startsWith("#") ? "👥 " : "💬 ";
            int naoLidas = naoLidasPorConversa.getOrDefault(item, 0);
            String badge = naoLidas > 0 ? "   ● (" + naoLidas + ")" : "";

            label.setText(icone + item + badge);
            label.setBorder(new EmptyBorder(6, 12, 6, 10));
            label.setFont(new Font("Segoe UI", (isSelected || naoLidas > 0) ? Font.BOLD : Font.PLAIN, 13));

            if (isSelected) {
                label.setBackground(temaAtual.botaoPrimario);
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(temaAtual.fundoChat);
                label.setForeground(naoLidas > 0 ? new Color(87, 242, 135) : temaAtual.textoBranco);
            }
            return label;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteChatGui gui = new ClienteChatGui();
            gui.iniciar();
        });
    }
}
