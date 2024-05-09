import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class PageRank {
    private static final double DAMPING_FACTOR = 0.85;
    private static final int MAX_ITERATIONS = 100;
    private static final double EPSILON = 1e-10;
    private static final String DIRECTORY_PATH = "D:/InvertedIndex/";

    public static void main(String[] args) throws IOException {
        File dir = new File(DIRECTORY_PATH);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.equals("pagerank_results.txt"));
        if (files == null) {
            System.out.println("No files found in the directory.");
            return;
        }

        String[] fileNames = Arrays.stream(files).map(File::getName).toArray(String[]::new);
        System.out.println("Processing files: " + Arrays.toString(fileNames));

        // Paso 1: Leer el índice invertido de un archivo
        Map<String, Map<String, Integer>> invertedIndex = readInvertedIndexFromFile(DIRECTORY_PATH + "inverted_index.txt");

        // Determinar los documentos relacionados basándose en un enlace más complejo
        Map<String, List<String>> fileRelations = linkDocumentsBasedOnComplexCriteria(invertedIndex, fileNames);

        // Construir la matriz de adyacencia basada en las relaciones de enlace entre archivos
        double[][] adjMatrix = buildAdjacencyMatrix(fileNames, fileRelations);

        // Calcular el PageRank para los archivos
        double[] filePageRank = calculatePageRank(adjMatrix);

        // Escribir los resultados de PageRank en un archivo de texto
        writePageRankToFile(filePageRank, fileNames, DIRECTORY_PATH + "pagerank_results.txt");
    }

    private static Map<String, Map<String, Integer>> readInvertedIndexFromFile(String fileName) throws IOException {
        Map<String, Map<String, Integer>> invertedIndex = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                String word = parts[0].trim();
                Map<String, Integer> documentCounts = Arrays.stream(parts[1].trim().split(","))
                    .map(s -> s.trim().split("="))
                    .collect(Collectors.toMap(
                        s -> s[0],
                        s -> Integer.parseInt(s[1]),
                        (a, b) -> b,
                        HashMap::new));
                invertedIndex.put(word, documentCounts);
            }
        }
        return invertedIndex;
    }

    private static Map<String, List<String>> linkDocumentsBasedOnComplexCriteria(Map<String, Map<String, Integer>> invertedIndex, String[] fileNames) {
        Map<String, List<String>> fileRelations = new HashMap<>();
        for (String fileName : fileNames) {
            fileRelations.put(fileName, new ArrayList<>());
            Map<String, Integer> fileWordCounts = invertedIndex.entrySet().stream()
                .filter(e -> e.getValue().containsKey(fileName))
                .collect(Collectors.toMap(
                    e -> e.getKey(),
                    e -> e.getValue().get(fileName),
                    (a, b) -> b));

            for (String word : fileWordCounts.keySet()) {
                for (String otherFile : invertedIndex.get(word).keySet()) {
                    if (!otherFile.equals(fileName) && !fileRelations.get(fileName).contains(otherFile)) {
                        fileRelations.get(fileName).add(otherFile);
                    }
                }
            }
        }
        return fileRelations;
    }

    private static double[][] buildAdjacencyMatrix(String[] fileNames, Map<String, List<String>> fileRelations) {
        int n = fileNames.length;
        double[][] matrix = new double[n][n];
        Map<String, Integer> fileNameToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            fileNameToIndex.put(fileNames[i], i);
        }

        for (Map.Entry<String, List<String>> entry : fileRelations.entrySet()) {
            int fromIndex = fileNameToIndex.get(entry.getKey());
            List<String> toFiles = entry.getValue();
            for (String toFile : toFiles) {
                int toIndex = fileNameToIndex.get(toFile);
                matrix[fromIndex][toIndex] = 1.0 / toFiles.size();
            }
        }
        return matrix;
    }

    private static double[] calculatePageRank(double[][] adjMatrix) {
        int n = adjMatrix.length;
        double[] pr = new double[n];
        Arrays.fill(pr, 1.0 / n);
        double[] newPr = new double[n];
        double diff;
        int iterations = MAX_ITERATIONS;

        do {
            diff = 0.0;
            for (int i = 0; i < n; i++) {
                newPr[i] = (1 - DAMPING_FACTOR) / n;
                for (int j = 0; j < n; j++) {
                    newPr[i] += DAMPING_FACTOR * adjMatrix[j][i] * pr[j];
                }
                diff += Math.abs(newPr[i] - pr[i]);
            }
            System.arraycopy(newPr, 0, pr, 0, n);
        } while (diff > EPSILON && --iterations > 0);

        return pr;
    }

    private static void writePageRankToFile(double[] pageRank, String[] fileNames, String fileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (int i = 0; i < pageRank.length; i++) {
                writer.write(fileNames[i] + ": " + pageRank[i]);
                writer.newLine();
            }
        }
    }
}
