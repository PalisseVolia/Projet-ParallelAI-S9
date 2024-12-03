package com.projet.utils.traitementdedonnee;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class CSVUtils {
    /**
     * Lire un fichier CSV
     * @param filePath Chemin du fichier
     * @return Liste de tableau de String représentant chaque ligne
     * @throws IOException En cas d'erreur de lecture
     */

    public static List<String[]> readCSVFile(String filePath) throws IOException {
        List<String[]> records = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(values);
            }
        }
        
        return records;
    }

    /**
     * Écrire un fichier CSV
     * @param data Données à écrire
     * @param filePath Chemin du fichier de sortie
     * @throws IOException En cas d'erreur d'écriture
     */
    public static void writeCSVFile(List<String[]> data, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            for (String[] row : data) {
                writer.write(String.join(",", row));
                writer.newLine();
            }
        }
    }

    /**
     * Agréger les lignes avec moyenne sur la dernière colonne
     * @param data Données d'entrée
     * @return Données agrégées
     */
    public static List<String[]> aggregateCSVByColumns(List<String[]> data) {
        Map<String, List<Double>> groupedRows = new HashMap<>();
        
        for (String[] row : data) {
            // Créer une clé avec les colonnes sauf la dernière
            StringBuilder key = new StringBuilder();
            for (int i = 0; i < row.length - 1; i++) {
                key.append(row[i]).append(",");
            }
            
            // Récupérer la valeur de la dernière colonne
            double lastValue = Double.parseDouble(row[row.length - 1].trim());
            
            // Ajouter à la map
            groupedRows.computeIfAbsent(key.toString(), k -> new ArrayList<>())
                      .add(lastValue);
        }
        
        // Créer la liste finale avec les moyennes
        List<String[]> result = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : groupedRows.entrySet()) {
            // Calculer la moyenne de la dernière colonne
            double average = entry.getValue().stream()
                                .mapToDouble(Double::doubleValue)
                                .average()
                                .orElse(0.0);
            
            // Recréer la ligne avec la moyenne
            String[] originalValues = entry.getKey().split(",");
            String[] newRow = new String[originalValues.length + 1];
            System.arraycopy(originalValues, 0, newRow, 0, originalValues.length);
            newRow[newRow.length - 1] = String.format("%.2f", average);
            
            result.add(newRow);
        }
        
        return result;
    }

    /**
     * Filtrer les lignes d'un CSV selon un critère
     * @param data Données d'entrée
     * @param columnIndex Index de la colonne à filtrer
     * @param value Valeur de filtrage
     * @return Lignes filtrées
     */
    public static List<String[]> filterCSV(List<String[]> data, int columnIndex, String value) {
        List<String[]> filteredData = new ArrayList<>();
        
        for (String[] row : data) {
            if (row[columnIndex].equals(value)) {
                filteredData.add(row);
            }
        }
        
        return filteredData;
    }

    /**
     * Trier un CSV selon une colonne
     * @param data Données d'entrée
     * @param columnIndex Index de la colonne de tri
     * @param ascending Ordre de tri (croissant/décroissant)
     * @return Données triées
     */
    public static List<String[]> sortCSV(List<String[]> data, int columnIndex, boolean ascending) {
        List<String[]> sortedData = new ArrayList<>(data);
        
        sortedData.sort((row1, row2) -> {
            int compareResult = row1[columnIndex].compareTo(row2[columnIndex]);
            return ascending ? compareResult : -compareResult;
        });
        
        return sortedData;
    }

    /**
     * Convertir une colonne en nombres
     * @param data Données d'entrée
     * @param columnIndex Index de la colonne à convertir
     * @return Liste de nombres
     */
    
    public static List<Double> convertColumnToNumbers(List<String[]> data, int columnIndex) {
        List<Double> numbers = new ArrayList<>();
        
        for (String[] row : data) {
            try {
                numbers.add(Double.parseDouble(row[columnIndex].trim()));
            } catch (NumberFormatException e) {
                // Ignorer les lignes qui ne peuvent pas être converties
                System.err.println("Impossible de convertir : " + row[columnIndex]);
            }
        }
        
        return numbers;
    }

}
