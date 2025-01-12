package com.parallelai.exec.app;

import com.parallelai.exec.train.TrainerManager;
import com.parallelai.exec.files.FilesManager;
import com.parallelai.exec.play.GameManager;
import java.util.Scanner;

/**
 * Gestionnaire principal de l'application Othello AI.
 * Cette classe gère :
 * - Le menu principal de l'application
 * - L'accès aux différentes fonctionnalités (jeu, entraînement, gestion des fichiers)
 * - La navigation entre les différents modules
 */
public class AppManager {
    private final Scanner scanner;
    
    /**
     * Constructeur du gestionnaire d'application
     * Initialise le scanner pour la saisie utilisateur
     */
    public AppManager() {
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * Démarre l'application et affiche le menu principal
     * Gère la boucle principale de l'application
     */
    public void start() {
        while (true) {
            displayMainMenu();
            int choice = scanner.nextInt();
            
            switch (choice) {
                case 1:
                    System.out.println("\n=== Gestionnaire de Jeu ===");
                    new GameManager().initialize();
                    break;
                    
                case 2:
                    System.out.println("\n=== Entraînement des Modèles ===");
                    new TrainerManager().startTraining();
                    break;
                    
                case 3:
                    System.out.println("\n=== Gestionnaire de Fichiers ===");
                    new FilesManager().startFileManager();
                    break;
                    
                case 4:
                    System.out.println("Fermeture de l'application...");
                    scanner.close();
                    return;
                    
                default:
                    System.out.println("Choix invalide. Veuillez réessayer.");
            }
        }
    }
    
    /**
     * Affiche le menu principal de l'application
     * Présente les différentes options disponibles à l'utilisateur
     */
    private void displayMainMenu() {
        System.out.println("\n=== Application Othello AI ===");
        System.out.println("1. Jouer une partie");
        System.out.println("2. Entraîner un modèle");
        System.out.println("3. Gérer les fichiers");
        System.out.println("4. Quitter");
        System.out.print("Entrez votre choix (1-4) : ");
    }
    
    /**
     * Point d'entrée principal de l'application
     */
    public static void main(String[] args) {
        new AppManager().start();
    }
}
