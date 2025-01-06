import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.nio.file.Paths;

public class FileDatabaseManager {
    // Database connection details
    // private static final String URL = "jdbc:mysql://127.0.0.1:3306/login-schema";
    // private static final String USER = "root";
    // private static final String PASSWORD = "1234";
    
    //Database connection details 2
    private static final String URL = "jdbc:mysql://sql2.minestrator.com:3306/minesr_iVGkRPW9";
    private static final String USER = "minesr_iVGkRPW9";
    private static final String PASSWORD = "53GCC6KpeTZnCHmW";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nChoose an action:");
            System.out.println("1. Insert a file into the database");
            System.out.println("2. List all files in the database");
            System.out.println("3. Download a file from the database");
            System.out.println("4. Exit");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    System.out.print("Enter the file path: ");
                    String filePath = scanner.nextLine();
                    insertFile(filePath);
                    break;
                case 2:
                    listFiles();
                    break;
                case 3:
                    System.out.print("Enter the file name to download: ");
                    String fileName = scanner.nextLine();
                    System.out.print("Enter the destination path: ");
                    String destinationPath = scanner.nextLine();
                    downloadFile(fileName, destinationPath);
                    break;
                case 4:
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    // Insert a file into the database
    public static void insertFile(String filePath) {
        String sql = "INSERT INTO file_storage (file_name, file_data) VALUES (?, ?)";

        try (
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            FileInputStream fileInputStream = new FileInputStream(new File(filePath))
        ) {
            preparedStatement.setString(1, new File(filePath).getName());
            preparedStatement.setBinaryStream(2, fileInputStream, fileInputStream.available());

            int rowsInserted = preparedStatement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("File inserted successfully!");
            } else {
                System.out.println("Failed to insert the file.");
            }
        } catch (SQLException e) {
            System.out.println("Database error:");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("File error:");
            e.printStackTrace();
        }
    }

    // List all files in the database
    public static void listFiles() {
        String sql = "SELECT file_name FROM file_storage";

        try (
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery()
        ) {
            System.out.println("Files in the database:");
            while (resultSet.next()) {
                System.out.println("- " + resultSet.getString("file_name"));
            }
        } catch (SQLException e) {
            System.out.println("Database error:");
            e.printStackTrace();
        }
    }

    // Download a file from the database
    public static void downloadFile(String fileName, String destinationPath) {
        String sql = "SELECT file_data FROM file_storage WHERE file_name = ?";

        try (
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement preparedStatement = connection.prepareStatement(sql)
        ) {
            preparedStatement.setString(1, fileName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    try (InputStream inputStream = resultSet.getBinaryStream("file_data")) {
                        // Check if the destination path is a directory
                        File destinationFile = new File(destinationPath);
                        if (destinationFile.isDirectory()) {
                            destinationPath = Paths.get(destinationPath, fileName).toString();
                        }

                        // Write file
                        try (FileOutputStream outputStream = new FileOutputStream(destinationPath)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            System.out.println("File downloaded successfully to: " + destinationPath);
                        }
                    }
                } else {
                    System.out.println("File not found in the database.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error:");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("File write error:");
            e.printStackTrace();
        }
    }

}
