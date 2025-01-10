package com.parallelai.exec.files;

import java.io.File;
import java.io.IOException;

public class FilesUtils {
    
    public static void clearModelDirectories() throws IOException {
        String[] modelPaths = {
            "projet/src/main/resources/models/CNN",
            "projet/src/main/resources/models/MLP"
        };
        
        for (String modelPath : modelPaths) {
            File directory = new File(modelPath);
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            file.delete();
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            clearModelDirectories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
