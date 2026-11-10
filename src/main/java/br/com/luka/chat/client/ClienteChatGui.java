package br.com.luka.chat.client;

import br.com.luka.chat.bot.BotIaCliente;
import br.com.luka.chat.server.Protocolo;
import br.com.luka.chat.service.ChatService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
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
 * Cliente com Interface Grafica Moderna inspirada no WhatsApp Desktop:
 * - Layout dividido em Sidebar de Conversas (~30%) e Area da Conversa (~70%)
 * - Cabecalho de perfil do usuario conectado com avatar e status online
 * - Pesquisa em tempo real e filtros rapidos (Tudo, Nao lidas, Grupos)
 * - Lista de conversas com avatar, status online, previa e horario da ultima mensagem e badge de nao lidas
 * - Baloes de mensagem arredondados (enviadas a direita em verde suave #D9FDD3, recebidas a esquerda em branco #FFFFFF)
 * - Tela inicial ilustrativa quando nenhuma conversa esta selecionada
 * - Gerenciamento completo de grupos (Criar, Adicionar Pessoas, Sair e Apagar) e Emojis Coloridos Twemoji
 */
public class ClienteChatGui extends JFrame {

    public enum Tema {
        CLARO("WhatsApp Claro",
                new Color(245, 246, 248), // fundoJanela (#F5F6F8)
                new Color(255, 255, 255), // fundoSidebar (#FFFFFF)
                new Color(239, 239, 239), // fundoChat (#EFEFEF)
                new Color(255, 255, 255), // fundoEntrada (#FFFFFF)
                new Color(32, 33, 36),    // textoPrincipal (#202124)
                new Color(119, 119, 119), // textoSecundario (#777777)
                new Color(37, 211, 102),  // botaoPrimario (#25D366)
                new Color(29, 168, 81),   // botaoHover (#1DA851)
                new Color(217, 253, 211), // bolhaEnviada (#D9FDD3)
                new Color(255, 255, 255), // bolhaRecebida (#FFFFFF)
                new Color(226, 229, 233), // corBorda (#E2E5E9)
                new Color(240, 242, 245), // itemHover / selecionado suave
                "#efefef", "#ffffff", "#d9fdd3", "#202124", "#008069", "#0066cc", "#777777", "#54656f", "#d83a56"),

        ESCURO("WhatsApp Escuro",
                new Color(17, 27, 33),
                new Color(17, 27, 33),
                new Color(11, 20, 26),
                new Color(42, 57, 66),
                new Color(233, 237, 239),
                new Color(134, 150, 160),
                new Color(0, 168, 132),
                new Color(0, 143, 112),
                new Color(0, 92, 75),
                new Color(32, 44, 51),
                new Color(38, 52, 61),
                new Color(32, 44, 51),
                "#0b141a", "#202c33", "#005c4b", "#e9edef", "#00a884", "#53bdeb", "#8696a0", "#ffd279", "#f15c6d"),

        DRACULA("Drácula (Roxo)",
                new Color(33, 34, 44),
                new Color(40, 42, 54),
                new Color(30, 31, 41),
                new Color(68, 71, 90),
                new Color(248, 248, 242),
                new Color(150, 160, 195),
                new Color(189, 147, 249),
                new Color(165, 120, 230),
                new Color(68, 71, 90),
                new Color(45, 48, 62),
                new Color(58, 61, 78),
                new Color(52, 55, 70),
                "#1e1f29", "#2d303e", "#44475a", "#f8f8f2", "#50fa7b", "#8be9fd", "#92a2d4", "#f1fa8c", "#ff5555"),

        ESMERALDA("Esmeralda",
                new Color(240, 244, 243),
                new Color(255, 255, 255),
                new Color(229, 221, 213),
                new Color(255, 255, 255),
                new Color(32, 33, 36),
                new Color(102, 119, 129),
                new Color(0, 128, 105),
                new Color(0, 105, 86),
                new Color(217, 253, 211),
                new Color(255, 255, 255),
                new Color(218, 224, 227),
                new Color(235, 243, 240),
                "#e5ddd5", "#ffffff", "#d9fdd3", "#202124", "#008069", "#0066cc", "#667781", "#54656f", "#d83a56");

        final String nome;
        final Color fundoJanela;
        final Color fundoSidebar;
        final Color fundoChat;
        final Color fundoEntrada;
        final Color textoBranco; // Texto principal
        final Color textoSecundario;
        final Color botaoPrimario;
        final Color botaoHover;
        final Color corBolhaEnviada;
        final Color corBolhaRecebida;
        final Color corBorda;
        final Color corItemSelecionado;

        final String cssFundoChat;
        final String cssBolhaOutro;
        final String cssBolhaEu;
        final String cssTexto;
        final String cssUserMe;
        final String cssUserOther;
        final String cssTime;
        final String cssSystem;
        final String cssError;

        Tema(String nome, Color fundoJanela, Color fundoSidebar, Color fundoChat, Color fundoEntrada,
             Color textoBranco, Color textoSecundario, Color botaoPrimario, Color botaoHover,
             Color corBolhaEnviada, Color corBolhaRecebida, Color corBorda, Color corItemSelecionado,
             String cssFundoChat, String cssBolhaOutro, String cssBolhaEu,
             String cssTexto, String cssUserMe, String cssUserOther,
             String cssTime, String cssSystem, String cssError) {
            this.nome = nome;
            this.fundoJanela = fundoJanela;
            this.fundoSidebar = fundoSidebar;
            this.fundoChat = fundoChat;
            this.fundoEntrada = fundoEntrada;
            this.textoBranco = textoBranco;
            this.textoSecundario = textoSecundario;
            this.botaoPrimario = botaoPrimario;
            this.botaoHover = botaoHover;
            this.corBolhaEnviada = corBolhaEnviada;
            this.corBolhaRecebida = corBolhaRecebida;
            this.corBorda = corBorda;
            this.corItemSelecionado = corItemSelecionado;
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

    private enum FiltroConversa {
        TUDO, NAO_LIDAS, GRUPOS
    }

    private enum TipoItemChat {
        MENSAGEM, SISTEMA, ERRO
    }

    private record ItemMensagem(
            TipoItemChat tipo,
            String remetente,
            String conteudoTexto,
            String conteudoHtml,
            String horario,
            boolean enviadaPorMim
    ) {
    }

    private static final String CONVERSA_IA_PADRAO = "@gemini";
    private static final Color COR_IA_DESTAQUE = new Color(108, 92, 231);    // Roxo/Indigo IA
    private static final Color COR_VERDE_DESTAQUE = new Color(37, 211, 102); // #25D366
    private static final Color COR_STATUS_ONLINE = new Color(37, 211, 102);  // #25D366
    private static final Color COR_CHECK_LIDO = new Color(83, 189, 235);     // #53BDEB
    private static final Color COR_PERIGO = new Color(218, 55, 60);
    private static final Color COR_PERIGO_HOVER = new Color(185, 42, 47);

    private static final Color[] PALETA_AVATARES = {
            new Color(0, 168, 132),   // Verde esmeralda
            new Color(31, 122, 236),  // Azul vibrante
            new Color(127, 102, 255), // Roxo suave
            new Color(229, 66, 163),  // Rosa
            new Color(216, 88, 42),   // Laranja terroso
            new Color(2, 150, 179),   // Ciano escuro
            new Color(108, 117, 125)  // Cinza azulado
    };

    private static final String CARD_BOAS_VINDAS = "BOAS_VINDAS";
    private static final String CARD_CONVERSA = "CONVERSA";

    // Componentes estruturais
    private final JPanel painelTopo;
    private final JPanel painelSidebar;
    private final JPanel painelTopoSidebar;
    private final JPanel painelPerfilUsuario;
    private final JPanel painelBuscaContainer;
    private final JPanel painelCaixaBusca;
    private final JPanel painelFiltros;
    private final JPanel painelAcoesGrupo;
    private final JPanel painelInferior;
    private final JPanel painelCampoArredondado;
    private final JPanel painelDireitoCards;
    private final CardLayout cardLayoutDireito;
    private final PainelBoasVindas painelBoasVindas;
    private final JPanel painelConversaAtiva;
    private final PainelListaMensagens painelMensagens;
    private final JPanel painelListaVaziaSidebar;
    private final CardLayout cardLayoutSidebarLista;
    private final JPanel painelCentroSidebar;

    private final JScrollPane scrollChat;
    private final JScrollPane scrollSidebar;
    private final JTextPane areaChat;
    private final HTMLEditorKit kitHtml;
    private HTMLDocument docHtml;
    private final JTextField campoTexto;
    private final JTextField campoPesquisa;
    private final JButton botaoEmoji;
    private final JButton botaoEnviar;
    private final JButton botaoNovaConversa;
    private final JButton botaoNovoGrupo;
    private final JButton botaoFiltroTudo;
    private final JButton botaoFiltroNaoLidas;
    private final JButton botaoFiltroGrupos;
    private final JButton botaoAddMembroGrupo;
    private final JButton botaoSairGrupo;
    private final JButton botaoApagarGrupo;
    private final JButton botaoConfigurarKeyIa;
    private final AvatarComponente avatarMeuPerfil;
    private final AvatarComponente avatarConversaAtiva;
    private final JLabel labelNomeMeuPerfil;
    private final JLabel labelStatus;
    private final JLabel labelConversaAtiva;
    private final JLabel labelSubtituloConversa;
    private final JLabel labelTituloSidebar;
    private final JLabel labelTema;
    private final JComboBox<Tema> comboTemas;

    private final DefaultListModel<String> modeloConversas = new DefaultListModel<>();
    private final JList<String> listaConversas;

    // Estado das conversas (manipulado exclusivamente na EDT do Swing)
    private String conversaAtiva = null;
    private FiltroConversa filtroAtual = FiltroConversa.TUDO;
    private String termoPesquisa = "";
    private int itemHoverIndex = -1;

    private final Map<String, List<String>> mensagensPorConversa = new HashMap<>();
    private final Map<String, List<ItemMensagem>> itensMensagemPorConversa = new HashMap<>();
    private final Map<String, String> ultimaPreviaPorConversa = new HashMap<>();
    private final Map<String, String> ultimoHorarioPorConversa = new HashMap<>();
    private final Map<String, Integer> naoLidasPorConversa = new HashMap<>();
    private final Set<String> conversasComHistoricoCarregado = new HashSet<>();
    private final Set<String> meusGrupos = new LinkedHashSet<>();
    private final Set<String> usuariosConhecidos = new LinkedHashSet<>();
    private final Set<String> usuariosOnline = new HashSet<>();

    private Tema temaAtual = Tema.CLARO;
    private ConexaoRedeCliente conexao;
    private String nickname;

    public ClienteChatGui() {
        super("Chat Multiusuário");

        setSize(1040, 680);
        setMinimumSize(new Dimension(780, 520));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        // Inicializa componentes legados de HTML para preservar total compatibilidade
        areaChat = new JTextPane();
        areaChat.setEditable(false);
        areaChat.setContentType("text/html");
        kitHtml = new HTMLEditorKit();
        areaChat.setEditorKit(kitHtml);
        docHtml = (HTMLDocument) areaChat.getDocument();

        // =========================================================================
        // 1. LATERAL ESQUERDA — CONVERSAS (~30% da largura da aplicacao)
        // =========================================================================
        painelSidebar = new JPanel(new BorderLayout(0, 0));
        painelSidebar.setPreferredSize(new Dimension(330, 0));
        painelSidebar.setMinimumSize(new Dimension(280, 0));

        painelTopoSidebar = new JPanel();
        painelTopoSidebar.setLayout(new BoxLayout(painelTopoSidebar, BoxLayout.Y_AXIS));

        // 1.1. Barra de Perfil Conectado + Seletor de Tema no topo da Sidebar
        painelPerfilUsuario = new JPanel(new BorderLayout(10, 0));
        painelPerfilUsuario.setBorder(new EmptyBorder(10, 14, 10, 14));

        avatarMeuPerfil = new AvatarComponente(38, true);
        avatarMeuPerfil.atualizar("Você", false, false);

        JPanel infoMeuPerfil = new JPanel(new GridLayout(2, 1, 0, 1));
        infoMeuPerfil.setOpaque(false);

        labelNomeMeuPerfil = new JLabel("Não conectado");
        labelNomeMeuPerfil.setFont(new Font("Segoe UI", Font.BOLD, 13));

        labelStatus = new JLabel("● Desconectado");
        labelStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        infoMeuPerfil.add(labelNomeMeuPerfil);
        infoMeuPerfil.add(labelStatus);

        JPanel esquerdaPerfil = new JPanel(new BorderLayout(10, 0));
        esquerdaPerfil.setOpaque(false);
        esquerdaPerfil.add(avatarMeuPerfil, BorderLayout.WEST);
        esquerdaPerfil.add(infoMeuPerfil, BorderLayout.CENTER);

        JPanel direitaPerfil = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        direitaPerfil.setOpaque(false);

        labelTema = new JLabel("🎨");
        labelTema.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        labelTema.setToolTipText("Alternar Tema Visual");

        comboTemas = new JComboBox<>(Tema.values());
        comboTemas.setSelectedItem(temaAtual);
        comboTemas.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        comboTemas.setFocusable(false);
        comboTemas.setToolTipText("Selecionar Tema");
        comboTemas.addActionListener(e -> {
            Tema selecionado = (Tema) comboTemas.getSelectedItem();
            if (selecionado != null && selecionado != temaAtual) {
                aplicarTema(selecionado);
            }
        });

        direitaPerfil.add(labelTema);
        direitaPerfil.add(comboTemas);

        painelPerfilUsuario.add(esquerdaPerfil, BorderLayout.CENTER);
        painelPerfilUsuario.add(direitaPerfil, BorderLayout.EAST);

        // 1.2. Linha de Titulo "Conversas" + Botoes de Nova Conversa e Novo Grupo
        JPanel painelHeaderConversas = new JPanel(new BorderLayout(8, 0));
        painelHeaderConversas.setOpaque(false);
        painelHeaderConversas.setBorder(new EmptyBorder(12, 16, 8, 14));

        labelTituloSidebar = new JLabel("Conversas");
        labelTituloSidebar.setFont(new Font("Segoe UI", Font.BOLD, 21));

        JPanel painelBotoesHeaderSidebar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        painelBotoesHeaderSidebar.setOpaque(false);

        botaoNovoGrupo = criarBotaoCompacto("+ Grupo", false, false);
        botaoNovoGrupo.setToolTipText("Criar Novo Grupo de Conversa");
        botaoNovoGrupo.setEnabled(false);
        botaoNovoGrupo.addActionListener(e -> dialogCriarGrupoModerno());

        botaoNovaConversa = criarBotaoIconeCircular("+", "Iniciar Nova Conversa ou Grupo", 32);
        botaoNovaConversa.setEnabled(false);
        botaoNovaConversa.addActionListener(e -> mostrarMenuNovaConversa());

        painelBotoesHeaderSidebar.add(botaoNovoGrupo);
        painelBotoesHeaderSidebar.add(botaoNovaConversa);

        painelHeaderConversas.add(labelTituloSidebar, BorderLayout.WEST);
        painelHeaderConversas.add(painelBotoesHeaderSidebar, BorderLayout.EAST);

        // 1.3. Campo de Pesquisa Arredondado
        painelBuscaContainer = new JPanel(new BorderLayout());
        painelBuscaContainer.setOpaque(false);
        painelBuscaContainer.setBorder(new EmptyBorder(2, 14, 8, 14));

        painelCaixaBusca = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(temaAtual.fundoJanela);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(temaAtual.corBorda);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        painelCaixaBusca.setOpaque(false);
        painelCaixaBusca.setBorder(new EmptyBorder(6, 12, 6, 12));

        JLabel iconeLupa = new JLabel("🔍");
        iconeLupa.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));

        campoPesquisa = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                    g2.setColor(temaAtual.textoSecundario);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString("Pesquisar ou começar uma nova conversa", 2, y);
                    g2.dispose();
                }
            }
        };
        campoPesquisa.setOpaque(false);
        campoPesquisa.setBorder(BorderFactory.createEmptyBorder());
        campoPesquisa.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoPesquisa.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrarPorPesquisa();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrarPorPesquisa();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrarPorPesquisa();
            }
        });

        painelCaixaBusca.add(iconeLupa, BorderLayout.WEST);
        painelCaixaBusca.add(campoPesquisa, BorderLayout.CENTER);
        painelBuscaContainer.add(painelCaixaBusca, BorderLayout.CENTER);

        // 1.4. Pilulas de Filtro (Tudo | Nao lidas | Grupos) estilo WhatsApp Desktop
        painelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        painelFiltros.setOpaque(false);
        painelFiltros.setBorder(new EmptyBorder(0, 12, 10, 12));

        botaoFiltroTudo = criarBotaoPilulaFiltro("Tudo", FiltroConversa.TUDO);
        botaoFiltroNaoLidas = criarBotaoPilulaFiltro("Não lidas", FiltroConversa.NAO_LIDAS);
        botaoFiltroGrupos = criarBotaoPilulaFiltro("Grupos", FiltroConversa.GRUPOS);

        painelFiltros.add(botaoFiltroTudo);
        painelFiltros.add(botaoFiltroNaoLidas);
        painelFiltros.add(botaoFiltroGrupos);

        painelTopoSidebar.add(painelPerfilUsuario);
        painelTopoSidebar.add(painelHeaderConversas);
        painelTopoSidebar.add(painelBuscaContainer);
        painelTopoSidebar.add(painelFiltros);

        painelSidebar.add(painelTopoSidebar, BorderLayout.NORTH);

        // 1.5. Lista de Conversas com estado vazio
        listaConversas = new JList<>(modeloConversas);
        listaConversas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaConversas.setFixedCellHeight(68);
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
        listaConversas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = listaConversas.locationToIndex(e.getPoint());
                Rectangle cellBounds = idx >= 0 ? listaConversas.getCellBounds(idx, idx) : null;
                int novoHover = (cellBounds != null && cellBounds.contains(e.getPoint())) ? idx : -1;
                if (novoHover != itemHoverIndex) {
                    itemHoverIndex = novoHover;
                    listaConversas.repaint();
                }
            }
        });
        listaConversas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (itemHoverIndex != -1) {
                    itemHoverIndex = -1;
                    listaConversas.repaint();
                }
            }
        });

        scrollSidebar = new JScrollPane(listaConversas);
        scrollSidebar.setBorder(BorderFactory.createEmptyBorder());
        scrollSidebar.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        estilizarScrollBar(scrollSidebar);

        painelListaVaziaSidebar = criarPainelListaVaziaSidebar();

        cardLayoutSidebarLista = new CardLayout();
        painelCentroSidebar = new JPanel(cardLayoutSidebarLista);
        painelCentroSidebar.setOpaque(false);
        painelCentroSidebar.add(scrollSidebar, "LISTA");
        painelCentroSidebar.add(painelListaVaziaSidebar, "VAZIA");

        painelSidebar.add(painelCentroSidebar, BorderLayout.CENTER);

        // =========================================================================
        // 2. AREA DIREITA — CONVERSA SELECIONADA (~70% da largura da aplicacao)
        // =========================================================================
        cardLayoutDireito = new CardLayout();
        painelDireitoCards = new JPanel(cardLayoutDireito);

        // 2.1. Tela Inicial quando nenhuma conversa esta selecionada
        painelBoasVindas = new PainelBoasVindas();

        // 2.2. Painel de Conversa Ativa (Cabecalho + Area de Mensagens + Campo de Envio)
        painelConversaAtiva = new JPanel(new BorderLayout());

        // Cabecalho da Conversa Selecionada
        painelTopo = new JPanel(new BorderLayout(12, 0));
        painelTopo.setBorder(new EmptyBorder(10, 18, 10, 18));

        JPanel painelInfoContato = new JPanel(new BorderLayout(12, 0));
        painelInfoContato.setOpaque(false);

        avatarConversaAtiva = new AvatarComponente(40, true);

        JPanel painelTextosContato = new JPanel(new GridLayout(2, 1, 0, 2));
        painelTextosContato.setOpaque(false);

        labelConversaAtiva = new JLabel("Selecione uma conversa");
        labelConversaAtiva.setFont(new Font("Segoe UI", Font.BOLD, 15));

        labelSubtituloConversa = new JLabel("Clique em um contato ou grupo ao lado");
        labelSubtituloConversa.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        painelTextosContato.add(labelConversaAtiva);
        painelTextosContato.add(labelSubtituloConversa);

        painelInfoContato.add(avatarConversaAtiva, BorderLayout.WEST);
        painelInfoContato.add(painelTextosContato, BorderLayout.CENTER);

        // Acoes do Grupo integradas no lado direito do cabecalho da conversa
        painelAcoesGrupo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        painelAcoesGrupo.setOpaque(false);
        painelAcoesGrupo.setVisible(false);

        botaoAddMembroGrupo = criarBotaoCompacto("+ Pessoa", true, false);
        botaoAddMembroGrupo.setToolTipText("Adicionar participantes a este grupo");
        botaoAddMembroGrupo.addActionListener(e -> dialogAdicionarPessoasAoGrupo());

        botaoSairGrupo = criarBotaoCompacto("Sair", false, false);
        botaoSairGrupo.setToolTipText("Sair deste grupo");
        botaoSairGrupo.addActionListener(e -> acaoSairDoGrupo());

        botaoApagarGrupo = criarBotaoCompacto("Apagar", false, true);
        botaoApagarGrupo.setToolTipText("Apagar grupo para todos os membros");
        botaoApagarGrupo.addActionListener(e -> acaoApagarGrupo());

        painelAcoesGrupo.add(botaoAddMembroGrupo);
        painelAcoesGrupo.add(botaoSairGrupo);
        painelAcoesGrupo.add(botaoApagarGrupo);

        botaoConfigurarKeyIa = criarBotaoCompacto("🔑 Configurar Key IA", true, false);
        botaoConfigurarKeyIa.setToolTipText("Configurar sua API Key do Google Gemini");
        botaoConfigurarKeyIa.setVisible(false);
        botaoConfigurarKeyIa.addActionListener(e -> dialogConfigurarChaveIa());

        JPanel painelDireitaHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        painelDireitaHeader.setOpaque(false);
        painelDireitaHeader.add(botaoConfigurarKeyIa);
        painelDireitaHeader.add(painelAcoesGrupo);

        painelTopo.add(painelInfoContato, BorderLayout.CENTER);
        painelTopo.add(painelDireitaHeader, BorderLayout.EAST);

        // Area Central de Mensagens com Baloes Estilo WhatsApp
        painelMensagens = new PainelListaMensagens();

        scrollChat = new JScrollPane(painelMensagens);
        scrollChat.setBorder(BorderFactory.createEmptyBorder());
        scrollChat.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollChat.getVerticalScrollBar().setUnitIncrement(20);
        estilizarScrollBar(scrollChat);

        scrollChat.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                painelMensagens.revalidate();
                painelMensagens.repaint();
            }
        });

        // Campo de Envio Fixo na Parte Inferior
        painelInferior = new JPanel(new BorderLayout(10, 0));
        painelInferior.setBorder(new EmptyBorder(10, 16, 12, 16));

        botaoEmoji = new JButton(EmojiUtil.obterIcone("😊", 22));
        botaoEmoji.setToolTipText("Inserir Emoji Colorido");
        botaoEmoji.setFocusPainted(false);
        botaoEmoji.setBorderPainted(false);
        botaoEmoji.setContentAreaFilled(false);
        botaoEmoji.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botaoEmoji.setBorder(new EmptyBorder(6, 6, 6, 6));
        botaoEmoji.setEnabled(false);
        botaoEmoji.addActionListener(e -> mostrarSeletorEmojis());

        EmojiUtil.preCarregarAssincrono(() -> {
            botaoEmoji.setIcon(EmojiUtil.obterIcone("😊", 22));
            recarregarHtmlDaConversaAtiva();
        });

        campoTexto = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                    g2.setColor(temaAtual.textoSecundario);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    FontMetrics fm = g2.getFontMetrics();
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString("Digite uma mensagem", 2, y);
                    g2.dispose();
                }
            }
        };
        campoTexto.setOpaque(false);
        campoTexto.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        campoTexto.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        campoTexto.setEnabled(false);

        painelCampoArredondado = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(temaAtual.fundoEntrada);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(temaAtual.corBorda);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        painelCampoArredondado.setOpaque(false);
        painelCampoArredondado.setBorder(new EmptyBorder(2, 6, 2, 6));
        painelCampoArredondado.add(campoTexto, BorderLayout.CENTER);

        botaoEnviar = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color corFundo = !isEnabled()
                        ? temaAtual.corBorda
                        : getModel().isRollover() ? temaAtual.botaoHover : temaAtual.botaoPrimario;
                g2.setColor(corFundo);
                g2.fillOval(0, 0, getWidth(), getHeight());

                // Icone de aviao/seta de envio estilo WhatsApp
                g2.setColor(Color.WHITE);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                Path2D seta = new Path2D.Double();
                seta.moveTo(cx - 6, cy - 6);
                seta.lineTo(cx + 8, cy);
                seta.lineTo(cx - 6, cy + 6);
                seta.lineTo(cx - 3, cy);
                seta.closePath();
                g2.fill(seta);
                g2.dispose();
            }
        };
        botaoEnviar.setPreferredSize(new Dimension(42, 42));
        botaoEnviar.setFocusPainted(false);
        botaoEnviar.setBorderPainted(false);
        botaoEnviar.setContentAreaFilled(false);
        botaoEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botaoEnviar.setToolTipText("Enviar mensagem (Enter)");
        botaoEnviar.setEnabled(false);

        painelInferior.add(botaoEmoji, BorderLayout.WEST);
        painelInferior.add(painelCampoArredondado, BorderLayout.CENTER);
        painelInferior.add(botaoEnviar, BorderLayout.EAST);

        painelConversaAtiva.add(painelTopo, BorderLayout.NORTH);
        painelConversaAtiva.add(scrollChat, BorderLayout.CENTER);
        painelConversaAtiva.add(painelInferior, BorderLayout.SOUTH);

        painelDireitoCards.add(painelBoasVindas, CARD_BOAS_VINDAS);
        painelDireitoCards.add(painelConversaAtiva, CARD_CONVERSA);
        cardLayoutDireito.show(painelDireitoCards, CARD_BOAS_VINDAS);

        // Divisao responsiva (~30% lateral esquerda, ~70% area direita)
        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelSidebar, painelDireitoCards);
        splitPrincipal.setDividerLocation(330);
        splitPrincipal.setResizeWeight(0.28);
        splitPrincipal.setDividerSize(1);
        splitPrincipal.setBorder(BorderFactory.createEmptyBorder());
        splitPrincipal.setContinuousLayout(true);

        add(splitPrincipal, BorderLayout.CENTER);

        // Acoes de envio
        botaoEnviar.addActionListener(e -> enviarMensagem());
        campoTexto.addActionListener(e -> enviarMensagem());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fecharAplicacao();
            }
        });

        // Garante que a conversa padrao com a IA (@gemini) ja apareca no topo desde o inicio
        usuariosConhecidos.add(BotIaCliente.NICKNAME_PADRAO.toLowerCase(Locale.ROOT));
        usuariosOnline.add(BotIaCliente.NICKNAME_PADRAO.toLowerCase(Locale.ROOT));

        atualizarModeloSidebar();
        aplicarTema(temaAtual);
    }

    private JPanel criarPainelListaVaziaSidebar() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setOpaque(false);

        JPanel interno = new JPanel();
        interno.setLayout(new BoxLayout(interno, BoxLayout.Y_AXIS));
        interno.setOpaque(false);
        interno.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel icone = new JLabel("💬", SwingConstants.CENTER);
        icone.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        icone.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titulo = new JLabel("Nenhuma conversa encontrada");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titulo.setForeground(temaAtual.textoBranco);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("<html><div style='text-align:center; width:200px;'>"
                + "Assim que outros usuários entrarem no chat ou você criar um grupo, eles aparecerão aqui."
                + "</div></html>");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(temaAtual.textoSecundario);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        interno.add(icone);
        interno.add(Box.createVerticalStrut(10));
        interno.add(titulo);
        interno.add(Box.createVerticalStrut(6));
        interno.add(sub);

        painel.add(interno);
        return painel;
    }

    private JButton criarBotaoIconeCircular(String simbolo, String tooltip, int tamanho) {
        JButton btn = new JButton(simbolo) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = !isEnabled()
                        ? temaAtual.corBorda
                        : getModel().isRollover() ? temaAtual.botaoHover : temaAtual.botaoPrimario;
                g2.setColor(bg);
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() - 1;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(tamanho, tamanho));
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton criarBotaoPilulaFiltro(String texto, FiltroConversa filtro) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean ativo = (filtroAtual == filtro);
                if (ativo) {
                    g2.setColor(temaAtual == Tema.ESCURO || temaAtual == Tema.DRACULA
                            ? temaAtual.botaoPrimario
                            : new Color(217, 253, 211));
                } else {
                    g2.setColor(temaAtual.fundoJanela);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                g2.setColor(ativo
                        ? (temaAtual == Tema.ESCURO || temaAtual == Tema.DRACULA ? Color.WHITE : new Color(0, 128, 105))
                        : temaAtual.textoSecundario);
                g2.setFont(new Font("Segoe UI", ativo ? Font.BOLD : Font.PLAIN, 12));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 14, 5, 14));
        btn.addActionListener(e -> {
            filtroAtual = filtro;
            painelFiltros.repaint();
            atualizarModeloSidebar();
        });
        return btn;
    }

    private void filtrarPorPesquisa() {
        termoPesquisa = campoPesquisa.getText().trim().toLowerCase(Locale.ROOT);
        atualizarModeloSidebar();
    }

    /**
     * Menu rapido ao clicar no botao '+' para iniciar uma nova conversa privada ou criar um grupo.
     */
    private void mostrarMenuNovaConversa() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(temaAtual.fundoSidebar);
        menu.setBorder(new LineBorder(temaAtual.corBorda, 1, true));

        JMenuItem itemConversaIa = new JMenuItem("✨  Abrir conversa com Assistente IA");
        itemConversaIa.setFont(new Font("Segoe UI", Font.BOLD, 13));
        itemConversaIa.setBackground(temaAtual.fundoSidebar);
        itemConversaIa.setForeground(COR_IA_DESTAQUE);
        itemConversaIa.setBorder(new EmptyBorder(8, 14, 8, 14));
        itemConversaIa.addActionListener(e -> selecionarConversa(CONVERSA_IA_PADRAO));

        JMenuItem itemKeyIa = new JMenuItem("🔑  Configurar API Key da IA (Groq)...");
        itemKeyIa.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemKeyIa.setBackground(temaAtual.fundoSidebar);
        itemKeyIa.setForeground(temaAtual.textoBranco);
        itemKeyIa.setBorder(new EmptyBorder(8, 14, 8, 14));
        itemKeyIa.addActionListener(e -> dialogConfigurarChaveIa());

        JMenuItem itemNovaPrivada = new JMenuItem("💬  Nova conversa com @contato...");
        itemNovaPrivada.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemNovaPrivada.setBackground(temaAtual.fundoSidebar);
        itemNovaPrivada.setForeground(temaAtual.textoBranco);
        itemNovaPrivada.setBorder(new EmptyBorder(8, 14, 8, 14));
        itemNovaPrivada.addActionListener(e -> dialogNovaConversaPrivada());

        JMenuItem itemNovoGrupo = new JMenuItem("👥  Criar novo #grupo...");
        itemNovoGrupo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemNovoGrupo.setBackground(temaAtual.fundoSidebar);
        itemNovoGrupo.setForeground(temaAtual.textoBranco);
        itemNovoGrupo.setBorder(new EmptyBorder(8, 14, 8, 14));
        itemNovoGrupo.addActionListener(e -> dialogCriarGrupoModerno());

        menu.add(itemConversaIa);
        menu.add(itemKeyIa);
        menu.addSeparator();
        menu.add(itemNovaPrivada);
        menu.add(itemNovoGrupo);
        menu.show(botaoNovaConversa, 0, botaoNovaConversa.getHeight() + 4);
    }

    /**
     * Modal para o usuario colar e salvar sua API Key da Groq (ou Gemini) em chat.local.properties.
     */
    private void dialogConfigurarChaveIa() {
        JDialog dialogo = new JDialog(this, "Configurar API Key — Assistente IA (Groq)", true);
        dialogo.setSize(430, 265);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout());
        dialogo.getContentPane().setBackground(temaAtual.fundoSidebar);

        JPanel conteudo = new JPanel(new BorderLayout(0, 12));
        conteudo.setOpaque(false);
        conteudo.setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel titulo = new JLabel("✨ Configurar API Key da IA (Groq)");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(temaAtual.textoBranco);

        String chaveAtual = BotIaCliente.lerChaveConfigurada();
        String statusKey = (chaveAtual != null && !chaveAtual.isBlank())
                ? "Status atual: Chave configurada e ativa ✅"
                : "Status atual: Sem chave (Modo Simulação/Demonstração)";

        JLabel sub = new JLabel("<html>Cole sua <b>API Key da Groq</b> (ex: <code>gsk_...</code>) abaixo.<br>"
                + "<span style='font-size:10px;'>" + statusKey + " • Salva localmente em chat.local.properties (fora do Git).</span></html>");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(temaAtual.textoSecundario);

        JPasswordField campoKey = new JPasswordField(chaveAtual != null ? chaveAtual : "");
        campoKey.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campoKey.setBackground(temaAtual.fundoEntrada);
        campoKey.setForeground(temaAtual.textoBranco);
        campoKey.setCaretColor(temaAtual.textoBranco);
        campoKey.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(temaAtual.corBorda, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JCheckBox mostrarKey = new JCheckBox("Mostrar caracteres da chave");
        mostrarKey.setOpaque(false);
        mostrarKey.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        mostrarKey.setForeground(temaAtual.textoSecundario);
        char echoPadrao = campoKey.getEchoChar();
        mostrarKey.addActionListener(e -> campoKey.setEchoChar(mostrarKey.isSelected() ? (char) 0 : echoPadrao));

        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.setOpaque(false);
        centro.add(sub, BorderLayout.NORTH);
        centro.add(campoKey, BorderLayout.CENTER);
        centro.add(mostrarKey, BorderLayout.SOUTH);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);

        JButton btnCancelar = criarBotaoCompacto("Cancelar", false, false);
        btnCancelar.setBackground(temaAtual.fundoJanela);
        btnCancelar.setForeground(temaAtual.textoBranco);
        btnCancelar.addActionListener(e -> dialogo.dispose());

        JButton btnSalvar = criarBotaoCompacto("Salvar API Key", true, false);
        btnSalvar.setBackground(temaAtual.botaoPrimario);
        btnSalvar.setForeground(Color.WHITE);

        Runnable acaoSalvar = () -> {
            String novaChave = new String(campoKey.getPassword()).trim();
            try {
                BotIaCliente.salvarChaveLocal(novaChave);
                atualizarCabecalhoConversaAtiva();
                listaConversas.repaint();
                dialogo.dispose();
                selecionarConversa(CONVERSA_IA_PADRAO);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dialogo,
                        "Erro ao salvar chave: " + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        };

        btnSalvar.addActionListener(e -> acaoSalvar.run());
        campoKey.addActionListener(e -> acaoSalvar.run());

        botoes.add(btnCancelar);
        botoes.add(btnSalvar);

        conteudo.add(titulo, BorderLayout.NORTH);
        conteudo.add(centro, BorderLayout.CENTER);
        conteudo.add(botoes, BorderLayout.SOUTH);

        dialogo.add(conteudo);
        SwingUtilities.invokeLater(campoKey::requestFocusInWindow);
        dialogo.setVisible(true);
    }

    private void dialogNovaConversaPrivada() {
        JDialog dialogo = new JDialog(this, "Nova Conversa", true);
        dialogo.setSize(360, 220);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout());
        dialogo.getContentPane().setBackground(temaAtual.fundoSidebar);

        JPanel conteudo = new JPanel(new BorderLayout(0, 12));
        conteudo.setOpaque(false);
        conteudo.setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel titulo = new JLabel("Iniciar conversa privada");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(temaAtual.textoBranco);

        JLabel sub = new JLabel("Digite o nickname do usuário (ex: ana):");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(temaAtual.textoSecundario);

        JTextField campoContato = criarCampoTextoModal("");

        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 4));
        centro.setOpaque(false);
        centro.add(sub);
        centro.add(campoContato);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);

        JButton btnCancelar = criarBotaoCompacto("Cancelar", false, false);
        btnCancelar.setBackground(temaAtual.fundoJanela);
        btnCancelar.setForeground(temaAtual.textoBranco);
        btnCancelar.addActionListener(e -> dialogo.dispose());

        JButton btnAbrir = criarBotaoCompacto("Conversar", true, false);
        btnAbrir.setBackground(temaAtual.botaoPrimario);
        btnAbrir.setForeground(Color.WHITE);

        Runnable confirmar = () -> {
            String nome = campoContato.getText().trim().toLowerCase(Locale.ROOT);
            if (nome.startsWith("@")) {
                nome = nome.substring(1).trim();
            }
            if (nome.isEmpty() || nome.contains(" ")) {
                campoContato.setBorder(new LineBorder(COR_PERIGO, 2, true));
                campoContato.requestFocusInWindow();
                return;
            }
            usuariosConhecidos.add(nome);
            filtroAtual = FiltroConversa.TUDO;
            atualizarModeloSidebar();
            selecionarConversa("@" + nome);
            dialogo.dispose();
        };

        btnAbrir.addActionListener(e -> confirmar.run());
        campoContato.addActionListener(e -> confirmar.run());

        botoes.add(btnCancelar);
        botoes.add(btnAbrir);

        conteudo.add(titulo, BorderLayout.NORTH);
        conteudo.add(centro, BorderLayout.CENTER);
        conteudo.add(botoes, BorderLayout.SOUTH);

        dialogo.add(conteudo);
        SwingUtilities.invokeLater(campoContato::requestFocusInWindow);
        dialogo.setVisible(true);
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

    private JButton criarBotaoCompacto(String texto, boolean primario, boolean perigo) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!btn.isEnabled()) return;
                if (perigo) {
                    btn.setBackground(COR_PERIGO_HOVER);
                } else if (primario) {
                    btn.setBackground(temaAtual.botaoHover);
                } else {
                    btn.setBackground(temaAtual.corBorda);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!btn.isEnabled()) return;
                if (perigo) {
                    btn.setBackground(COR_PERIGO);
                } else if (primario) {
                    btn.setBackground(temaAtual.botaoPrimario);
                } else {
                    btn.setBackground(temaAtual.fundoJanela);
                }
            }
        });
        return btn;
    }

    private void estilizarScrollBar(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(140, 145, 150, 120);
                this.trackColor = scroll == scrollSidebar ? temaAtual.fundoSidebar : temaAtual.fundoChat;
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
        popup.setBackground(temaAtual.fundoSidebar);
        popup.setBorder(new LineBorder(temaAtual.corBorda, 1, true));

        JPanel grade = new JPanel(new GridLayout(6, 6, 4, 4));
        grade.setBackground(temaAtual.fundoSidebar);
        grade.setBorder(new EmptyBorder(8, 8, 8, 8));

        for (String emoji : EmojiUtil.getMapaEmojis().keySet()) {
            JButton btn = new JButton(EmojiUtil.obterIcone(emoji, 22));
            btn.setToolTipText(emoji);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setBackground(temaAtual.fundoJanela);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setBorder(new EmptyBorder(6, 6, 6, 6));

            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(temaAtual.corItemSelecionado);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(temaAtual.fundoJanela);
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
        popup.show(botaoEmoji, 0, -260);
    }

    /**
     * Modal customizado e estilizado para Criar um Grupo e selecionar as pessoas.
     */
    private void dialogCriarGrupoModerno() {
        JDialog dialogo = new JDialog(this, "Criar Novo Grupo", true);
        dialogo.setSize(390, 420);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout());
        dialogo.getContentPane().setBackground(temaAtual.fundoSidebar);

        JPanel conteudo = new JPanel(new BorderLayout(0, 12));
        conteudo.setOpaque(false);
        conteudo.setBorder(new EmptyBorder(18, 22, 18, 22));

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

        JPanel painelLista = new JPanel();
        painelLista.setLayout(new BoxLayout(painelLista, BoxLayout.Y_AXIS));
        painelLista.setBackground(temaAtual.fundoJanela);
        painelLista.setBorder(new EmptyBorder(8, 10, 8, 10));

        List<JCheckBox> checkboxes = new ArrayList<>();
        for (String user : usuariosConhecidos) {
            if (nickname == null || !user.equalsIgnoreCase(nickname)) {
                JCheckBox cb = criarCheckBoxModal(user, true);
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
        scrollPessoas.setBorder(new LineBorder(temaAtual.corBorda, 1, true));
        estilizarScrollBar(scrollPessoas);

        wrapperCentro.add(labelParticipantes, BorderLayout.NORTH);
        wrapperCentro.add(scrollPessoas, BorderLayout.CENTER);
        conteudo.add(wrapperCentro, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        painelBotoes.setOpaque(false);

        JButton btnCancelar = criarBotaoEstilizado("Cancelar", false);
        btnCancelar.setBackground(temaAtual.fundoJanela);
        btnCancelar.setForeground(temaAtual.textoBranco);
        btnCancelar.addActionListener(e -> dialogo.dispose());

        JButton btnCriar = criarBotaoEstilizado("Criar Grupo", true);
        btnCriar.setBackground(temaAtual.botaoPrimario);
        btnCriar.setForeground(Color.WHITE);

        Runnable acaoConfirmar = () -> {
            String nome = campoNomeGrupo.getText().trim();
            if (nome.isEmpty()) {
                campoNomeGrupo.setBorder(new LineBorder(COR_PERIGO, 2, true));
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

    /**
     * Modal para adicionar novas pessoas a um grupo que ja existe.
     */
    private void dialogAdicionarPessoasAoGrupo() {
        if (conversaAtiva == null || !conversaAtiva.startsWith("#")) {
            return;
        }
        final String grupoAlvo = conversaAtiva;

        JDialog dialogo = new JDialog(this, "Adicionar ao Grupo " + grupoAlvo, true);
        dialogo.setSize(370, 340);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout());
        dialogo.getContentPane().setBackground(temaAtual.fundoSidebar);

        JPanel conteudo = new JPanel(new BorderLayout(0, 12));
        conteudo.setOpaque(false);
        conteudo.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel titulo = new JLabel("Adicionar pessoas a " + grupoAlvo);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titulo.setForeground(temaAtual.textoBranco);
        conteudo.add(titulo, BorderLayout.NORTH);

        JPanel painelLista = new JPanel();
        painelLista.setLayout(new BoxLayout(painelLista, BoxLayout.Y_AXIS));
        painelLista.setBackground(temaAtual.fundoJanela);
        painelLista.setBorder(new EmptyBorder(8, 10, 8, 10));

        List<JCheckBox> checkboxes = new ArrayList<>();
        for (String user : usuariosConhecidos) {
            if (nickname == null || !user.equalsIgnoreCase(nickname)) {
                JCheckBox cb = criarCheckBoxModal(user, false);
                checkboxes.add(cb);
                painelLista.add(cb);
            }
        }

        if (checkboxes.isEmpty()) {
            JLabel avisoVazio = new JLabel("<html><div style='padding:10px;'>Nenhum outro contato disponível no momento.</div></html>");
            avisoVazio.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            avisoVazio.setForeground(temaAtual.textoSecundario);
            painelLista.add(avisoVazio);
        }

        JScrollPane scrollPessoas = new JScrollPane(painelLista);
        scrollPessoas.setBorder(new LineBorder(temaAtual.corBorda, 1, true));
        estilizarScrollBar(scrollPessoas);
        conteudo.add(scrollPessoas, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        painelBotoes.setOpaque(false);

        JButton btnCancelar = criarBotaoEstilizado("Cancelar", false);
        btnCancelar.setBackground(temaAtual.fundoJanela);
        btnCancelar.setForeground(temaAtual.textoBranco);
        btnCancelar.addActionListener(e -> dialogo.dispose());

        JButton btnAdicionar = criarBotaoEstilizado("Adicionar", true);
        btnAdicionar.setBackground(temaAtual.botaoPrimario);
        btnAdicionar.setForeground(Color.WHITE);
        btnAdicionar.addActionListener(e -> {
            List<String> escolhidos = new ArrayList<>();
            for (JCheckBox cb : checkboxes) {
                if (cb.isSelected()) {
                    escolhidos.add((String) cb.getClientProperty("username"));
                }
            }
            if (escolhidos.isEmpty()) {
                dialogo.dispose();
                return;
            }
            final String cmd = Protocolo.COMANDO_ADD_GRUPO + grupoAlvo + " " + String.join(",", escolhidos);
            new Thread(() -> conexao.enviar(cmd), "thread-add-grupo").start();
            dialogo.dispose();
        });

        painelBotoes.add(btnCancelar);
        painelBotoes.add(btnAdicionar);
        conteudo.add(painelBotoes, BorderLayout.SOUTH);

        dialogo.add(conteudo);
        dialogo.setVisible(true);
    }

    /**
     * Sai do grupo atualmente selecionado.
     */
    private void acaoSairDoGrupo() {
        if (conversaAtiva == null || !conversaAtiva.startsWith("#")) {
            return;
        }
        final String grupoAlvo = conversaAtiva;
        removerGrupoLocalmente(grupoAlvo);
        new Thread(() -> conexao.enviar(Protocolo.COMANDO_SAIR_GRUPO + grupoAlvo), "thread-sair-grupo").start();
    }

    /**
     * Pede confirmacao estilizada e apaga o grupo para todos os participantes.
     */
    private void acaoApagarGrupo() {
        if (conversaAtiva == null || !conversaAtiva.startsWith("#")) {
            return;
        }
        final String grupoAlvo = conversaAtiva;

        JDialog dialogo = new JDialog(this, "Apagar Grupo", true);
        dialogo.setSize(370, 200);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout());
        dialogo.getContentPane().setBackground(temaAtual.fundoSidebar);

        JPanel conteudo = new JPanel(new BorderLayout(0, 14));
        conteudo.setOpaque(false);
        conteudo.setBorder(new EmptyBorder(20, 22, 18, 22));

        JLabel texto = new JLabel("<html><b>Deseja apagar o grupo " + escaparHtml(grupoAlvo) + "?</b><br><br>"
                + "O grupo e todo o histórico de mensagens serão excluídos para todos os participantes.</html>");
        texto.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        texto.setForeground(temaAtual.textoBranco);
        conteudo.add(texto, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        painelBotoes.setOpaque(false);

        JButton btnCancelar = criarBotaoCompacto("Cancelar", false, false);
        btnCancelar.setBackground(temaAtual.fundoJanela);
        btnCancelar.setForeground(temaAtual.textoBranco);
        btnCancelar.addActionListener(e -> dialogo.dispose());

        JButton btnConfirmar = criarBotaoCompacto("Sim, Apagar Grupo", false, true);
        btnConfirmar.setBackground(COR_PERIGO);
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.addActionListener(e -> {
            dialogo.dispose();
            removerGrupoLocalmente(grupoAlvo);
            new Thread(() -> conexao.enviar(Protocolo.COMANDO_APAGAR_GRUPO + grupoAlvo), "thread-apagar-grupo").start();
        });

        painelBotoes.add(btnCancelar);
        painelBotoes.add(btnConfirmar);
        conteudo.add(painelBotoes, BorderLayout.SOUTH);

        dialogo.add(conteudo);
        dialogo.setVisible(true);
    }

    private void removerGrupoLocalmente(String grupo) {
        String chave = grupo.trim().toLowerCase(Locale.ROOT);
        meusGrupos.remove(chave);
        mensagensPorConversa.remove(chave);
        itensMensagemPorConversa.remove(chave);
        ultimaPreviaPorConversa.remove(chave);
        ultimoHorarioPorConversa.remove(chave);
        naoLidasPorConversa.remove(chave);
        conversasComHistoricoCarregado.remove(chave);

        if (chave.equalsIgnoreCase(conversaAtiva)) {
            conversaAtiva = null;
            painelAcoesGrupo.setVisible(false);
            cardLayoutDireito.show(painelDireitoCards, CARD_BOAS_VINDAS);
            campoTexto.setEnabled(false);
            botaoEnviar.setEnabled(false);
            botaoEmoji.setEnabled(false);
        }
        atualizarModeloSidebar();
        recarregarHtmlDaConversaAtiva();
    }

    private JCheckBox criarCheckBoxModal(String user, boolean marcadoPorPadrao) {
        boolean online = usuariosOnline.contains(user.toLowerCase(Locale.ROOT));
        String sufixo = online ? "  (online)" : "";
        JCheckBox cb = new JCheckBox("  @" + user + sufixo);
        cb.putClientProperty("username", user);
        cb.setSelected(marcadoPorPadrao);
        cb.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cb.setBackground(temaAtual.fundoJanela);
        cb.setForeground(temaAtual.textoBranco);
        cb.setFocusPainted(false);
        cb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cb.setBorder(new EmptyBorder(6, 4, 6, 4));
        return cb;
    }

    private JTextField criarCampoTextoModal(String valorInicial) {
        JTextField tf = new JTextField(valorInicial);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setBackground(temaAtual.fundoEntrada);
        tf.setForeground(temaAtual.textoBranco);
        tf.setCaretColor(temaAtual.textoBranco);
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(temaAtual.corBorda, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return tf;
    }

    private void selecionarConversa(String alvo) {
        conversaAtiva = alvo;
        naoLidasPorConversa.remove(alvo);
        listaConversas.setSelectedValue(alvo, true);
        listaConversas.repaint();

        cardLayoutDireito.show(painelDireitoCards, CARD_CONVERSA);

        atualizarCabecalhoConversaAtiva();

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

    private void atualizarCabecalhoConversaAtiva() {
        if (conversaAtiva == null) {
            cardLayoutDireito.show(painelDireitoCards, CARD_BOAS_VINDAS);
            return;
        }
        boolean ehGrupo = conversaAtiva.startsWith("#");
        boolean ehIa = conversaAtiva.equalsIgnoreCase(CONVERSA_IA_PADRAO);
        painelAcoesGrupo.setVisible(ehGrupo);
        botaoConfigurarKeyIa.setVisible(ehIa);

        if (ehGrupo) {
            String nomeExibicao = formatarNomeExibicao(conversaAtiva);
            labelConversaAtiva.setText(nomeExibicao + "  (" + conversaAtiva + ")");
            labelSubtituloConversa.setText("Grupo de conversa • mensagens em tempo real");
            labelSubtituloConversa.setForeground(temaAtual.textoSecundario);
            avatarConversaAtiva.atualizar(conversaAtiva, true, false);
        } else if (ehIa) {
            labelConversaAtiva.setText("Assistente IA ✨");
            boolean temKey = BotIaCliente.possuiChaveConfigurada();
            botaoConfigurarKeyIa.setText(temKey ? "🔑 Key Groq Ativa" : "🔑 Configurar Key IA");
            if (temKey) {
                labelSubtituloConversa.setText("● online • Groq IA ativa (Llama 3.3)");
                labelSubtituloConversa.setForeground(temaAtual == Tema.CLARO ? new Color(0, 128, 105) : COR_STATUS_ONLINE);
            } else {
                labelSubtituloConversa.setText("● online • Modo demonstração (clique em '🔑 Configurar Key IA')");
                labelSubtituloConversa.setForeground(temaAtual.textoSecundario);
            }
            avatarConversaAtiva.atualizar(conversaAtiva, false, true);
        } else if (conversaAtiva.startsWith("@")) {
            String userLimpo = conversaAtiva.substring(1).toLowerCase(Locale.ROOT);
            boolean online = usuariosOnline.contains(userLimpo);
            labelConversaAtiva.setText(formatarNomeExibicao(conversaAtiva));
            if (online) {
                labelSubtituloConversa.setText("● online");
                labelSubtituloConversa.setForeground(temaAtual == Tema.CLARO ? new Color(0, 128, 105) : COR_STATUS_ONLINE);
            } else {
                labelSubtituloConversa.setText("offline");
                labelSubtituloConversa.setForeground(temaAtual.textoSecundario);
            }
            avatarConversaAtiva.atualizar(conversaAtiva, false, online);
        }
    }

    private void recarregarHtmlDaConversaAtiva() {
        areaChat.setText("");
        docHtml = (HTMLDocument) areaChat.getDocument();
        aplicarRegrasCss();

        if (conversaAtiva == null) {
            cardLayoutDireito.show(painelDireitoCards, CARD_BOAS_VINDAS);
            painelBoasVindas.repaint();
            return;
        }

        cardLayoutDireito.show(painelDireitoCards, CARD_CONVERSA);

        List<String> blocos = mensagensPorConversa.getOrDefault(conversaAtiva, List.of());
        for (String html : blocos) {
            try {
                kitHtml.insertHTML(docHtml, docHtml.getLength(), html, 0, 0, null);
            } catch (Exception ignored) {
            }
        }

        painelMensagens.reconstruirConversa(
                conversaAtiva,
                itensMensagemPorConversa.getOrDefault(conversaAtiva, List.of())
        );
        rolarParaFinalDoChat();
    }

    private void rolarParaFinalDoChat() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollChat.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void adicionarMensagemNaConversa(String chaveConversa, String html) {
        adicionarMensagemEstruturadaNaConversa(chaveConversa, html, null);
    }

    private void adicionarMensagemEstruturadaNaConversa(String chaveConversa, String html, ItemMensagem item) {
        if (chaveConversa == null) {
            return;
        }
        List<String> lista = mensagensPorConversa.computeIfAbsent(chaveConversa, k -> new ArrayList<>());
        if (!lista.isEmpty() && lista.get(lista.size() - 1).equals(html)) {
            return;
        }
        lista.add(html);

        ItemMensagem itemFinal = item != null ? item : converterHtmlLegadoEmItem(html);
        List<ItemMensagem> listaItens = itensMensagemPorConversa.computeIfAbsent(chaveConversa, k -> new ArrayList<>());
        listaItens.add(itemFinal);

        // Atualiza previa e horario na barra lateral
        if (itemFinal.tipo() == TipoItemChat.MENSAGEM) {
            String prefixo = itemFinal.enviadaPorMim()
                    ? "✓✓ Você: "
                    : (chaveConversa.startsWith("#") ? itemFinal.remetente() + ": " : "");
            ultimaPreviaPorConversa.put(chaveConversa, prefixo + itemFinal.conteudoTexto());
            if (itemFinal.horario() != null && !itemFinal.horario().isBlank()) {
                ultimoHorarioPorConversa.put(chaveConversa, extrairApenasHora(itemFinal.horario()));
            }
        } else if (itemFinal.tipo() == TipoItemChat.SISTEMA) {
            ultimaPreviaPorConversa.put(chaveConversa, itemFinal.conteudoTexto());
        }

        if (chaveConversa.equalsIgnoreCase(conversaAtiva)) {
            try {
                kitHtml.insertHTML(docHtml, docHtml.getLength(), html, 0, 0, null);
                areaChat.setCaretPosition(docHtml.getLength());
            } catch (Exception ignored) {
            }
            painelMensagens.adicionarItemVisual(chaveConversa, itemFinal);
            rolarParaFinalDoChat();
        } else {
            naoLidasPorConversa.merge(chaveConversa, 1, Integer::sum);
        }
        listaConversas.repaint();
    }

    private String extrairApenasHora(String horarioBruto) {
        if (horarioBruto == null) return "";
        String limpo = horarioBruto.trim();
        int espaco = limpo.lastIndexOf(' ');
        if (espaco != -1 && espaco + 1 < limpo.length()) {
            return limpo.substring(espaco + 1);
        }
        return limpo;
    }

    private ItemMensagem converterHtmlLegadoEmItem(String html) {
        String semTags = html.replaceAll("<[^>]+>", "").trim();
        if (html.contains("class='error'")) {
            return new ItemMensagem(TipoItemChat.ERRO, "", semTags, escaparHtml(semTags), "", false);
        }
        return new ItemMensagem(TipoItemChat.SISTEMA, "", semTags, escaparHtml(semTags), "", false);
    }

    private void atualizarModeloSidebar() {
        String selecionadaAntes = conversaAtiva;
        modeloConversas.clear();

        List<String> candidatas = new ArrayList<>();
        // Fixa a conversa padrao com a IA (@gemini) sempre no topo da lista
        if (nickname == null || !nickname.equalsIgnoreCase(BotIaCliente.NICKNAME_PADRAO)) {
            candidatas.add(CONVERSA_IA_PADRAO);
        }
        for (String grupo : meusGrupos) {
            candidatas.add(grupo);
        }
        for (String user : usuariosConhecidos) {
            if (user.equalsIgnoreCase(BotIaCliente.NICKNAME_PADRAO)) {
                continue;
            }
            if (nickname == null || !user.equalsIgnoreCase(nickname)) {
                candidatas.add("@" + user);
            }
        }

        for (String item : candidatas) {
            if (filtroAtual == FiltroConversa.NAO_LIDAS && naoLidasPorConversa.getOrDefault(item, 0) <= 0) {
                continue;
            }
            if (filtroAtual == FiltroConversa.GRUPOS && !item.startsWith("#")) {
                continue;
            }
            if (!termoPesquisa.isEmpty()) {
                String nomeItem = item.toLowerCase(Locale.ROOT);
                String previaItem = ultimaPreviaPorConversa.getOrDefault(item, "").toLowerCase(Locale.ROOT);
                if (!nomeItem.contains(termoPesquisa) && !previaItem.contains(termoPesquisa)) {
                    continue;
                }
            }
            modeloConversas.addElement(item);
        }

        if (modeloConversas.isEmpty()) {
            cardLayoutSidebarLista.show(painelCentroSidebar, "VAZIA");
        } else {
            cardLayoutSidebarLista.show(painelCentroSidebar, "LISTA");
        }

        if (selecionadaAntes != null && modeloConversas.contains(selecionadaAntes)) {
            listaConversas.setSelectedValue(selecionadaAntes, false);
        }
        listaConversas.repaint();
    }

    private void aplicarTema(Tema novoTema) {
        this.temaAtual = novoTema;

        getContentPane().setBackground(temaAtual.fundoJanela);

        // Sidebar
        painelSidebar.setBackground(temaAtual.fundoSidebar);
        painelSidebar.setBorder(new MatteBorder(0, 0, 0, 1, temaAtual.corBorda));
        painelTopoSidebar.setBackground(temaAtual.fundoSidebar);
        painelTopoSidebar.setBorder(new MatteBorder(0, 0, 1, 0, temaAtual.corBorda));

        painelPerfilUsuario.setBackground(temaAtual.fundoJanela);
        painelPerfilUsuario.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, temaAtual.corBorda),
                new EmptyBorder(10, 14, 10, 14)
        ));

        labelNomeMeuPerfil.setForeground(temaAtual.textoBranco);
        labelTituloSidebar.setForeground(temaAtual.textoBranco);
        labelTema.setForeground(temaAtual.textoSecundario);

        campoPesquisa.setForeground(temaAtual.textoBranco);
        campoPesquisa.setCaretColor(temaAtual.textoBranco);

        botaoNovoGrupo.setBackground(temaAtual.fundoJanela);
        botaoNovoGrupo.setForeground(temaAtual.textoBranco);

        listaConversas.setBackground(temaAtual.fundoSidebar);
        listaConversas.setForeground(temaAtual.textoBranco);
        scrollSidebar.getViewport().setBackground(temaAtual.fundoSidebar);

        // Area direita e Cabecalho da conversa
        painelTopo.setBackground(temaAtual.fundoJanela);
        painelTopo.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, temaAtual.corBorda),
                new EmptyBorder(10, 18, 10, 18)
        ));
        labelConversaAtiva.setForeground(temaAtual.textoBranco);
        atualizarCabecalhoConversaAtiva();

        botaoAddMembroGrupo.setBackground(temaAtual.botaoPrimario);
        botaoAddMembroGrupo.setForeground(Color.WHITE);

        botaoConfigurarKeyIa.setBackground(COR_IA_DESTAQUE);
        botaoConfigurarKeyIa.setForeground(Color.WHITE);

        botaoSairGrupo.setBackground(temaAtual.fundoSidebar);
        botaoSairGrupo.setForeground(temaAtual.textoBranco);

        botaoApagarGrupo.setBackground(COR_PERIGO);
        botaoApagarGrupo.setForeground(Color.WHITE);

        scrollChat.setBackground(temaAtual.fundoChat);
        scrollChat.getViewport().setBackground(temaAtual.fundoChat);
        painelMensagens.setBackground(temaAtual.fundoChat);
        areaChat.setBackground(temaAtual.fundoChat);

        // Painel inferior de digitacao
        painelInferior.setBackground(temaAtual.fundoJanela);
        painelInferior.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, temaAtual.corBorda),
                new EmptyBorder(10, 16, 12, 16)
        ));

        campoTexto.setForeground(temaAtual.textoBranco);
        campoTexto.setCaretColor(temaAtual.textoBranco);

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
        modalLogin.setSize(390, 340);
        modalLogin.setLocationRelativeTo(this);
        modalLogin.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        modalLogin.setLayout(new BorderLayout());
        modalLogin.getContentPane().setBackground(temaAtual.fundoSidebar);

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
        form.setBorder(new EmptyBorder(20, 26, 14, 26));

        JLabel titulo = new JLabel("Bem-vindo ao Chat");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 19));
        titulo.setForeground(temaAtual.textoBranco);

        JLabel lblNick = new JLabel("Seu nome de usuário (Nickname):");
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

        JButton btnEntrar = criarBotaoEstilizado("Conectar ao Chat  ➤", true);
        btnEntrar.setBackground(temaAtual.botaoPrimario);
        btnEntrar.setForeground(Color.WHITE);

        Runnable acaoConectar = () -> {
            String nickDigitado = campoNick.getText().trim();
            if (nickDigitado.isEmpty() || nickDigitado.contains(" ")) {
                campoNick.setBorder(new LineBorder(COR_PERIGO, 2, true));
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
        rodape.setBorder(new EmptyBorder(6, 26, 22, 26));
        rodape.add(btnEntrar, BorderLayout.CENTER);

        modalLogin.add(form, BorderLayout.CENTER);
        modalLogin.add(rodape, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(campoNick::requestFocusInWindow);
        modalLogin.setVisible(true);
    }

    private void conectarAoServidor(String host, int porta, String nick) {
        labelNomeMeuPerfil.setText("@" + nick);
        labelStatus.setText("● Conectando a " + host + ":" + porta + "...");
        labelStatus.setForeground(temaAtual.textoSecundario);
        avatarMeuPerfil.atualizar("@" + nick, false, false);

        conexao = new ConexaoRedeCliente(
                host,
                porta,
                this::receberLinhaDoServidor,
                this::atualizarStatus
        );

        new Thread(() -> {
            try {
                conexao.conectar(nick);
                if (!nick.equalsIgnoreCase(BotIaCliente.NICKNAME_PADRAO)) {
                    BotIaCliente.garantirBotAtivoEmBackground(host, porta);
                }
                SwingUtilities.invokeLater(() -> {
                    setTitle("Chat Multiusuário — @" + nick);
                    labelNomeMeuPerfil.setText(formatarNomeExibicao("@" + nick) + " (@" + nick + ")");
                    labelStatus.setText("● Online");
                    labelStatus.setForeground(temaAtual == Tema.CLARO ? new Color(0, 128, 105) : COR_STATUS_ONLINE);
                    avatarMeuPerfil.atualizar("@" + nick, false, true);

                    botaoNovoGrupo.setEnabled(true);
                    botaoNovaConversa.setEnabled(true);

                    // Abre a conversa padrao com a IA automaticamente se nenhuma estiver selecionada
                    if (conversaAtiva == null && !nick.equalsIgnoreCase(BotIaCliente.NICKNAME_PADRAO)) {
                        selecionarConversa(CONVERSA_IA_PADRAO);
                    } else if (conversaAtiva != null) {
                        campoTexto.setEnabled(true);
                        botaoEnviar.setEnabled(true);
                        botaoEmoji.setEnabled(true);
                        campoTexto.requestFocusInWindow();
                    }
                    painelBoasVindas.repaint();
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    labelStatus.setText("● Desconectado");
                    labelStatus.setForeground(COR_PERIGO);
                    avatarMeuPerfil.atualizar("@" + nick, false, false);
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
            // 1. Grupo removido ou usuario saiu: ~REMGRUPO|#devs
            if (linha.startsWith(ChatService.PREFIXO_REMOVER_GRUPO)) {
                String grupoRem = linha.substring(ChatService.PREFIXO_REMOVER_GRUPO.length()).trim();
                removerGrupoLocalmente(grupoRem);
                return;
            }

            // 2. Atualizacao de contatos e grupos: ~CONTATOS|u1,u2|#devs
            if (linha.startsWith(ChatService.PREFIXO_CONTATOS)) {
                processarAtualizacaoContatos(linha.substring(ChatService.PREFIXO_CONTATOS.length()));
                return;
            }

            // 3. Mensagem de grupo especifico: ~GRUPO|#devs|[dd/MM HH:mm] nome: texto
            if (linha.startsWith(ChatService.PREFIXO_GRUPO)) {
                String resto = linha.substring(ChatService.PREFIXO_GRUPO.length());
                int pipe = resto.indexOf('|');
                if (pipe != -1) {
                    String grupo = resto.substring(0, pipe).trim().toLowerCase(Locale.ROOT);
                    String msgFormatada = resto.substring(pipe + 1);
                    if (meusGrupos.add(grupo)) {
                        atualizarModeloSidebar();
                    }
                    adicionarMensagemEstruturadaNaConversa(
                            grupo,
                            formatarLinhaEmHtml(msgFormatada),
                            parsearLinhaMensagem(msgFormatada)
                    );
                    return;
                }
            }

            // 4. Mensagem privada: ~PRIV|remetente|destinatario|[dd/MM HH:mm] nome: texto
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
                    adicionarMensagemEstruturadaNaConversa(
                            chavePrivada,
                            formatarLinhaEmHtml(msgFormatada),
                            parsearLinhaMensagem(msgFormatada)
                    );
                    return;
                }
            }

            // 5. Erros do protocolo (! ...)
            if (linha.startsWith(Protocolo.PREFIXO_ERRO)) {
                String msgErro = linha.substring(Protocolo.PREFIXO_ERRO.length());
                if (conversaAtiva != null) {
                    ItemMensagem itemErro = new ItemMensagem(
                            TipoItemChat.ERRO, "", "! " + msgErro, escaparHtml("! " + msgErro), "", false
                    );
                    adicionarMensagemEstruturadaNaConversa(
                            conversaAtiva,
                            "<div class='error'>! " + escaparHtml(msgErro) + "</div>",
                            itemErro
                    );
                } else {
                    JOptionPane.showMessageDialog(this, msgErro, "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
    }

    private void processarAtualizacaoContatos(String payload) {
        String[] blocos = payload.split("\\|", -1);
        usuariosOnline.clear();
        usuariosOnline.add(BotIaCliente.NICKNAME_PADRAO.toLowerCase(Locale.ROOT));
        usuariosConhecidos.add(BotIaCliente.NICKNAME_PADRAO.toLowerCase(Locale.ROOT));
        if (blocos.length >= 1 && !blocos[0].isBlank()) {
            for (String u : blocos[0].split(",")) {
                String nome = u.trim().toLowerCase(Locale.ROOT);
                if (!nome.isEmpty()) {
                    usuariosOnline.add(nome);
                    if (nickname == null || !nome.equalsIgnoreCase(nickname)) {
                        usuariosConhecidos.add(nome);
                    }
                }
            }
        }
        Set<String> gruposDoServidor = new LinkedHashSet<>();
        if (blocos.length >= 2 && !blocos[1].isBlank()) {
            for (String g : blocos[1].split(",")) {
                String grupo = g.trim().toLowerCase(Locale.ROOT);
                if (!grupo.isEmpty()) {
                    gruposDoServidor.add(grupo);
                }
            }
        }
        meusGrupos.clear();
        meusGrupos.addAll(gruposDoServidor);
        atualizarModeloSidebar();
        atualizarCabecalhoConversaAtiva();
    }

    private ItemMensagem parsearLinhaMensagem(String linha) {
        if (linha.startsWith("* ")) {
            String txt = linha.substring(2).trim();
            String html = EmojiUtil.substituirEmojisPorImgHtml(escaparHtml(linha));
            return new ItemMensagem(TipoItemChat.SISTEMA, "", txt, html, "", false);
        }
        if (linha.startsWith("[") && linha.contains("] ")) {
            int fecharColchete = linha.indexOf("] ");
            String horario = linha.substring(1, fecharColchete).trim();
            String resto = linha.substring(fecharColchete + 2);

            int doisPontos = resto.indexOf(": ");
            if (doisPontos != -1) {
                String remetente = resto.substring(0, doisPontos).trim();
                String conteudoBruto = resto.substring(doisPontos + 2);
                String conteudoHtml = EmojiUtil.substituirEmojisPorImgHtml(escaparHtml(conteudoBruto));
                boolean souEu = nickname != null && nickname.equalsIgnoreCase(remetente);
                return new ItemMensagem(TipoItemChat.MENSAGEM, remetente, conteudoBruto, conteudoHtml, horario, souEu);
            }
        }
        String html = EmojiUtil.substituirEmojisPorImgHtml(escaparHtml(linha));
        return new ItemMensagem(TipoItemChat.MENSAGEM, "", linha, html, "", false);
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
            labelStatus.setForeground(COR_PERIGO);
            avatarMeuPerfil.atualizar(nickname != null ? "@" + nickname : "Você", false, false);
            if (conversaAtiva != null) {
                ItemMensagem itemStatus = new ItemMensagem(
                        TipoItemChat.SISTEMA, "", status, escaparHtml(status), "", false
                );
                adicionarMensagemEstruturadaNaConversa(
                        conversaAtiva,
                        "<div class='system'>[ " + escaparHtml(status) + " ]</div>",
                        itemStatus
                );
            }
            campoTexto.setEnabled(false);
            botaoEnviar.setEnabled(false);
            botaoEmoji.setEnabled(false);
            botaoNovoGrupo.setEnabled(false);
            botaoNovaConversa.setEnabled(false);
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

    private static Color obterCorAvatar(String identificador) {
        if (identificador == null || identificador.isEmpty()) {
            return PALETA_AVATARES[0];
        }
        String limpo = identificador.startsWith("@") || identificador.startsWith("#")
                ? identificador.substring(1)
                : identificador;
        if (limpo.equalsIgnoreCase(BotIaCliente.NICKNAME_PADRAO)) {
            return COR_IA_DESTAQUE;
        }
        int idx = Math.abs(limpo.toLowerCase(Locale.ROOT).hashCode()) % PALETA_AVATARES.length;
        return PALETA_AVATARES[idx];
    }

    private static String obterIniciais(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return "?";
        }
        if (identificador.startsWith("#")) {
            String nomeGrupo = identificador.substring(1).trim();
            return nomeGrupo.isEmpty() ? "#" : nomeGrupo.substring(0, 1).toUpperCase(Locale.ROOT);
        }
        String limpo = identificador.startsWith("@") ? identificador.substring(1).trim() : identificador.trim();
        if (limpo.isEmpty()) {
            return "?";
        }
        if (limpo.equalsIgnoreCase(BotIaCliente.NICKNAME_PADRAO)) {
            return "IA";
        }
        return limpo.substring(0, Math.min(2, limpo.length())).toUpperCase(Locale.ROOT);
    }

    private static String formatarNomeExibicao(String identificador) {
        if (identificador == null || identificador.isEmpty()) {
            return "";
        }
        String limpo = (identificador.startsWith("@") || identificador.startsWith("#"))
                ? identificador.substring(1)
                : identificador;
        if (limpo.isEmpty()) {
            return identificador;
        }
        if (limpo.equalsIgnoreCase(BotIaCliente.NICKNAME_PADRAO)) {
            return "Assistente IA ✨";
        }
        String[] partes = limpo.split("[-_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (!p.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) {
                    sb.append(p.substring(1));
                }
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // COMPONENTES VISUAIS CUSTOMIZADOS (AVATAR, TELA INICIAL, BALOES E SIDEBAR)
    // =========================================================================

    /**
     * Avatar circular reutilizavel com suporte a indicador de status online.
     */
    private class AvatarComponente extends JComponent {
        private final int diametro;
        private final boolean exibirIndicadorOnline;
        private String identificador = "";
        private boolean ehGrupo = false;
        private boolean online = false;

        public AvatarComponente(int diametro, boolean exibirIndicadorOnline) {
            this.diametro = diametro;
            this.exibirIndicadorOnline = exibirIndicadorOnline;
            setPreferredSize(new Dimension(diametro + 4, diametro + 4));
            setMinimumSize(new Dimension(diametro + 4, diametro + 4));
            setOpaque(false);
        }

        public void atualizar(String identificador, boolean ehGrupo, boolean online) {
            this.identificador = identificador != null ? identificador : "";
            this.ehGrupo = ehGrupo;
            this.online = online;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int x = (getWidth() - diametro) / 2;
            int y = (getHeight() - diametro) / 2;

            g2.setColor(obterCorAvatar(identificador));
            g2.fillOval(x, y, diametro, diametro);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(11, diametro / 3)));
            String texto = ehGrupo ? "#" : obterIniciais(identificador);
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (diametro - fm.stringWidth(texto)) / 2;
            int ty = y + ((diametro - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(texto, tx, ty);

            if (exibirIndicadorOnline && !ehGrupo && online) {
                int tamDot = Math.max(10, diametro / 3 - 1);
                int dx = x + diametro - tamDot + 1;
                int dy = y + diametro - tamDot + 1;
                g2.setColor(temaAtual.fundoJanela);
                g2.fillOval(dx - 2, dy - 2, tamDot + 4, tamDot + 4);
                g2.setColor(COR_STATUS_ONLINE);
                g2.fillOval(dx, dy, tamDot, tamDot);
            }
            g2.dispose();
        }
    }

    /**
     * Tela inicial apresentada na area direita quando nenhuma conversa esta selecionada.
     */
    private class PainelBoasVindas extends JPanel {
        public PainelBoasVindas() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            g2.setColor(temaAtual.fundoJanela);
            g2.fillRect(0, 0, getWidth(), getHeight());

            int cx = getWidth() / 2;
            int cy = getHeight() / 2 - 35;

            // Ilustracao vetorial discreta (circulo suave + baloes de conversa)
            g2.setColor(temaAtual == Tema.ESCURO || temaAtual == Tema.DRACULA
                    ? new Color(255, 255, 255, 14)
                    : new Color(37, 211, 102, 24));
            g2.fillOval(cx - 56, cy - 56, 112, 112);

            // Balao esquerdo
            g2.setColor(temaAtual.corBolhaRecebida);
            g2.fillRoundRect(cx - 36, cy - 26, 48, 34, 14, 14);
            g2.setColor(temaAtual.corBorda);
            g2.drawRoundRect(cx - 36, cy - 26, 48, 34, 14, 14);

            // Linhas simuladas no balao esquerdo
            g2.setColor(temaAtual.textoSecundario);
            g2.fillRoundRect(cx - 26, cy - 15, 28, 4, 4, 4);
            g2.fillRoundRect(cx - 26, cy - 6, 18, 4, 4, 4);

            // Balao direito em destaque verde WhatsApp
            g2.setColor(COR_VERDE_DESTAQUE);
            g2.fillRoundRect(cx - 6, cy - 4, 46, 32, 14, 14);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(cx + 4, cy + 7, 26, 4, 4, 4);
            g2.fillRoundRect(cx + 4, cy + 15, 16, 4, 4, 4);

            // Titulo
            g2.setColor(temaAtual.textoBranco);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
            String titulo = "Chat Multiusuário Desktop";
            FontMetrics fmTit = g2.getFontMetrics();
            g2.drawString(titulo, cx - fmTit.stringWidth(titulo) / 2, cy + 88);

            // Subtitulo explicativo
            g2.setColor(temaAtual.textoSecundario);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            String linha1 = "Selecione uma conversa na barra lateral esquerda para visualizar o histórico";
            String linha2 = "ou clique no botão '+' para iniciar uma nova conversa ou criar um grupo.";
            FontMetrics fmSub = g2.getFontMetrics();
            g2.drawString(linha1, cx - fmSub.stringWidth(linha1) / 2, cy + 116);
            g2.drawString(linha2, cx - fmSub.stringWidth(linha2) / 2, cy + 136);

            // Rodape discreto
            String rodape = "🔒 Mensagens sincronizadas em tempo real com histórico persistente";
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            FontMetrics fmRod = g2.getFontMetrics();
            g2.drawString(rodape, cx - fmRod.stringWidth(rodape) / 2, getHeight() - 32);

            // Barra inferior verde de destaque estilo WhatsApp Desktop
            g2.setColor(COR_VERDE_DESTAQUE);
            g2.fillRect(0, getHeight() - 5, getWidth(), 5);

            g2.dispose();
        }
    }

    /**
     * Painel vertical rolavel que exibe os baloes de mensagens estilo WhatsApp Desktop.
     */
    private class PainelListaMensagens extends JPanel implements Scrollable {
        private final JPanel containerLinhas;

        public PainelListaMensagens() {
            setLayout(new BorderLayout());
            containerLinhas = new JPanel();
            containerLinhas.setLayout(new BoxLayout(containerLinhas, BoxLayout.Y_AXIS));
            containerLinhas.setOpaque(false);
            containerLinhas.setBorder(new EmptyBorder(14, 18, 16, 18));
            add(containerLinhas, BorderLayout.NORTH);
        }

        public void reconstruirConversa(String chaveConversa, List<ItemMensagem> itens) {
            containerLinhas.removeAll();
            containerLinhas.add(criarPilulaCentral("Hoje", false));
            containerLinhas.add(Box.createVerticalStrut(8));

            if (CONVERSA_IA_PADRAO.equalsIgnoreCase(chaveConversa) && itens.isEmpty()) {
                String msgIa = BotIaCliente.possuiChaveConfigurada()
                        ? "✨ Assistente IA (Groq) pronto! Envie qualquer pergunta abaixo."
                        : "✨ Assistente IA — Clique em '🔑 Configurar Key IA' no topo para colar sua key da Groq.";
                containerLinhas.add(criarPilulaCentral(msgIa, false));
                containerLinhas.add(Box.createVerticalStrut(8));
            }

            for (ItemMensagem item : itens) {
                adicionarComponenteItem(chaveConversa, item);
            }
            revalidate();
            repaint();
        }

        public void adicionarItemVisual(String chaveConversa, ItemMensagem item) {
            if (containerLinhas.getComponentCount() == 0) {
                containerLinhas.add(criarPilulaCentral("Hoje", false));
                containerLinhas.add(Box.createVerticalStrut(8));
            }
            adicionarComponenteItem(chaveConversa, item);
            revalidate();
            repaint();
        }

        private void adicionarComponenteItem(String chaveConversa, ItemMensagem item) {
            if (item.tipo() == TipoItemChat.SISTEMA) {
                containerLinhas.add(criarPilulaCentral(item.conteudoTexto(), false));
                containerLinhas.add(Box.createVerticalStrut(8));
            } else if (item.tipo() == TipoItemChat.ERRO) {
                containerLinhas.add(criarPilulaCentral(item.conteudoTexto(), true));
                containerLinhas.add(Box.createVerticalStrut(8));
            } else {
                containerLinhas.add(criarLinhaBalao(chaveConversa, item));
                containerLinhas.add(Box.createVerticalStrut(6));
            }
        }

        private JComponent criarPilulaCentral(String texto, boolean erro) {
            JPanel linha = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
            linha.setOpaque(false);

            JPanel pilula = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(erro ? new Color(253, 232, 232) : temaAtual.corBolhaRecebida);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.setColor(erro ? COR_PERIGO : temaAtual.corBorda);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                    g2.dispose();
                }
            };
            pilula.setOpaque(false);
            pilula.setBorder(new EmptyBorder(5, 14, 5, 14));

            String html = EmojiUtil.substituirEmojisPorImgHtml(escaparHtml(texto));
            JLabel lbl = new JLabel("<html><div style='text-align:center;'>" + html + "</div></html>");
            lbl.setFont(new Font("Segoe UI", erro ? Font.BOLD : Font.PLAIN, 11));
            lbl.setForeground(erro ? COR_PERIGO : temaAtual.textoSecundario);

            pilula.add(lbl, BorderLayout.CENTER);
            linha.add(pilula);
            return linha;
        }

        private JComponent criarLinhaBalao(String chaveConversa, ItemMensagem item) {
            boolean enviada = item.enviadaPorMim();
            boolean ehGrupo = chaveConversa != null && chaveConversa.startsWith("#");

            JPanel linhaWrapper = new JPanel(new BorderLayout());
            linhaWrapper.setOpaque(false);

            JPanel fluxoAlinhado = new JPanel(new FlowLayout(enviada ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 0));
            fluxoAlinhado.setOpaque(false);

            if (!enviada) {
                AvatarComponente miniAvatar = new AvatarComponente(28, false);
                miniAvatar.atualizar("@" + item.remetente(), false, false);
                JPanel wrapperAvatar = new JPanel(new BorderLayout());
                wrapperAvatar.setOpaque(false);
                wrapperAvatar.add(miniAvatar, BorderLayout.NORTH);
                fluxoAlinhado.add(wrapperAvatar);
            }

            // Painel do balao arredondado
            JPanel balao = new JPanel(new BorderLayout(0, 3)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Sombra discreta
                    g2.setColor(new Color(0, 0, 0, 14));
                    g2.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 2, 16, 16);

                    // Fundo do balao (verde suave para enviadas, branco para recebidas)
                    g2.setColor(enviada ? temaAtual.corBolhaEnviada : temaAtual.corBolhaRecebida);
                    g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 16, 16);

                    // Borda discreta
                    g2.setColor(new Color(0, 0, 0, 18));
                    g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 16, 16);
                    g2.dispose();
                }
            };
            balao.setOpaque(false);
            balao.setBorder(new EmptyBorder(7, 12, 7, 12));

            // Nome do remetente no topo do balao para mensagens recebidas
            if (!enviada && item.remetente() != null && !item.remetente().isEmpty()) {
                JLabel lblRemetente = new JLabel(ehGrupo ? formatarNomeExibicao(item.remetente()) : "@" + item.remetente());
                lblRemetente.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lblRemetente.setForeground(obterCorAvatar(item.remetente()));
                balao.add(lblRemetente, BorderLayout.NORTH);
            }

            // Conteudo da mensagem com quebra automatica se for muito longa
            String textoBruto = item.conteudoTexto() != null ? item.conteudoTexto() : "";
            String estiloLargura = textoBruto.length() > 58 ? "width: 310px;" : "";
            JLabel lblConteudo = new JLabel(
                    "<html><div style='" + estiloLargura + " line-height: 1.35;'>"
                            + item.conteudoHtml()
                            + "</div></html>"
            );
            lblConteudo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblConteudo.setForeground(temaAtual.textoBranco);
            balao.add(lblConteudo, BorderLayout.CENTER);

            // Rodape do balao com horario discreto e checkmarks
            JPanel rodapeBalao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            rodapeBalao.setOpaque(false);

            String horaExibicao = extrairApenasHora(item.horario());
            JLabel lblHora = new JLabel(horaExibicao);
            lblHora.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblHora.setForeground(temaAtual.textoSecundario);
            rodapeBalao.add(lblHora);

            if (enviada) {
                JLabel lblCheck = new JLabel("✓✓");
                lblCheck.setFont(new Font("Segoe UI Symbol", Font.BOLD, 10));
                lblCheck.setForeground(COR_CHECK_LIDO);
                rodapeBalao.add(lblCheck);
            }

            balao.add(rodapeBalao, BorderLayout.SOUTH);

            fluxoAlinhado.add(balao);
            linhaWrapper.add(fluxoAlinhado, enviada ? BorderLayout.EAST : BorderLayout.WEST);
            return linhaWrapper;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(temaAtual.fundoChat);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Padrao pontilhado ultra discreto inspirado no fundo de conversa do WhatsApp
            g2.setColor(temaAtual == Tema.ESCURO || temaAtual == Tema.DRACULA
                    ? new Color(255, 255, 255, 6)
                    : new Color(0, 0, 0, 7));
            int passo = 28;
            for (int y = 12; y < getHeight(); y += passo) {
                for (int x = 12; x < getWidth(); x += passo) {
                    g2.fillOval(x, y, 2, 2);
                }
            }
            g2.dispose();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 80;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Renderizador customizado da Sidebar que desenha:
     * - Avatar circular com cor consistente e ponto verde de status online
     * - Nome do usuario/grupo
     * - Horario da ultima mensagem no canto superior direito
     * - Previa da ultima mensagem na linha inferior
     * - Badge circular verde (#25D366) de mensagens nao lidas
     */
    private class RenderizadorConversaModerno extends JPanel implements ListCellRenderer<String> {

        private String itemTexto = "";
        private boolean selecionado = false;
        private boolean hover = false;
        private int naoLidas = 0;

        public RenderizadorConversaModerno() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            this.itemTexto = value != null ? value : "";
            this.selecionado = isSelected;
            this.hover = (index == itemHoverIndex);
            this.naoLidas = naoLidasPorConversa.getOrDefault(this.itemTexto, 0);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            // Fundo da celula
            g2.setColor(temaAtual.fundoSidebar);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (selecionado) {
                g2.setColor(temaAtual.corItemSelecionado);
                g2.fillRoundRect(6, 3, getWidth() - 12, getHeight() - 6, 12, 12);
            } else if (hover) {
                g2.setColor(temaAtual.fundoJanela);
                g2.fillRoundRect(6, 3, getWidth() - 12, getHeight() - 6, 12, 12);
            }

            boolean ehGrupo = itemTexto.startsWith("#");
            String nomeLimpo = itemTexto.length() > 1 ? itemTexto.substring(1) : itemTexto;
            boolean online = !ehGrupo && usuariosOnline.contains(nomeLimpo.toLowerCase(Locale.ROOT));

            // 1. Avatar Circular (44px)
            int tamAvatar = 44;
            int xAvatar = 16;
            int yAvatar = (getHeight() - tamAvatar) / 2;

            g2.setColor(obterCorAvatar(itemTexto));
            g2.fillOval(xAvatar, yAvatar, tamAvatar, tamAvatar);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            String inicial = ehGrupo ? "#" : obterIniciais(itemTexto);
            FontMetrics fmAvatar = g2.getFontMetrics();
            int tx = xAvatar + (tamAvatar - fmAvatar.stringWidth(inicial)) / 2;
            int ty = yAvatar + ((tamAvatar - fmAvatar.getHeight()) / 2) + fmAvatar.getAscent();
            g2.drawString(inicial, tx, ty);

            // Indicador de status online no canto inferior direito do avatar
            if (!ehGrupo) {
                int tamStatus = 12;
                int sx = xAvatar + tamAvatar - tamStatus + 1;
                int sy = yAvatar + tamAvatar - tamStatus + 1;
                g2.setColor(selecionado ? temaAtual.corItemSelecionado : temaAtual.fundoSidebar);
                g2.fillOval(sx - 2, sy - 2, tamStatus + 4, tamStatus + 4);
                g2.setColor(online ? COR_STATUS_ONLINE : new Color(180, 185, 190));
                g2.fillOval(sx, sy, tamStatus, tamStatus);
            }

            // 2. Horario da ultima mensagem no canto superior direito
            String horario = ultimoHorarioPorConversa.getOrDefault(itemTexto, "");
            int margemDireita = 16;
            int larguraHorario = 0;
            if (!horario.isEmpty()) {
                g2.setFont(new Font("Segoe UI", naoLidas > 0 ? Font.BOLD : Font.PLAIN, 11));
                g2.setColor(naoLidas > 0 ? COR_VERDE_DESTAQUE : temaAtual.textoSecundario);
                FontMetrics fmHora = g2.getFontMetrics();
                larguraHorario = fmHora.stringWidth(horario);
                g2.drawString(horario, getWidth() - larguraHorario - margemDireita, 26);
            }

            // 3. Nome do Contato ou Grupo
            int xTexto = xAvatar + tamAvatar + 13;
            int limiteDireitoNome = getWidth() - larguraHorario - margemDireita - 10;
            String nomeExibicao = formatarNomeExibicao(itemTexto);
            if (ehGrupo) {
                nomeExibicao = nomeExibicao + " (" + itemTexto + ")";
            }

            g2.setColor(temaAtual.textoBranco);
            g2.setFont(new Font("Segoe UI", naoLidas > 0 ? Font.BOLD : Font.BOLD, 14));
            String nomeTruncado = truncarTexto(nomeExibicao, g2.getFontMetrics(), Math.max(60, limiteDireitoNome - xTexto));
            g2.drawString(nomeTruncado, xTexto, 26);

            // 4. Badge de Mensagens Nao Lidas no canto inferior direito
            int larguraBadge = 0;
            if (naoLidas > 0) {
                String txtBadge = String.valueOf(naoLidas);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fmBadge = g2.getFontMetrics();
                larguraBadge = Math.max(20, fmBadge.stringWidth(txtBadge) + 10);
                int altBadge = 20;
                int xBadge = getWidth() - larguraBadge - margemDireita;
                int yBadge = 34;

                g2.setColor(COR_VERDE_DESTAQUE);
                g2.fillRoundRect(xBadge, yBadge, larguraBadge, altBadge, 20, 20);

                g2.setColor(Color.WHITE);
                int bx = xBadge + (larguraBadge - fmBadge.stringWidth(txtBadge)) / 2;
                int by = yBadge + ((altBadge - fmBadge.getHeight()) / 2) + fmBadge.getAscent();
                g2.drawString(txtBadge, bx, by);
            }

            // 5. Previa da ultima mensagem (ou status online/offline se ainda sem mensagens)
            boolean ehIa = itemTexto.equalsIgnoreCase(CONVERSA_IA_PADRAO);
            String previaPadrao = ehGrupo
                    ? "Grupo de conversa"
                    : ehIa
                        ? "✨ Assistente de IA • pergunte qualquer coisa"
                        : (online ? "Online • clique para conversar" : "Offline");
            String previa = ultimaPreviaPorConversa.getOrDefault(itemTexto, previaPadrao);

            g2.setColor(naoLidas > 0 ? temaAtual.textoBranco : temaAtual.textoSecundario);
            g2.setFont(new Font("Segoe UI", naoLidas > 0 ? Font.BOLD : Font.PLAIN, 12));
            int limiteDireitoPrevia = getWidth() - larguraBadge - margemDireita - 10;
            String previaTruncada = truncarTexto(previa, g2.getFontMetrics(), Math.max(60, limiteDireitoPrevia - xTexto));
            g2.drawString(previaTruncada, xTexto, 47);

            // Linha divisoria inferior sutil
            g2.setColor(temaAtual.corBorda);
            g2.drawLine(xTexto, getHeight() - 1, getWidth() - 12, getHeight() - 1);

            g2.dispose();
        }

        private String truncarTexto(String texto, FontMetrics fm, int larguraMaxima) {
            if (texto == null || fm.stringWidth(texto) <= larguraMaxima) {
                return texto != null ? texto : "";
            }
            String sufixo = "...";
            int largSufixo = fm.stringWidth(sufixo);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < texto.length(); i++) {
                if (fm.stringWidth(sb.toString() + texto.charAt(i)) + largSufixo > larguraMaxima) {
                    break;
                }
                sb.append(texto.charAt(i));
            }
            return sb + sufixo;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteChatGui gui = new ClienteChatGui();
            gui.iniciar();
        });
    }
}
