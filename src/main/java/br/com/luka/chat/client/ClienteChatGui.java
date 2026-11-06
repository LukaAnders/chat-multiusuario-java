package br.com.luka.chat.client;

import br.com.luka.chat.server.Protocolo;
import br.com.luka.chat.service.ChatService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
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
 * Cliente com Interface Grafica Moderna Estilo WhatsApp/Discord:
 * - Avatares circulares desenhados com Graphics2D (sem quadradinhos quebrados)
 * - Baloes de mensagem estilizados
 * - Seletor popup de Emojis
 * - Modais customizados (Conectar e Criar Grupo com participantes)
 * - Seletor dinamico de 4 temas
 */
public class ClienteChatGui extends JFrame {

    public enum Tema {
        ESCURO("Escuro (Discord)",
                new Color(30, 31, 34), new Color(43, 45, 49), new Color(56, 58, 64),
                new Color(242, 243, 245), new Color(160, 168, 180),
                new Color(88, 101, 242), new Color(71, 82, 196),
                "#2b2d31", "#35373c", "#233b2e", "#f2f3f5", "#57f287", "#00b0f4", "#949ba4", "#fee75c", "#ed4245"),

        CLARO("Claro (Clean)",
                new Color(240, 242, 245), new Color(255, 255, 255), new Color(228, 231, 236),
                new Color(28, 30, 33), new Color(101, 103, 107),
                new Color(0, 132, 255), new Color(0, 110, 215),
                "#ffffff", "#f0f2f5", "#e7f3ff", "#1c1e21", "#1b873f", "#0066cc", "#65676b", "#b58105", "#d83a56"),

        DRACULA("Drácula (Roxo)",
                new Color(33, 34, 44), new Color(40, 42, 54), new Color(68, 71, 90),
                new Color(248, 248, 242), new Color(189, 147, 249),
                new Color(189, 147, 249), new Color(165, 120, 230),
                "#282a36", "#343746", "#2d3f3a", "#f8f8f2", "#50fa7b", "#8be9fd", "#92a2d4", "#f1fa8c", "#ff5555"),

        ESMERALDA("Esmeralda (WhatsApp)",
                new Color(17, 27, 33), new Color(32, 44, 51), new Color(42, 57, 66),
                new Color(233, 237, 239), new Color(134, 150, 160),
                new Color(0, 168, 132), new Color(0, 143, 112),
                "#111b21", "#202c33", "#005c4b", "#e9edef", "#53bdeb", "#00a884", "#8696a0", "#ffd279", "#f15c6d");

        final String nome;
        final Color fundoJanela;
        final Color fundoChat;
        final Color fundoEntrada;
        final Color textoBranco;
        final Color textoSecundario;
        final Color botaoPrimario;
        final Color botaoHover;

        final String cssFundoChat;
        final String cssBolhaOutro;
        final String cssBolhaEu;
        final String cssTexto;
        final String cssUserMe;
        final String cssUserOther;
        final String cssTime;
        final String cssSystem;
        final String cssError;

