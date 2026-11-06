package br.com.luka.chat.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utilitario para renderizar Emojis Coloridos (Twemoji PNG) no Java Swing,
 * superando a limitacao monocromatica de fontes AWT.
 * Faz cache automatico em disco (.emojis-cache) para carregar instantaneamente
 * e funcionar offline apos o primeiro carregamento.
 */
public final class EmojiUtil {

    private static final String CDN_TWEMOJI =
        "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/";

    private static final Path PASTA_CACHE = Path.of("data", "emojis");

    private static final Map<String, String> MAPA_EMOJIS = new LinkedHashMap<>();
    private static final Map<String, ImageIcon> CACHE_ICONES = new ConcurrentHashMap<>();
    private static final Map<String, String> CACHE_URL_LOCAL = new ConcurrentHashMap<>();
    private static final ExecutorService POOL_DOWNLOAD = Executors.newFixedThreadPool(6);

    static {
        // Carinhas e Emocoes
        MAPA_EMOJIS.put("😀", "1f600");
        MAPA_EMOJIS.put("😂", "1f602");
        MAPA_EMOJIS.put("🤣", "1f923");
        MAPA_EMOJIS.put("😊", "1f60a");
        MAPA_EMOJIS.put("😍", "1f60d");
        MAPA_EMOJIS.put("😎", "1f60e");
        MAPA_EMOJIS.put("🤔", "1f914");
        MAPA_EMOJIS.put("😅", "1f605");
        MAPA_EMOJIS.put("😢", "1f622");
        MAPA_EMOJIS.put("😭", "1f62d");
        MAPA_EMOJIS.put("😡", "1f621");
        MAPA_EMOJIS.put("🤯", "1f92f");
        MAPA_EMOJIS.put("🥳", "1f973");
        MAPA_EMOJIS.put("😴", "1f634");
        // Gestos
        MAPA_EMOJIS.put("👍", "1f44d");
        MAPA_EMOJIS.put("👎", "1f44e");
        MAPA_EMOJIS.put("👏", "1f44f");
        MAPA_EMOJIS.put("🙌", "1f64c");
        MAPA_EMOJIS.put("🙏", "1f64f");
        MAPA_EMOJIS.put("🤝", "1f91d");
        MAPA_EMOJIS.put("👋", "1f44b");
        MAPA_EMOJIS.put("✌️", "270c");
        // Simbolos e Objetos
        MAPA_EMOJIS.put("❤️", "2764");
        MAPA_EMOJIS.put("🔥", "1f525");
        MAPA_EMOJIS.put("✨", "2728");
        MAPA_EMOJIS.put("🎉", "1f389");
        MAPA_EMOJIS.put("💯", "1f4af");
        MAPA_EMOJIS.put("🚀", "1f680");
        MAPA_EMOJIS.put("✅", "2705");
        MAPA_EMOJIS.put("❌", "274c");
        MAPA_EMOJIS.put("⚡", "26a1");
        MAPA_EMOJIS.put("💻", "1f4bb");
        MAPA_EMOJIS.put("☕", "2615");
        MAPA_EMOJIS.put("👀", "1f440");
        MAPA_EMOJIS.put("⭐", "2b50");
        MAPA_EMOJIS.put("💬", "1f4ac");
    }

    private EmojiUtil() {
    }

    public static Map<String, String> getMapaEmojis() {
        return MAPA_EMOJIS;
    }

    /**
     * Inicia o pre-carregamento assincrono dos emojis em segundo plano.
     */
    public static void preCarregarAssincrono(Runnable aoTerminar) {
        try {
            Files.createDirectories(PASTA_CACHE);
        } catch (Exception ignored) {
        }

        for (Map.Entry<String, String> entry : MAPA_EMOJIS.entrySet()) {
            String emoji = entry.getKey();
            String codigo = entry.getValue();
            POOL_DOWNLOAD.submit(() -> carregarOuBaixar(emoji, codigo));
        }

        POOL_DOWNLOAD.submit(() -> {
            if (aoTerminar != null) {
                SwingUtilities.invokeLater(aoTerminar);
            }
        });
    }

