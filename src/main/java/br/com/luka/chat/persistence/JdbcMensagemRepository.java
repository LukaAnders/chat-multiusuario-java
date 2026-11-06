package br.com.luka.chat.persistence;

import br.com.luka.chat.model.Mensagem;
import br.com.luka.chat.model.MensagemRepository;
import br.com.luka.chat.model.PersistenciaException;
import br.com.luka.chat.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Implementacao JDBC. Todo SQL do projeto fica aqui.
 * Stateless: cada operacao abre e fecha sua propria conexao.
 */
public class JdbcMensagemRepository implements MensagemRepository {

    private static final String SQL_INSERIR =
        "INSERT INTO mensagem (remetente, destino, conteudo, horario) VALUES (?, ?, ?, ?)";
    private static final String SQL_POR_DESTINO =
        "SELECT id, remetente, destino, conteudo, horario FROM mensagem WHERE LOWER(destino) = LOWER(?) ORDER BY id DESC LIMIT ?";
    private static final String SQL_PRIVADAS =
        "SELECT id, remetente, destino, conteudo, horario FROM mensagem "
        + "WHERE (LOWER(remetente) = LOWER(?) AND LOWER(destino) = LOWER(?)) "
        + "   OR (LOWER(remetente) = LOWER(?) AND LOWER(destino) = LOWER(?)) "
        + "ORDER BY id DESC LIMIT ?";

    private final ConnectionFactory fabrica;

    public JdbcMensagemRepository(ConnectionFactory fabrica) {
        this.fabrica = fabrica;
    }

    @Override
    public Mensagem salvar(Mensagem mensagem) {
        try (Connection conexao = fabrica.abrir();
             PreparedStatement comando = conexao.prepareStatement(
                 SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {

            comando.setString(1, mensagem.getRemetente().getNome());
            comando.setString(2, mensagem.getDestino());
            comando.setString(3, mensagem.getConteudo());
            comando.setTimestamp(4, Timestamp.valueOf(mensagem.getHorario()));
            comando.executeUpdate();

            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new PersistenciaException("Id nao gerado", null);
                }
                return mensagem.comId(chaves.getLong(1));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao salvar mensagem", e);
        }
    }

    @Override
    public List<Mensagem> buscarUltimas(int quantidade) {
        return buscarPorDestino(Mensagem.DESTINO_GERAL, quantidade);
    }

    @Override
    public List<Mensagem> buscarPorDestino(String destino, int quantidade) {
        List<Mensagem> mensagens = new ArrayList<>();
        try (Connection conexao = fabrica.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_POR_DESTINO)) {

            comando.setString(1, destino.trim().toLowerCase(Locale.ROOT));
            comando.setInt(2, quantidade);
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    mensagens.add(mapearMensagem(resultado));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao buscar historico por destino", e);
        }
        Collections.reverse(mensagens);
        return mensagens;
    }

    @Override
    public List<Mensagem> buscarPrivadas(Usuario usuarioA, Usuario usuarioB, int quantidade) {
        List<Mensagem> mensagens = new ArrayList<>();
        String nomeA = usuarioA.getNome().toLowerCase(Locale.ROOT);
        String nomeB = usuarioB.getNome().toLowerCase(Locale.ROOT);
        String destA = "@" + nomeA;
        String destB = "@" + nomeB;

        try (Connection conexao = fabrica.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_PRIVADAS)) {

            comando.setString(1, nomeA);
            comando.setString(2, destB);
            comando.setString(3, nomeB);
            comando.setString(4, destA);
            comando.setInt(5, quantidade);

            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    mensagens.add(mapearMensagem(resultado));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao buscar historico privado", e);
        }
        Collections.reverse(mensagens);
        return mensagens;
    }

    private Mensagem mapearMensagem(ResultSet resultado) throws SQLException {
        return new Mensagem(
            resultado.getLong("id"),
            new Usuario(resultado.getString("remetente")),
            resultado.getString("destino"),
            resultado.getString("conteudo"),
            resultado.getTimestamp("horario").toLocalDateTime()
        );
    }
}
