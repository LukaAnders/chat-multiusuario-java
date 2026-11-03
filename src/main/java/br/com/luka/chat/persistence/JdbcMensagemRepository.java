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

/**
 * Implementacao JDBC. Todo SQL do projeto fica aqui.
 * Stateless: cada operacao abre e fecha sua propria conexao.
 */
public class JdbcMensagemRepository implements MensagemRepository {

    private static final String SQL_INSERIR =
        "INSERT INTO mensagem (remetente, conteudo, horario) VALUES (?, ?, ?)";
    private static final String SQL_ULTIMAS =
        "SELECT id, remetente, conteudo, horario FROM mensagem ORDER BY id DESC LIMIT ?";

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
            comando.setString(2, mensagem.getConteudo());
            comando.setTimestamp(3, Timestamp.valueOf(mensagem.getHorario()));
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
        List<Mensagem> mensagens = new ArrayList<>();
        try (Connection conexao = fabrica.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_ULTIMAS)) {

            comando.setInt(1, quantidade);
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    mensagens.add(new Mensagem(
                        resultado.getLong("id"),
                        new Usuario(resultado.getString("remetente")),
                        resultado.getString("conteudo"),
                        resultado.getTimestamp("horario").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao buscar historico", e);
        }
        Collections.reverse(mensagens); // mais antiga primeiro
        return mensagens;
    }
}