    private static File carregarOuBaixar(String emoji, String codigoHex) {
        File arquivoLocal = PASTA_CACHE.resolve(codigoHex + ".png").toFile();
        if (!arquivoLocal.exists() || arquivoLocal.length() == 0) {
            try {
                URL url = URI.create(CDN_TWEMOJI + codigoHex + ".png").toURL();
                try (InputStream in = url.openStream()) {
                    Files.copy(in, arquivoLocal.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            }
        }
        if (arquivoLocal.exists() && arquivoLocal.length() > 0) {
            try {
                CACHE_URL_LOCAL.put(emoji, arquivoLocal.toURI().toURL().toExternalForm());
                BufferedImage img = ImageIO.read(arquivoLocal);
                if (img != null) {
                    Image redim = img.getScaledInstance(22, 22, Image.SCALE_SMOOTH);
                    CACHE_ICONES.put(emoji, new ImageIcon(redim));
                }
            } catch (Exception ignored) {
            }
        }
        return arquivoLocal;
    }

    /**
     * Devolve o Icone colorido 22x22 do emoji para botoes Swing.
     * Se ainda nao estiver em cache, carrega ou desenha um icone colorido de fallback.
     */
    public static ImageIcon obterIcone(String emoji, int tamanho) {
        String codigo = MAPA_EMOJIS.get(emoji);
        if (codigo == null) {
            return criarIconeFallbackColorido(emoji, tamanho);
        }

        if (tamanho == 22 && CACHE_ICONES.containsKey(emoji)) {
            return CACHE_ICONES.get(emoji);
        }

        File arq = carregarOuBaixar(emoji, codigo);
        if (arq.exists() && arq.length() > 0) {
            try {
                BufferedImage img = ImageIO.read(arq);
                if (img != null) {
                    Image redim = img.getScaledInstance(tamanho, tamanho, Image.SCALE_SMOOTH);
                    ImageIcon icone = new ImageIcon(redim);
                    if (tamanho == 22) {
                        CACHE_ICONES.put(emoji, icone);
                    }
                    return icone;
                }
            } catch (Exception ignored) {
            }
        }
        return criarIconeFallbackColorido(emoji, tamanho);
    }

    /**
     * Substitui todos os emojis conhecidos de um texto escapado por tags <img>
     * apontando para os PNGs coloridos em cache, permitindo que o JTextPane
     * renderize os emojis 100% coloridos!
     */
    public static String substituirEmojisPorImgHtml(String textoHtmlEscapado) {
        if (textoHtmlEscapado == null || textoHtmlEscapado.isEmpty()) {
            return "";
        }
        String resultado = textoHtmlEscapado;
        for (Map.Entry<String, String> entry : MAPA_EMOJIS.entrySet()) {
            String emoji = entry.getKey();
            if (resultado.contains(emoji)) {
                String urlImg = CACHE_URL_LOCAL.get(emoji);
                if (urlImg == null) {
                    File arq = carregarOuBaixar(emoji, entry.getValue());
                    if (arq.exists() && arq.length() > 0) {
                        try {
                            urlImg = arq.toURI().toURL().toExternalForm();
                            CACHE_URL_LOCAL.put(emoji, urlImg);
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (urlImg == null) {
                    urlImg = CDN_TWEMOJI + entry.getValue() + ".png";
                }
                String tagImg = "<img src='" + urlImg + "' width='18' height='18' align='bottom'>&nbsp;";
                resultado = resultado.replace(emoji, tagImg);
            }
        }
        return resultado;
    }

    private static ImageIcon criarIconeFallbackColorido(String emoji, int tam) {
        BufferedImage img = new BufferedImage(tam, tam, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(255, 204, 77));
        g2.fillOval(1, 1, tam - 2, tam - 2);
        g2.setColor(new Color(102, 69, 0));
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, tam - 6));
        FontMetrics fm = g2.getFontMetrics();
        int x = (tam - fm.stringWidth(emoji)) / 2;
        int y = ((tam - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(emoji, x, y);
        g2.dispose();
        return new ImageIcon(img);
    }
}
