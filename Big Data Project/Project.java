import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConectarMySQL {
    public static void main(String[] args) {
        // URL do banco de dados
        String url = "jdbc:mysql://localhost:3306/nome_do_banco";
        String usuario = "seu_usuario";
        String senha = "sua_senha";

        // Conexão
        Connection conexao = null;

        try {
            // Registrar o driver JDBC (opcional em versões recentes do Java)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Estabelecer a conexão
            conexao = DriverManager.getConnection(url, usuario, senha);

            // Teste de conexão
            if (conexao != null) {
                System.out.println("Conexão estabelecida com sucesso!");
            }
        } catch (SQLException e) {
            System.out.println("Erro ao conectar: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Driver JDBC não encontrado: " + e.getMessage());
        } finally {
            // Fechar a conexão
            try {
                if (conexao != null) {
                    conexao.close();
                }
            } catch (SQLException e) {
                System.out.println("Erro ao fechar a conexão: " + e.getMessage());
            }
        }
    }
}
