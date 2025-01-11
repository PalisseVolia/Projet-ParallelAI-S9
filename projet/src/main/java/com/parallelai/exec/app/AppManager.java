package com.parallelai.exec.app;

import com.parallelai.exec.train.TrainerManager;
import com.parallelai.exec.play.GameManager;
import com.parallelai.exec.files.FilesManager;
import java.util.Scanner;

public class AppManager {
    private final Scanner scanner;
    
    public AppManager() {
        this.scanner = new Scanner(System.in);
    }
    
    public void start() {
        while (true) {
            displayMainMenu();
            int choice = scanner.nextInt();
            
            switch (choice) {
                case 1:
                    System.out.println("\n=== Game Manager ===");
                    new GameManager().initialize();
                    break;
                    
                case 2:
                    System.out.println("\n=== Model Trainer ===");
                    new TrainerManager().startTraining();
                    break;
                    
                case 3:
                    new FilesManager().startFileManager();
                    break;
                    
                case 4:
                    System.out.println("Exiting application...");
                    scanner.close();
                    return;
                    
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    private void displayMainMenu() {
        System.out.println("\n=== Parallel AI Application ===");
        System.out.println("1. Play Game");
        System.out.println("2. Train Model");
        System.out.println("3. Delete Files");
        System.out.println("4. Exit");
        System.out.print("Enter your choice (1-4): ");
    }
    
    public static void main(String[] args) {
        new AppManager().start();
    }
}
