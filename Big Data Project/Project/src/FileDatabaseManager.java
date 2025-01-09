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

    // Database connection details 2
    private static final String URL = "jdbc:mysql://sql2.minestrator.com:3306/minesr_iVGkRPW9";
    private static final String USER = "minesr_iVGkRPW9";
    private static final String PASSWORD = "53GCC6KpeTZnCHmW";
    static int choice2;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nChoose an action:");
            System.out.println("1. Insert a file into the database");
            System.out.println("2. Delete a file in the database");
            System.out.println("3. List all files in the database");
            System.out.println("4. Download a file from the database");
            System.out.println("5. Exit");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice != 5) {
                System.out.println("\nWould you like to manipulate a:");
                System.out.println("1. Model");
                System.out.println("2. DataSet");
                choice2 = scanner.nextInt();
                scanner.nextLine();
            }

            switch (choice) {
                case 1:
                    System.out.print("Enter the file path: ");
                    String filePath = scanner.nextLine();
                    insertFile(filePath);
                    break;
                case 2:
                    System.out.print("Enter the file name to delete: ");
                    String fileName_delete = scanner.nextLine();
                    deleteFile(fileName_delete);
                    break;
                case 3:
                    listFiles();
                    break;
                case 4:
                    System.out.print("Enter the file name to download: ");
                    String fileName = scanner.nextLine();
                    System.out.print("Enter the destination path: ");
                    String destinationPath = scanner.nextLine();
                    downloadFile(fileName, destinationPath);
                    break;
                case 5:
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

        if (choice2 == 2) {
            String sql = "INSERT INTO dataSet (file_name, file_data) VALUES (?, ?)";

            try (
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
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
        } else {
            String sql = "INSERT INTO model (file_name, file_data) VALUES (?, ?)";

            try (
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
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
    }

    public static void deleteFile(String filePath) {
        if (choice2 == 2) {
            String sql = "DELETE FROM dataSet WHERE file_name = ?"; // SQL statement for deletion

            try (
                    // Establish database connection
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    // Prepare the DELETE SQL statement
                    PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

                // Set the file name parameter in the DELETE statement
                preparedStatement.setString(1, new File(filePath).getName());

                // Execute the DELETE query
                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("File deleted from database successfully!");
                } else {
                    System.out.println("No file found with the given name, deletion failed.");
                }
            } catch (SQLException e) {
                System.out.println("Database error:");
                e.printStackTrace();
            }
        } else {
            String sql = "DELETE FROM model WHERE file_name = ?"; // SQL statement for deletion

            try (
                    // Establish database connection
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    // Prepare the DELETE SQL statement
                    PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

                // Set the file name parameter in the DELETE statement
                preparedStatement.setString(1, new File(filePath).getName());

                // Execute the DELETE query
                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("File deleted from database successfully!");
                } else {
                    System.out.println("No file found with the given name, deletion failed.");
                }
            } catch (SQLException e) {
                System.out.println("Database error:");
                e.printStackTrace();
            }
        }
    }

    // List all files in the database
    public static void listFiles() {
        if (choice2 == 2) {
            String sql = "SELECT file_name FROM dataSet";

            try (
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    ResultSet resultSet = preparedStatement.executeQuery()) {
                System.out.println("Files in the database:");
                while (resultSet.next()) {
                    System.out.println("- " + resultSet.getString("file_name"));
                }
            } catch (SQLException e) {
                System.out.println("Database error:");
                e.printStackTrace();
            }
        } else {
            String sql = "SELECT file_name FROM model";

            try (
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    ResultSet resultSet = preparedStatement.executeQuery()) {
                System.out.println("Files in the database:");
                while (resultSet.next()) {
                    System.out.println("- " + resultSet.getString("file_name"));
                }
            } catch (SQLException e) {
                System.out.println("Database error:");
                e.printStackTrace();
            }
        }
    }

    // Download a file from the database
    public static void downloadFile(String fileName, String destinationPath) {
        if (choice2 == 2) {
            String sql = "SELECT file_data FROM dataSet WHERE file_name = ?";

            try (
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
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
        } else {
            String sql = "SELECT file_data FROM model WHERE file_name = ?";

            try (
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
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
}
