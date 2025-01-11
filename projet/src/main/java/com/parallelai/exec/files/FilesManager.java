package com.parallelai.exec.files;

import com.parallelai.database.FileDatabaseManager;
import java.util.Scanner;

public class FilesManager {
    private final Scanner scanner;
    
    public FilesManager() {
        this.scanner = new Scanner(System.in);
    }
    
    public void startFileManager() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== File Manager ===");
            System.out.println("Choose file type to manage:");
            System.out.println("1. CNN Models");
            System.out.println("2. MLP Models");
            System.out.println("3. Datasets");
            System.out.println("4. Return to Main Menu");
            System.out.print("Enter your choice (1-4): ");
            
            int typeChoice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            
            if (typeChoice == 4) {
                running = false;
                continue;
            }
            
            if (typeChoice < 1 || typeChoice > 3) {
                System.out.println("Invalid choice. Please try again.");
                continue;
            }
            
            manageFilesOfType(typeChoice);
        }
    }
    
    private void manageFilesOfType(int type) {
        while (true) {
            String[] files = FileDatabaseManager.getFileList(type);
            
            if (files.length == 0) {
                System.out.println("No files available to delete.");
                return;
            }
            
            System.out.println("\nAvailable files:");
            for (int i = 0; i < files.length; i++) {
                System.out.println((i + 1) + ". " + files[i]);
            }
            System.out.println((files.length + 1) + ". Return to previous menu");
            System.out.print("Choose a file to delete (1-" + (files.length + 1) + "): ");
            
            int fileChoice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            
            if (fileChoice == files.length + 1) {
                return;
            }
            
            if (fileChoice < 1 || fileChoice > files.length) {
                System.out.println("Invalid choice. Please try again.");
                continue;
            }
            
            String selectedFile = files[fileChoice - 1];
            System.out.print("Are you sure you want to delete '" + selectedFile + "'? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            
            if (confirm.equals("y")) {
                FileDatabaseManager.deleteFile(selectedFile, type);
                System.out.println("File deleted successfully.");
            }
        }
    }
}
