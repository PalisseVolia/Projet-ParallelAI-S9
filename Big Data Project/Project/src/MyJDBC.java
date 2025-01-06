import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MyJDBC {
    public static void main(String[] args) {
        try{
            Connection connection = DriverManager.getConnection(
            "jdbc:mysql://127.0.0.1:3306/login-schema",
            "root",
            "1234"
            );

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM `login-schema`.`users`");

        while(resultSet.next()){
            System.out.println(resultSet.getString("username"));
            System.out.println(resultSet.getString("password"));
        }

        }catch(SQLException e){
            e.printStackTrace();
        }      

    }

}