        Tema(String nome, Color fundoJanela, Color fundoChat, Color fundoEntrada,
             Color textoBranco, Color textoSecundario, Color botaoPrimario, Color botaoHover,
             String cssFundoChat, String cssBolhaOutro, String cssBolhaEu,
             String cssTexto, String cssUserMe, String cssUserOther,
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
            this.cssBolhaOutro = cssBolhaOutro;
            this.cssBolhaEu = cssBolhaEu;
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

    private static final String[] EMOJIS = {
        "😀", "😂", "🤣", "😊", "😍", "😎",
        "🤔", "😅", "😢", "😭", "😡", "🤯",
        "👍", "👎", "👏", "🙌", "🙏", "🤝",
        "❤️", "🔥", "✨", "🎉", "💯", "🚀",
        "✅", "❌", "⚡", "💻", "☕", "👀"
    };

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
    private final JButton botaoEmoji;
    private final JButton botaoEnviar;
    private final JButton botaoNovoGrupo;
    private final JLabel labelStatus;
    private final JLabel labelConversaAtiva;
    private final JLabel labelTituloSidebar;
    private final JLabel labelTema;
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

    private Tema temaAtual = Tema.ESMERALDA;
    private ConexaoRedeCliente conexao;
    private String nickname;

    public ClienteChatGui() {
        super("Chat Multiusuário POO");

        setSize(920, 620);
        setMinimumSize(new Dimension(720, 480));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Painel Superior (Header com Status, Conversa Ativa e Seletor de Tema)
        painelTopo = new JPanel(new BorderLayout());
        painelTopo.setBorder(new EmptyBorder(12, 18, 12, 18));

        labelStatus = new JLabel("● Desconectado");
        labelStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        painelTopo.add(labelStatus, BorderLayout.WEST);

        labelConversaAtiva = new JLabel("Selecione uma conversa ao lado", SwingConstants.CENTER);
        labelConversaAtiva.setFont(new Font("Segoe UI", Font.BOLD, 15));
        painelTopo.add(labelConversaAtiva, BorderLayout.CENTER);

        JPanel painelDireitaTopo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        painelDireitaTopo.setOpaque(false);

        labelTema = new JLabel("Tema:");
        labelTema.setFont(new Font("Segoe UI", Font.BOLD, 12));

        comboTemas = new JComboBox<>(Tema.values());
        comboTemas.setSelectedItem(temaAtual);
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
        painelSidebar = new JPanel(new BorderLayout(0, 10));
        painelSidebar.setPreferredSize(new Dimension(260, 0));
        painelSidebar.setBorder(new EmptyBorder(12, 14, 14, 10));

        painelTopoSidebar = new JPanel(new BorderLayout(0, 8));
        painelTopoSidebar.setOpaque(false);

        labelTituloSidebar = new JLabel("CONVERSAS & GRUPOS");
        labelTituloSidebar.setFont(new Font("Segoe UI", Font.BOLD, 11));

        botaoNovoGrupo = criarBotaoEstilizado("+ Criar Novo Grupo", true);
        botaoNovoGrupo.setEnabled(false);
        botaoNovoGrupo.addActionListener(e -> dialogCriarGrupoModerno());

        painelTopoSidebar.add(labelTituloSidebar, BorderLayout.NORTH);
        painelTopoSidebar.add(botaoNovoGrupo, BorderLayout.CENTER);
        painelSidebar.add(painelTopoSidebar, BorderLayout.NORTH);

        listaConversas = new JList<>(modeloConversas);
        listaConversas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaConversas.setFixedCellHeight(48);
        listaConversas.setCellRenderer(new RenderizadorConversaModerno());
        listaConversas.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
        estilizarScrollBar(scrollSidebar);
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
        scrollChat.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 16));
        scrollChat.getVerticalScrollBar().setUnitIncrement(18);
        estilizarScrollBar(scrollChat);

        // 4. Painel Inferior (Botão de Emoji Colorido + Campo de Texto + Botão Enviar)
        painelInferior = new JPanel(new BorderLayout(8, 0));
        painelInferior.setBorder(new EmptyBorder(10, 6, 14, 16));

        botaoEmoji = new JButton(EmojiUtil.obterIcone("😊", 20));
        botaoEmoji.setToolTipText("Inserir Emoji Colorido");
        botaoEmoji.setFocusPainted(false);
        botaoEmoji.setBorderPainted(false);
        botaoEmoji.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botaoEmoji.setBorder(new EmptyBorder(6, 10, 6, 10));
        botaoEmoji.setEnabled(false);
        botaoEmoji.addActionListener(e -> mostrarSeletorEmojis());

        EmojiUtil.preCarregarAssincrono(() -> {
            botaoEmoji.setIcon(EmojiUtil.obterIcone("😊", 20));
            recarregarHtmlDaConversaAtiva();
        });

        campoTexto = new JTextField();
        campoTexto.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        campoTexto.setEnabled(false);

        botaoEnviar = criarBotaoEstilizado("Enviar  ➤", true);
        botaoEnviar.setEnabled(false);

        painelInferior.add(botaoEmoji, BorderLayout.WEST);
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
        aplicarTema(temaAtual);
    }

