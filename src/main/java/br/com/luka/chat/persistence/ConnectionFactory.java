package br.com.luka.chat.persistence;

import br.com.luka.chat.model.PersistenciaException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cria uma Connection nova por operacao. Conexoes JDBC nao sao
 * compartilhadas entre threads.
 */
public class ConnectionFactory {

    private final String url;
    private final String usuario;
    private final String senha;

    public ConnectionFactory(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    public Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    /** Executa o schema.sql do classpath (idempotente: IF NOT EXISTS). */
    public void criarTabelas() {
        String script = lerScript("/schema.sql");
        try (Connection conexao = abrir();
             Statement comando = conexao.createStatement()) {
            for (String sql : script.split(";")) {
                String limpo = sql.lines()
                    .filter(l -> !l.trim().startsWith("--"))
                    .reduce("", (a, b) -> a + "\n" + b)
                    .trim();
                if (!limpo.isEmpty()) {
                    comando.execute(limpo);
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao criar tabelas", e);
        }
    }

    private String lerScript(String caminho) {
        try (InputStream entrada = ConnectionFactory.class.getResourceAsStream(caminho)) {
            if (entrada == null) {
                throw new IllegalStateException("Script nao encontrado: " + caminho);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler " + caminho, e);
        }
    }
}

