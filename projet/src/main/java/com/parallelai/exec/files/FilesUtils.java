package com.parallelai.exec.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Classe utilitaire pour la gestion des fichiers du projet.
 * Cette classe fournit des méthodes statiques pour manipuler les fichiers
 * et répertoires liés aux modèles d'IA.
 */
public class FilesUtils {
    
    /**
     * Nettoie les répertoires contenant les modèles d'IA.
     * Cette méthode supprime tous les fichiers contenus dans les dossiers
     * des modèles CNN et MLP, tout en préservant les répertoires eux-mêmes.
     *
     * @throws IOException Si une erreur survient lors de la suppression des fichiers
     */
    public static void clearModelDirectories() throws IOException {
        String[] modelPaths = {
            "projet\\src\\main\\ressources\\models\\CNN",
            "projet\\src\\main\\ressources\\models\\MLP"
        };
        
        for (String modelPath : modelPaths) {
            Path path = Path.of(modelPath);
            if (Files.exists(path)) {
                try (Stream<Path> walk = Files.walk(path)) {
                    walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .filter(file -> !file.getPath().equals(path.toString()))
                        .forEach(File::delete);
                }
            }
        }
    }
}
