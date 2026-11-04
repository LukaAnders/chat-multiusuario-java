package br.com.luka.chat.client;

import br.com.luka.chat.server.Protocolo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * Cliente com Interface Grafica (Swing) para o Chat Multiusuario.
 * 
 * Regra de concorrencia: a thread de rede NUNCA altera componentes
 * graficos diretamente; todas as atualizacoes sao despachadas via
 * SwingUtilities.invokeLater.
 */
public class ClienteChatGui extends JFrame {

    private final JTextArea areaMensagens;
    private final JTextField campoTexto;
    private final JButton botaoEnviar;
    private final JLabel labelStatus;

    private ConexaoRedeCliente conexao;
    private String nickname;

    public ClienteChatGui() {
        super("Chat Multiusuário POO");
        configurarAparencia();

        setLayout(new BorderLayout(10, 10));
        setSize(650, 500);
        setMinimumSize(new Dimension(450, 350));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Painel Superior: Status e identificacao
        JPanel painelTopo = new JPanel(new BorderLayout());
        painelTopo.setBorder(new EmptyBorder(8, 12, 4, 12));
        labelStatus = new JLabel("Desconectado");
        labelStatus.setFont(labelStatus.getFont().deriveFont(Font.BOLD));
        painelTopo.add(labelStatus, BorderLayout.WEST);
        add(painelTopo, BorderLayout.NORTH);

        // Painel Central: Area de mensagens com rolagem
        areaMensagens = new JTextArea();
        areaMensagens.setEditable(false);
        areaMensagens.setLineWrap(true);
        areaMensagens.setWrapStyleWord(true);
        areaMensagens.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        areaMensagens.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(areaMensagens);
        scroll.setBorder(BorderFactory.createTitledBorder("Mensagens do Chat"));
        add(scroll, BorderLayout.CENTER);

        // Painel Inferior: Campo de texto e botao de envio
        JPanel painelInferior = new JPanel(new BorderLayout(8, 0));
        painelInferior.setBorder(new EmptyBorder(8, 12, 12, 12));

        campoTexto = new JTextField();
        campoTexto.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        campoTexto.setEnabled(false);

        botaoEnviar = new JButton("Enviar");
        botaoEnviar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        botaoEnviar.setEnabled(false);

        painelInferior.add(campoTexto, BorderLayout.CENTER);
        painelInferior.add(botaoEnviar, BorderLayout.EAST);
        add(painelInferior, BorderLayout.SOUTH);

        // Acoes de envio
        botaoEnviar.addActionListener(e -> enviarMensagem());
        campoTexto.addActionListener(e -> enviarMensagem());

        // Tratamento ao fechar a janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fecharAplicacao();
            }
        });
    }

    private void configurarAparencia() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    public void iniciar() {
        setVisible(true);

        // Dialogo para conectar
        String host = "localhost";
        int porta = 5000;

        JTextField campoNick = new JTextField();
        JTextField campoHost = new JTextField(host);
        JTextField campoPorta = new JTextField(String.valueOf(porta));

        JPanel painelConexao = new JPanel(new GridLayout(3, 2, 5, 5));
        painelConexao.add(new JLabel("Seu Nickname:"));
        painelConexao.add(campoNick);
        painelConexao.add(new JLabel("Host:"));
        painelConexao.add(campoHost);
        painelConexao.add(new JLabel("Porta:"));
        painelConexao.add(campoPorta);

        int resultado = JOptionPane.showConfirmDialog(
                this,
                painelConexao,
                "Conectar ao Servidor",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (resultado != JOptionPane.OK_OPTION) {
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

        try {
            porta = Integer.parseInt(campoPorta.getText().trim());
        } catch (NumberFormatException e) {
            porta = 5000;
        }
        host = campoHost.getText().trim();

        conectarAoServidor(host, porta, nickname);
    }

    private void conectarAoServidor(String host, int porta, String nick) {
        labelStatus.setText("Conectando a " + host + ":" + porta + "...");

        conexao = new ConexaoRedeCliente(
                host,
                porta,
                this::receberLinhaDoServidor,
                this::atualizarStatus
        );

        // Executa a conexao fora da Event Dispatch Thread para nao congelar a UI
        new Thread(() -> {
            try {
                conexao.conectar(nick);
                SwingUtilities.invokeLater(() -> {
                    setTitle("Chat Multiusuário — Logado como: " + nick);
                    labelStatus.setText("Online como @" + nick + " (" + host + ":" + porta + ")");
                    campoTexto.setEnabled(true);
                    botaoEnviar.setEnabled(true);
                    campoTexto.requestFocusInWindow();
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    labelStatus.setText("Falha na conexão.");
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
            // Se for erro de protocolo (! ...)
            if (linha.startsWith(Protocolo.PREFIXO_ERRO)) {
                JOptionPane.showMessageDialog(
                        this,
                        linha.substring(Protocolo.PREFIXO_ERRO.length()),
                        "Aviso do Chat",
                        JOptionPane.WARNING_MESSAGE
                );
            }

            areaMensagens.append(linha + "\n");
            areaMensagens.setCaretPosition(areaMensagens.getDocument().getLength());
        });
    }

    private void atualizarStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            labelStatus.setText(status);
            areaMensagens.append("[" + status + "]\n");
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