    private JButton criarBotaoEstilizado(String texto, boolean primario) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(9, 18, 9, 18));

        if (primario) {
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (btn.isEnabled()) {
                        btn.setBackground(temaAtual.botaoHover);
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (btn.isEnabled()) {
                        btn.setBackground(temaAtual.botaoPrimario);
                    }
                }
            });
        }
        return btn;
    }

    private void estilizarScrollBar(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(100, 105, 115, 140);
                this.trackColor = temaAtual.fundoChat;
            }
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return botaoZero();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return botaoZero();
            }
            private JButton botaoZero() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    /**
     * Abre o popup com grade de emojis COLORIDOS (PNG Twemoji) para inserir no campo de texto.
     */
    private void mostrarSeletorEmojis() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(temaAtual.fundoJanela);
        popup.setBorder(new LineBorder(temaAtual.botaoPrimario, 1, true));

        JPanel grade = new JPanel(new GridLayout(6, 6, 4, 4));
        grade.setBackground(temaAtual.fundoJanela);
        grade.setBorder(new EmptyBorder(8, 8, 8, 8));

        for (String emoji : EmojiUtil.getMapaEmojis().keySet()) {
            JButton btn = new JButton(EmojiUtil.obterIcone(emoji, 22));
            btn.setToolTipText(emoji);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setBackground(temaAtual.fundoEntrada);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setBorder(new EmptyBorder(6, 6, 6, 6));

            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(temaAtual.botaoPrimario);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(temaAtual.fundoEntrada);
                }
            });

            btn.addActionListener(e -> {
                campoTexto.setText(campoTexto.getText() + emoji);
                popup.setVisible(false);
                campoTexto.requestFocusInWindow();
            });
            grade.add(btn);
        }

        popup.add(grade);
        popup.show(botaoEmoji, 0, -255);
    }

    /**
     * Modal customizado e estilizado para Criar um Grupo e selecionar as pessoas.
     */
    private void dialogCriarGrupoModerno() {
        JDialog dialogo = new JDialog(this, "Criar Novo Grupo", true);
        dialogo.setSize(380, 410);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout());
        dialogo.getContentPane().setBackground(temaAtual.fundoJanela);

        JPanel conteudo = new JPanel(new BorderLayout(0, 12));
        conteudo.setOpaque(false);
        conteudo.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Topo: Título e Campo de Nome
        JPanel painelTopoModal = new JPanel(new BorderLayout(0, 6));
        painelTopoModal.setOpaque(false);

        JLabel titulo = new JLabel("Novo Grupo de Conversa");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(temaAtual.textoBranco);

        JLabel subLabel = new JLabel("Nome do grupo:");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(temaAtual.textoSecundario);

        JTextField campoNomeGrupo = criarCampoTextoModal("");

        JPanel boxTitulo = new JPanel(new GridLayout(2, 1, 0, 4));
        boxTitulo.setOpaque(false);
        boxTitulo.add(titulo);
        boxTitulo.add(subLabel);

        painelTopoModal.add(boxTitulo, BorderLayout.NORTH);
        painelTopoModal.add(campoNomeGrupo, BorderLayout.CENTER);
        conteudo.add(painelTopoModal, BorderLayout.NORTH);

        // Centro: Lista de Checkboxes estilizados com os contatos
        JPanel painelLista = new JPanel();
        painelLista.setLayout(new BoxLayout(painelLista, BoxLayout.Y_AXIS));
        painelLista.setBackground(temaAtual.fundoChat);
        painelLista.setBorder(new EmptyBorder(8, 10, 8, 10));

        List<JCheckBox> checkboxes = new ArrayList<>();
        for (String user : usuariosConhecidos) {
            if (nickname == null || !user.equalsIgnoreCase(nickname)) {
                JCheckBox cb = new JCheckBox("  @" + user);
                cb.putClientProperty("username", user);
                cb.setSelected(true);
                cb.setFont(new Font("Segoe UI", Font.BOLD, 13));
                cb.setBackground(temaAtual.fundoChat);
                cb.setForeground(temaAtual.textoBranco);
                cb.setFocusPainted(false);
                cb.setCursor(new Cursor(Cursor.HAND_CURSOR));
                cb.setBorder(new EmptyBorder(6, 4, 6, 4));
                checkboxes.add(cb);
                painelLista.add(cb);
            }
        }

        if (checkboxes.isEmpty()) {
            JLabel avisoVazio = new JLabel("<html><div style='padding:10px;'>Nenhum outro contato conectado ainda.<br>Você já pode criar o grupo!</div></html>");
            avisoVazio.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            avisoVazio.setForeground(temaAtual.textoSecundario);
            painelLista.add(avisoVazio);
        }

        JPanel wrapperCentro = new JPanel(new BorderLayout(0, 6));
        wrapperCentro.setOpaque(false);
        JLabel labelParticipantes = new JLabel("Selecione os participantes:");
        labelParticipantes.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        labelParticipantes.setForeground(temaAtual.textoSecundario);

        JScrollPane scrollPessoas = new JScrollPane(painelLista);
        scrollPessoas.setBorder(new LineBorder(temaAtual.fundoEntrada, 1, true));
        estilizarScrollBar(scrollPessoas);

        wrapperCentro.add(labelParticipantes, BorderLayout.NORTH);
        wrapperCentro.add(scrollPessoas, BorderLayout.CENTER);
        conteudo.add(wrapperCentro, BorderLayout.CENTER);

        // Rodapé: Botões Cancelar e Criar
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        painelBotoes.setOpaque(false);

        JButton btnCancelar = criarBotaoEstilizado("Cancelar", false);
        btnCancelar.setBackground(temaAtual.fundoEntrada);
        btnCancelar.setForeground(temaAtual.textoBranco);
        btnCancelar.addActionListener(e -> dialogo.dispose());

        JButton btnCriar = criarBotaoEstilizado("Criar Grupo", true);
        btnCriar.setBackground(temaAtual.botaoPrimario);
        btnCriar.setForeground(Color.WHITE);

        Runnable acaoConfirmar = () -> {
            String nome = campoNomeGrupo.getText().trim();
            if (nome.isEmpty()) {
                campoNomeGrupo.setBorder(new LineBorder(new Color(237, 66, 69), 2, true));
                campoNomeGrupo.requestFocusInWindow();
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
            dialogo.dispose();
        };

        btnCriar.addActionListener(e -> acaoConfirmar.run());
        campoNomeGrupo.addActionListener(e -> acaoConfirmar.run());

        painelBotoes.add(btnCancelar);
        painelBotoes.add(btnCriar);
        conteudo.add(painelBotoes, BorderLayout.SOUTH);

        dialogo.add(conteudo);
        SwingUtilities.invokeLater(campoNomeGrupo::requestFocusInWindow);
        dialogo.setVisible(true);
    }

    private JTextField criarCampoTextoModal(String valorInicial) {
        JTextField tf = new JTextField(valorInicial);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setBackground(temaAtual.fundoEntrada);
        tf.setForeground(temaAtual.textoBranco);
        tf.setCaretColor(temaAtual.textoBranco);
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(temaAtual.botaoPrimario, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return tf;
    }

    private void selecionarConversa(String alvo) {
        conversaAtiva = alvo;
        naoLidasPorConversa.remove(alvo);
        listaConversas.setSelectedValue(alvo, true);
        listaConversas.repaint();

        if (alvo.startsWith("#")) {
            labelConversaAtiva.setText("Grupo  " + alvo);
        } else if (alvo.startsWith("@")) {
            labelConversaAtiva.setText("Conversa com  " + alvo);
        }

        boolean podeEnviar = conexao != null && conexao.isConectado();
        campoTexto.setEnabled(podeEnviar);
        botaoEnviar.setEnabled(podeEnviar);
        botaoEmoji.setEnabled(podeEnviar);

        recarregarHtmlDaConversaAtiva();

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
                String boasVindas = "<div style='text-align:center; margin-top: 130px;'>"
                        + "<h2 style='color:" + temaAtual.cssUserOther + "; margin-bottom:8px;'>Suas Conversas</h2>"
                        + "<p style='color:" + temaAtual.cssTime + "; line-height:1.6;'>"
                        + "Selecione um <b>@contato</b> na barra lateral para conversar no privado<br>"
                        + "ou clique em <b>+ Criar Novo Grupo</b> para montar um grupo com seus colegas.</p>"
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
            selecionarConversa(modeloConversas.getElementAt(0));
        }
    }

    private void aplicarTema(Tema novoTema) {
        this.temaAtual = novoTema;

        getContentPane().setBackground(temaAtual.fundoJanela);
        painelTopo.setBackground(temaAtual.fundoJanela);
        painelTopo.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, temaAtual.fundoEntrada),
                new EmptyBorder(12, 18, 12, 18)
        ));
        painelSidebar.setBackground(temaAtual.fundoJanela);
        painelInferior.setBackground(temaAtual.fundoJanela);

        labelConversaAtiva.setForeground(temaAtual.textoBranco);
        labelTituloSidebar.setForeground(temaAtual.textoSecundario);
        labelTema.setForeground(temaAtual.textoSecundario);

        botaoNovoGrupo.setBackground(temaAtual.botaoPrimario);
        botaoNovoGrupo.setForeground(Color.WHITE);

        listaConversas.setBackground(temaAtual.fundoChat);
        listaConversas.setForeground(temaAtual.textoBranco);
        scrollSidebar.getViewport().setBackground(temaAtual.fundoChat);
        scrollSidebar.setBorder(new LineBorder(temaAtual.fundoEntrada, 1, true));

        scrollChat.setBackground(temaAtual.fundoJanela);
        scrollChat.getViewport().setBackground(temaAtual.fundoChat);
        areaChat.setBackground(temaAtual.fundoChat);

        botaoEmoji.setBackground(temaAtual.fundoEntrada);
        botaoEmoji.setForeground(temaAtual.textoBranco);

        campoTexto.setBackground(temaAtual.fundoEntrada);
        campoTexto.setForeground(temaAtual.textoBranco);
        campoTexto.setCaretColor(temaAtual.textoBranco);
        campoTexto.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(temaAtual.fundoEntrada.darker(), 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));

        botaoEnviar.setBackground(temaAtual.botaoPrimario);
        botaoEnviar.setForeground(Color.WHITE);

        comboTemas.setBackground(temaAtual.fundoEntrada);
        comboTemas.setForeground(temaAtual.textoBranco);

        estilizarScrollBar(scrollChat);
        estilizarScrollBar(scrollSidebar);
        aplicarRegrasCss();
        recarregarHtmlDaConversaAtiva();
        repaint();
    }

    private void aplicarRegrasCss() {
        kitHtml.getStyleSheet().addRule("body { font-family: 'Segoe UI', 'Segoe UI Emoji', Arial, sans-serif; margin: 14px; background-color: " + temaAtual.cssFundoChat + "; color: " + temaAtual.cssTexto + "; font-size: 13px; }");
        kitHtml.getStyleSheet().addRule(".bubble-other { background-color: " + temaAtual.cssBolhaOutro + "; padding: 8px 12px; margin-bottom: 10px; border-left: 3px solid " + temaAtual.cssUserOther + "; }");
        kitHtml.getStyleSheet().addRule(".bubble-me { background-color: " + temaAtual.cssBolhaEu + "; padding: 8px 12px; margin-bottom: 10px; border-left: 3px solid " + temaAtual.cssUserMe + "; }");
        kitHtml.getStyleSheet().addRule(".header-line { margin-bottom: 3px; }");
        kitHtml.getStyleSheet().addRule(".user-me { color: " + temaAtual.cssUserMe + "; font-weight: bold; font-size: 12px; }");
        kitHtml.getStyleSheet().addRule(".user-other { color: " + temaAtual.cssUserOther + "; font-weight: bold; font-size: 12px; }");
        kitHtml.getStyleSheet().addRule(".time { color: " + temaAtual.cssTime + "; font-size: 10px; margin-left: 8px; }");
        kitHtml.getStyleSheet().addRule(".content { color: " + temaAtual.cssTexto + "; font-size: 13.5px; line-height: 1.4; }");
        kitHtml.getStyleSheet().addRule(".system { color: " + temaAtual.cssSystem + "; font-style: italic; text-align: center; margin: 10px 0; font-size: 12px; }");
        kitHtml.getStyleSheet().addRule(".error { color: " + temaAtual.cssError + "; font-weight: bold; margin: 8px 0; font-size: 12px; }");
    }

    /**
     * Modal customizado de Login / Conexao ao Servidor.
     */
    public void iniciar() {
        setVisible(true);

        JDialog modalLogin = new JDialog(this, "Entrar no Chat", true);
        modalLogin.setSize(370, 310);
        modalLogin.setLocationRelativeTo(this);
        modalLogin.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        modalLogin.setLayout(new BorderLayout());
        modalLogin.getContentPane().setBackground(temaAtual.fundoJanela);

        modalLogin.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                modalLogin.dispose();
                dispose();
                System.exit(0);
            }
        });

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel titulo = new JLabel("Bem-vindo ao Chat");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(temaAtual.textoBranco);

        JLabel lblNick = new JLabel("Como você quer ser chamado (Nickname)?");
        lblNick.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblNick.setForeground(temaAtual.textoSecundario);
        JTextField campoNick = criarCampoTextoModal("");

        JLabel lblServidor = new JLabel("Servidor (Host e Porta):");
        lblServidor.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblServidor.setForeground(temaAtual.textoSecundario);

        JPanel painelHostPorta = new JPanel(new BorderLayout(8, 0));
        painelHostPorta.setOpaque(false);
        JTextField campoHost = criarCampoTextoModal("localhost");
        JTextField campoPorta = criarCampoTextoModal("5000");
        campoPorta.setPreferredSize(new Dimension(85, 0));
        painelHostPorta.add(campoHost, BorderLayout.CENTER);
        painelHostPorta.add(campoPorta, BorderLayout.EAST);

        JButton btnEntrar = criarBotaoEstilizado("Entrar no Chat  ➤", true);
        btnEntrar.setBackground(temaAtual.botaoPrimario);
        btnEntrar.setForeground(Color.WHITE);

        Runnable acaoConectar = () -> {
            String nickDigitado = campoNick.getText().trim();
            if (nickDigitado.isEmpty() || nickDigitado.contains(" ")) {
                campoNick.setBorder(new LineBorder(new Color(237, 66, 69), 2, true));
                campoNick.requestFocusInWindow();
                return;
            }
            nickname = nickDigitado;
            int porta = 5000;
            try {
                porta = Integer.parseInt(campoPorta.getText().trim());
            } catch (NumberFormatException ignored) {
            }
            String host = campoHost.getText().trim();
            modalLogin.dispose();
            conectarAoServidor(host, porta, nickname);
        };

        btnEntrar.addActionListener(e -> acaoConectar.run());
        campoNick.addActionListener(e -> acaoConectar.run());

        form.add(titulo);
        form.add(lblNick);
        form.add(campoNick);
        form.add(lblServidor);
        form.add(painelHostPorta);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.setBorder(new EmptyBorder(4, 24, 20, 24));
        rodape.add(btnEntrar, BorderLayout.CENTER);

        modalLogin.add(form, BorderLayout.CENTER);
        modalLogin.add(rodape, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(campoNick::requestFocusInWindow);
        modalLogin.setVisible(true);
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
                        botaoEmoji.setEnabled(true);
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
                    adicionarMensagemNaConversa(conversaAtiva, "<div class='error'>! " + escaparHtml(msgErro) + "</div>");
                } else {
                    JOptionPane.showMessageDialog(this, msgErro, "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }
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
            return "<div class='system'>" + EmojiUtil.substituirEmojisPorImgHtml(escaparHtml(linha)) + "</div>";
        }
        if (linha.startsWith("[") && linha.contains("] ")) {
            int fecharColchete = linha.indexOf("] ");
            String horario = escaparHtml(linha.substring(1, fecharColchete));
            String resto = linha.substring(fecharColchete + 2);

            int doisPontos = resto.indexOf(": ");
            if (doisPontos != -1) {
                String remetente = resto.substring(0, doisPontos);
                String conteudo = EmojiUtil.substituirEmojisPorImgHtml(escaparHtml(resto.substring(doisPontos + 2)));

                boolean souEu = nickname != null && nickname.equalsIgnoreCase(remetente.trim());
                String classeBolha = souEu ? "bubble-me" : "bubble-other";
                String classeUser = souEu ? "user-me" : "user-other";
                String tagRemetente = souEu ? "Você" : escaparHtml(remetente);

                return "<div class='" + classeBolha + "'>"
                        + "<div class='header-line'>"
                        + "<span class='" + classeUser + "'>" + tagRemetente + "</span>"
                        + "<span class='time'>  " + horario + "</span>"
                        + "</div>"
                        + "<div class='content'>" + conteudo + "</div>"
                        + "</div>";
            }
        }
        return "<div class='bubble-other'><div class='content'>"
                + EmojiUtil.substituirEmojisPorImgHtml(escaparHtml(linha)) + "</div></div>";
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
            botaoEmoji.setEnabled(false);
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

    /**
     * Renderizador customizado da Sidebar que desenha avatares circulares (com as
     * iniciais do contato ou '#' para grupos) e badge de nao lidas via Graphics2D,
     * eliminando qualquer problema de quadradinhos de fonte!
     */
    private class RenderizadorConversaModerno extends JPanel implements ListCellRenderer<String> {

        private String itemTexto = "";
        private boolean selecionado = false;
        private int naoLidas = 0;

        public RenderizadorConversaModerno() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            this.itemTexto = value != null ? value : "";
            this.selecionado = isSelected;
            this.naoLidas = naoLidasPorConversa.getOrDefault(this.itemTexto, 0);
            setBackground(isSelected ? temaAtual.botaoPrimario : temaAtual.fundoChat);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            boolean ehGrupo = itemTexto.startsWith("#");
            String nomeLimpo = itemTexto.length() > 1 ? itemTexto.substring(1) : itemTexto;

            // 1. Desenha o Avatar Circular
            int tamAvatar = 32;
            int xAvatar = 12;
            int yAvatar = (getHeight() - tamAvatar) / 2;

            Color corAvatar = selecionado ? new Color(255, 255, 255, 50)
                            : ehGrupo ? new Color(88, 101, 242) : new Color(0, 168, 132);
            g2.setColor(corAvatar);
            g2.fillOval(xAvatar, yAvatar, tamAvatar, tamAvatar);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            String inicial = ehGrupo ? "#" : nomeLimpo.substring(0, 1).toUpperCase(Locale.ROOT);
            FontMetrics fmAvatar = g2.getFontMetrics();
            int tx = xAvatar + (tamAvatar - fmAvatar.stringWidth(inicial)) / 2;
            int ty = yAvatar + ((tamAvatar - fmAvatar.getHeight()) / 2) + fmAvatar.getAscent();
            g2.drawString(inicial, tx, ty);

            // 2. Desenha o Nome e Subtítulo
            int xTexto = xAvatar + tamAvatar + 12;
            g2.setColor(selecionado ? Color.WHITE : temaAtual.textoBranco);
            g2.setFont(new Font("Segoe UI", (selecionado || naoLidas > 0) ? Font.BOLD : Font.PLAIN, 13));
            g2.drawString(ehGrupo ? itemTexto : "@" + nomeLimpo, xTexto, getHeight() / 2 - 2);

            g2.setColor(selecionado ? new Color(230, 235, 245) : temaAtual.textoSecundario);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(ehGrupo ? "Grupo de conversa" : "Conversa privada", xTexto, getHeight() / 2 + 13);

            // 3. Desenha Badge de Mensagens Não Lidas (se houver)
            if (naoLidas > 0) {
                String txtBadge = String.valueOf(naoLidas);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fmBadge = g2.getFontMetrics();
                int largBadge = Math.max(20, fmBadge.stringWidth(txtBadge) + 10);
                int altBadge = 20;
                int xBadge = getWidth() - largBadge - 12;
                int yBadge = (getHeight() - altBadge) / 2;

                g2.setColor(new Color(35, 165, 89));
                g2.fillRoundRect(xBadge, yBadge, largBadge, altBadge, 10, 10);

                g2.setColor(Color.WHITE);
                int bx = xBadge + (largBadge - fmBadge.stringWidth(txtBadge)) / 2;
                int by = yBadge + ((altBadge - fmBadge.getHeight()) / 2) + fmBadge.getAscent();
                g2.drawString(txtBadge, bx, by);
            }

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteChatGui gui = new ClienteChatGui();
            gui.iniciar();
        });
    }
}
