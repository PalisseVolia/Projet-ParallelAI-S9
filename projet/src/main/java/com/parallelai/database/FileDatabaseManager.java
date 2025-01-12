package com.parallelai.database;

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
    // Paramètres de connexion à la base de données
    private static final String URL = "jdbc:mysql://sql2.minestrator.com:3306/minesr_iVGkRPW9";
    private static final String USER = "minesr_iVGkRPW9";
    private static final String PASSWORD = "53GCC6KpeTZnCHmW";

    // Chemins des différents dossiers de ressources
    private static final String datasetsPath = "projet\\src\\main\\ressources\\data";
    private static final String CnnModelPath = "projet\\src\\main\\ressources\\models\\CNN";
    private static final String MlpModelPath = "projet\\src\\main\\ressources\\models\\MLP";

    // Programme principal de test
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int type = 0;

        while (true) {
            System.out.println("\nChoisissez une action :");
            System.out.println("1. Insérer un fichier dans la base de données");
            System.out.println("2. Supprimer un fichier de la base de données");
            System.out.println("3. Lister tous les fichiers de la base de données");
            System.out.println("4. Télécharger un fichier depuis la base de données");
            System.out.println("5. Quitter");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice != 5) {
                System.out.println("\nQuel type de fichier souhaitez-vous manipuler :");
                System.out.println("1. Modèle CNN");
                System.out.println("2. Modèle MLP");
                System.out.println("3. Jeu de données");
                type = scanner.nextInt();
                scanner.nextLine();
            }

            switch (choice) {
                case 1:
                    System.out.print("Entrez le chemin du fichier : ");
                    String filePath = scanner.nextLine();
                    insertFile(filePath, type);
                    break;
                case 2:
                    String[] files = getFileList(type);
                    if (files.length == 0) {
                        System.out.println("Aucun fichier disponible à supprimer.");
                        break;
                    }
                    System.out.println("Sélectionnez un fichier à supprimer :");
                    displayNumberedList(files);
                    int fileIndex = scanner.nextInt();
                    scanner.nextLine(); // consume newline
                    if (fileIndex > 0 && fileIndex <= files.length) {
                        deleteFile(files[fileIndex - 1], type);
                    } else {
                        System.out.println("Sélection invalide.");
                    }
                    break;
                case 3:
                    listFiles(type);
                    break;
                case 4:
                    String[] downloadFiles = getFileList(type);
                    if (downloadFiles.length == 0) {
                        System.out.println("Aucun fichier disponible à télécharger.");
                        break;
                    }
                    System.out.println("Sélectionnez un fichier à télécharger :");
                    displayNumberedList(downloadFiles);
                    int downloadIndex = scanner.nextInt();
                    scanner.nextLine();
                    if (downloadIndex > 0 && downloadIndex <= downloadFiles.length) {
                        downloadFile(downloadFiles[downloadIndex - 1], type);
                    } else {
                        System.out.println("Sélection invalide.");
                    }
                    break;
                case 5:
                    System.out.println("Quitter...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Choix invalide. Réessayez.");
            }
        }
    }

    // Insère un fichier dans la base de données
    public static void insertFile(String filePath, int type) {
        String sql;
        if (type == 3) {
            sql = "INSERT INTO dataSet (file_name, file_data) VALUES (?, ?)";
            
            try (
                Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
                
                preparedStatement.setString(1, new File(filePath).getName());
                preparedStatement.setBinaryStream(2, fileInputStream, fileInputStream.available());

                int rowsInserted = preparedStatement.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("Jeu de données inséré avec succès !");
                } else {
                    System.out.println("Échec de l'insertion du jeu de données.");
                }
            } catch (SQLException | IOException e) {
                System.out.println("Erreur lors de l'insertion du fichier :");
                e.printStackTrace();
            }
        } else {
            sql = "INSERT INTO model (file_name, file_data, model_type) VALUES (?, ?, ?)";
            
            try (
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
                preparedStatement.setString(1, new File(filePath).getName());
                preparedStatement.setBinaryStream(2, fileInputStream, fileInputStream.available());
                preparedStatement.setString(3, type == 1 ? "CNN" : "MLP");

                int rowsInserted = preparedStatement.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("Fichier inséré avec succès !");
                } else {
                    System.out.println("Échec de l'insertion du fichier.");
                }
            } catch (SQLException e) {
                System.out.println("Erreur de base de données :");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Erreur de fichier :");
                e.printStackTrace();
            }
        }
    }

    // Supprime un fichier de la base de données
    public static void deleteFile(String filePath, int type) {
        if (type == 3) {
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
                    System.out.println("Fichier supprimé de la base de données avec succès !");
                } else {
                    System.out.println("Aucun fichier trouvé avec ce nom, échec de la suppression.");
                }
            } catch (SQLException e) {
                System.out.println("Erreur de base de données :");
                e.printStackTrace();
            }
        } else {
            String sql = "DELETE FROM model WHERE file_name = ? AND model_type = ?"; // SQL statement for deletion

            try (
                    // Establish database connection
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    // Prepare the DELETE SQL statement
                    PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

                // Set the file name parameter in the DELETE statement
                preparedStatement.setString(1, new File(filePath).getName());
                preparedStatement.setString(2, type == 1 ? "CNN" : "MLP");

                // Execute the DELETE query
                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("Fichier supprimé de la base de données avec succès !");
                } else {
                    System.out.println("Aucun fichier trouvé avec ce nom, échec de la suppression.");
                }
            } catch (SQLException e) {
                System.out.println("Erreur de base de données :");
                e.printStackTrace();
            }
        }
    }

    // Liste tous les fichiers dans la base de données
    public static void listFiles(int type) {
        if (type == 3) {
            String sql = "SELECT file_name FROM dataSet";

            try (
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    ResultSet resultSet = preparedStatement.executeQuery()) {
                System.out.println("Fichiers dans la base de données :");
                while (resultSet.next()) {
                    System.out.println("- " + resultSet.getString("file_name"));
                }
            } catch (SQLException e) {
                System.out.println("Erreur de base de données :");
                e.printStackTrace();
            }
        } else {
            String sql = "SELECT file_name FROM model WHERE model_type = ?";

            try (
                    Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                
                preparedStatement.setString(1, type == 1 ? "CNN" : "MLP");
                
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    System.out.println("Fichiers dans la base de données :");
                    while (resultSet.next()) {
                        System.out.println("- " + resultSet.getString("file_name"));
                    }
                }
            } catch (SQLException e) {
                System.out.println("Erreur de base de données :");
                e.printStackTrace();
            }
        }
    }

    // Télécharge un fichier depuis la base de données
    public static void downloadFile(String fileName, int type) {
        String destinationPath;
        if (type == 3) {
            destinationPath = datasetsPath;
        } else if (type == 1) {
            destinationPath = CnnModelPath;
        } else {
            destinationPath = MlpModelPath;
        }

        String sql = type == 3 ? 
            "SELECT file_data FROM dataSet WHERE file_name = ?" :
            "SELECT file_data FROM model WHERE file_name = ? AND model_type = ?";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setString(1, fileName);
            if (type != 3) {
                preparedStatement.setString(2, type == 1 ? "CNN" : "MLP");
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    try (InputStream inputStream = resultSet.getBinaryStream("file_data")) {
                        String fullPath = Paths.get(destinationPath, fileName).toString();
                        
                        // Create directories if they don't exist
                        new File(destinationPath).mkdirs();

                        // Write file
                        try (FileOutputStream outputStream = new FileOutputStream(fullPath)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            System.out.println("Fichier téléchargé avec succès vers : " + fullPath);
                        }
                    }
                } else {
                    System.out.println("Fichier non trouvé dans la base de données.");
                }
            }
        } catch (SQLException | IOException e) {
            System.out.println("Erreur lors du téléchargement du fichier :");
            e.printStackTrace();
        }
    }

    // Nouvelle surcharge de downloadFile avec chemin personnalisé
    public static void downloadFile(String fileName, String customPath, int type) {
        String sql = type == 3 ? 
            "SELECT file_data FROM dataSet WHERE file_name = ?" :
            "SELECT file_data FROM model WHERE file_name = ? AND model_type = ?";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setString(1, fileName);
            if (type != 3) {
                preparedStatement.setString(2, type == 1 ? "CNN" : "MLP");
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    try (InputStream inputStream = resultSet.getBinaryStream("file_data")) {
                        // Créer le dossier parent si nécessaire
                        new File(customPath).getParentFile().mkdirs();

                        // Écrire le fichier
                        try (FileOutputStream outputStream = new FileOutputStream(customPath)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            System.out.println("Fichier téléchargé avec succès vers : " + customPath);
                        }
                    }
                } else {
                    System.out.println("Fichier non trouvé dans la base de données.");
                }
            }
        } catch (SQLException | IOException e) {
            System.out.println("Erreur lors du téléchargement du fichier :");
            e.printStackTrace();
        }
    }

    public static String[] getFileList(int type) {
        java.util.List<String> files = new java.util.ArrayList<>();
        String sql = type == 3 ? 
            "SELECT file_name FROM dataSet" :
            "SELECT file_name FROM model WHERE model_type = ?";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            if (type != 3) {
                preparedStatement.setString(1, type == 1 ? "CNN" : "MLP");
            }
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    files.add(resultSet.getString("file_name"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur de base de données :");
            e.printStackTrace();
        }
        return files.toArray(new String[0]);
    }

    private static void displayNumberedList(String[] items) {
        for (int i = 0; i < items.length; i++) {
            System.out.println((i + 1) + ". " + items[i]);
        }
    }
}
