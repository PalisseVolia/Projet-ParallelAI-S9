package com.parallelai.exec.files;

import com.parallelai.database.FileDatabaseManager;
import java.util.Scanner;

/**
 * Gestionnaire de fichiers permettant la manipulation des modèles CNN, MLP et
 * des jeux de données.
 * Cette classe fournit une interface utilisateur en ligne de commande pour
 * gérer les fichiers
 * stockés dans le système.
 */
public class FilesManager {
    private final Scanner scanner;

    /**
     * Constructeur initialisant le scanner pour la lecture des entrées utilisateur.
     */
    public FilesManager() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Démarre l'interface de gestion des fichiers.
     * Affiche un menu permettant à l'utilisateur de choisir le type de fichiers
     * à gérer (CNN, MLP, Datasets) et gère les interactions utilisateur.
     */
    public void startFileManager() {
        boolean running = true;
        while (running) {
            System.out.println("Choisissez le type de fichier :");
            System.out.println("1. Modèles CNN");
            System.out.println("2. Modèles MLP");
            System.out.println("3. Jeux de données");
            System.out.println("4. Retour au Menu Principal");
            System.out.print("Entrez votre choix (1-4) : ");

            int typeChoice = scanner.nextInt();
            scanner.nextLine();

            if (typeChoice == 4) {
                running = false;
                continue;
            }

            if (typeChoice < 1 || typeChoice > 3) {
                System.out.println("Choix invalide. Veuillez réessayer.");
                continue;
            }

            manageFilesOfType(typeChoice);
        }
    }

    /**
     * Gère les fichiers d'un type spécifique en permettant leur suppression.
     * 
     * @param type Le type de fichier à gérer (1 pour CNN, 2 pour MLP, 3 pour
     *             Datasets)
     */
    private void manageFilesOfType(int type) {
        while (true) {
            String[] files = FileDatabaseManager.getFileList(type);

            if (files.length == 0) {
                System.out.println("Aucun fichier disponible à supprimer.");
                return;
            }

            System.out.println("\nFichiers disponibles :");
            for (int i = 0; i < files.length; i++) {
                System.out.println((i + 1) + ". " + files[i]);
            }
            System.out.println((files.length + 1) + ". Retour au menu précédent");
            System.out.print("Choisissez un fichier à supprimer (1-" + (files.length + 1) + ") : ");

            int fileChoice = scanner.nextInt();
            scanner.nextLine();

            if (fileChoice == files.length + 1) {
                return;
            }

            if (fileChoice < 1 || fileChoice > files.length) {
                System.out.println("Choix invalide. Veuillez réessayer.");
                continue;
            }

            String selectedFile = files[fileChoice - 1];
            System.out.print("Êtes-vous sûr de vouloir supprimer '" + selectedFile + "' ? (y/n) : ");
            String confirm = scanner.nextLine().trim().toLowerCase();

            if (confirm.equals("y")) {
                FileDatabaseManager.deleteFile(selectedFile, type);
                System.out.println("Fichier supprimé avec succès.");
            }
        }
    }
}
