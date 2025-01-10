package com.parallelai.exec.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class FilesUtils {
    
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
