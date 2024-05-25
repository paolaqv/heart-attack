package com.example.wekaapp.controller;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class WekaController {

    private File uploadedFile;
    private final String outputDir = System.getProperty("java.io.tmpdir");
    private final String staticDir = "src/main/resources/static/images";
    private String currentArbolPngFile;
    private String currentRedNeuronalPngFile;
    private String currentRedNeuronalTxtFile;
    private String currentClusteringPngFile;
    private String currentClusteringTxtFile;

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        try {
            uploadedFile = new File(outputDir + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename());
            file.transferTo(uploadedFile);
            model.addAttribute("message", "Archivo subido exitosamente.");
            model.addAttribute("fileName", file.getOriginalFilename());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error al subir el archivo: " + e.getMessage());
        }
        return "index";
    }

    @GetMapping("/result")
    public String result(Model model) {
        if (uploadedFile == null) {
            model.addAttribute("error", "No se ha subido ningún archivo.");
            return "result";
        }

        try {
            String datasetPath = uploadedFile.getPath();
            String arbolDotFile = outputDir + "/arbol_" + UUID.randomUUID() + ".dot";
            currentArbolPngFile = staticDir + "/arbol_" + UUID.randomUUID() + ".png";

            generarArbol(datasetPath, arbolDotFile, currentArbolPngFile);
            model.addAttribute("imagePath", "/" + currentArbolPngFile);

        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Error al procesar el archivo: " + e.getMessage());
        }

        return "result";
    }

    @GetMapping("/resultRed")
    public String resultRed(Model model) {
        if (uploadedFile == null) {
            model.addAttribute("error", "No se ha subido ningún archivo.");
            return "resultRed";
        }

        try {
            String datasetPath = uploadedFile.getPath();
            String redNeuronalDotFile = outputDir + "/red_neuronal_" + UUID.randomUUID() + ".dot";
            currentRedNeuronalPngFile = staticDir + "/red_neuronal_" + UUID.randomUUID() + ".png";
            currentRedNeuronalTxtFile = outputDir + "/red_neuronal_" + UUID.randomUUID() + ".txt";

            generarRedNeuronal(datasetPath, redNeuronalDotFile, currentRedNeuronalPngFile);
            obtenerEstructuraRedNeuronal(datasetPath, currentRedNeuronalTxtFile);

            model.addAttribute("imagePath", "/" + currentRedNeuronalPngFile);
            model.addAttribute("fileContent", new String(Files.readAllBytes(Paths.get(currentRedNeuronalTxtFile))));

        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Error al procesar el archivo: " + e.getMessage());
        }

        return "resultRed";
    }

    @GetMapping("/resultCluster")
    public String resultCluster(Model model) {
        if (uploadedFile == null) {
            model.addAttribute("error", "No se ha subido ningún archivo.");
            return "resultCluster";
        }

        try {
            String datasetPath = uploadedFile.getPath();
            currentClusteringTxtFile = outputDir + "/clustering_" + UUID.randomUUID() + ".txt";
            currentClusteringPngFile = staticDir + "/clustering_" + UUID.randomUUID() + ".png";

            ejecutarKMeans(datasetPath, currentClusteringTxtFile, currentClusteringPngFile);

            model.addAttribute("imagePath", "/" + currentClusteringPngFile);
            model.addAttribute("fileContent", new String(Files.readAllBytes(Paths.get(currentClusteringTxtFile))));

        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Error al procesar el archivo: " + e.getMessage());
        }

        return "resultCluster";
    }

    @GetMapping("/generateReport")
    public String generateReport(@RequestParam("type") String type, Model model) {
        try {
            String userHome = System.getProperty("user.home");
            String pdfOutputPath = userHome + "/Downloads/report-" + UUID.randomUUID() + ".pdf";
            try (PdfWriter writer = new PdfWriter(pdfOutputPath)) {
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                switch (type) {
                    case "result":
                        generateResultPdf(document);
                        break;
                    case "resultRed":
                        generateResultRedPdf(document);
                        break;
                    case "resultCluster":
                        generateResultClusterPdf(document);
                        break;
                }

                document.close();
                System.out.println("PDF generado en: " + pdfOutputPath);
            }

            model.addAttribute("message", "Reporte generado exitosamente.");
            model.addAttribute("pdfPath", pdfOutputPath);

        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("error", "Error al generar el reporte: " + e.getMessage());
        }

        return "index";
    }

    private void generateResultPdf(Document document) throws IOException {
        if (currentArbolPngFile != null && new File(currentArbolPngFile).exists()) {
            ImageData data = ImageDataFactory.create(currentArbolPngFile);
            Image img = new Image(data);
            document.add(new Paragraph("Decision Tree Result"));
            document.add(img);
        } else {
            document.add(new Paragraph("Imagen no encontrada: " + currentArbolPngFile));
        }
    }

    private void generateResultRedPdf(Document document) throws IOException {
        if (currentRedNeuronalPngFile != null && new File(currentRedNeuronalPngFile).exists()) {
            ImageData data = ImageDataFactory.create(currentRedNeuronalPngFile);
            Image img = new Image(data);
            document.add(new Paragraph("Neural Network Result"));
            document.add(img);
            if (currentRedNeuronalTxtFile != null && new File(currentRedNeuronalTxtFile).exists()) {
                String content = new String(Files.readAllBytes(Paths.get(currentRedNeuronalTxtFile)));
                document.add(new Paragraph(content));
            } else {
                document.add(new Paragraph("Archivo de texto no encontrado: " + currentRedNeuronalTxtFile));
            }
        } else {
            document.add(new Paragraph("Imagen no encontrada: " + currentRedNeuronalPngFile));
        }
    }

    private void generateResultClusterPdf(Document document) throws IOException {
        if (currentClusteringPngFile != null && new File(currentClusteringPngFile).exists()) {
            ImageData data = ImageDataFactory.create(currentClusteringPngFile);
            Image img = new Image(data);
            document.add(new Paragraph("Clustering Result"));
            document.add(img);
            if (currentClusteringTxtFile != null && new File(currentClusteringTxtFile).exists()) {
                String content = new String(Files.readAllBytes(Paths.get(currentClusteringTxtFile)));
                document.add(new Paragraph(content));
            } else {
                document.add(new Paragraph("Archivo de texto no encontrado: " + currentClusteringTxtFile));
            }
        } else {
            document.add(new Paragraph("Imagen no encontrada: " + currentClusteringPngFile));
        }
    }

    private void generarArbol(String datasetPath, String outputDotFile, String outputPngFile) throws IOException {
        try {
            CSVLoader loader = new CSVLoader();
            loader.setSource(new File(datasetPath));
            Instances data = loader.getDataSet();
            data.setClassIndex(data.numAttributes() - 1);

            weka.classifiers.trees.J48 tree = new weka.classifiers.trees.J48();
            tree.buildClassifier(data);

            FileWriter writer = new FileWriter(outputDotFile);
            writer.write(tree.graph());
            writer.close();

            runCommand("dot -Tpng " + outputDotFile + " -o " + outputPngFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generarRedNeuronal(String datasetPath, String outputDotFile, String outputPngFile) throws IOException {
        try {
            int[] topologia = {4, 3, 2};
            FileWriter writer = new FileWriter(outputDotFile);
            writer.write("digraph G {\n");

            for (int i = 0; i < topologia.length - 1; i++) {
                for (int j = 0; j < topologia[i]; j++) {
                    for (int k = 0; k < topologia[i + 1]; k++) {
                        writer.write(String.format("  %d -> %d;\n", i * topologia[i] + j, (i + 1) * topologia[i + 1] + k));
                    }
                }
            }

            writer.write("}\n");
            writer.close();

            runCommand("dot -Tpng " + outputDotFile + " -o " + outputPngFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void obtenerEstructuraRedNeuronal(String datasetPath, String outputTxtFile) throws IOException {
        try {
            CSVLoader loader = new CSVLoader();
            loader.setSource(new File(datasetPath));
            Instances data = loader.getDataSet();
            data.setClassIndex(data.numAttributes() - 1);

            MultilayerPerceptron mlp = new MultilayerPerceptron();
            mlp.buildClassifier(data);

            String estructura = mlp.toString();
            FileWriter writer = new FileWriter(outputTxtFile);
            writer.write(estructura);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ejecutarKMeans(String datasetPath, String outputTxtFile, String outputPngFile) throws IOException {
        try {
            CSVLoader loader = new CSVLoader();
            loader.setSource(new File(datasetPath));
            Instances data = loader.getDataSet();

            SimpleKMeans kmeans = new SimpleKMeans();
            kmeans.setNumClusters(3);
            kmeans.buildClusterer(data);

            ClusterEvaluation eval = new ClusterEvaluation();
            eval.setClusterer(kmeans);
            eval.evaluateClusterer(data);

            // Guardar las asignaciones de los clusters en un archivo de texto
            FileWriter txtWriter = new FileWriter(outputTxtFile);
            txtWriter.write(eval.clusterResultsToString());
            txtWriter.close();

            // Crear el archivo .dot para el gráfico de dispersión
            FileWriter dotWriter = new FileWriter(outputTxtFile.replace(".txt", ".dot"));
            dotWriter.write("graph clustering {\n");
            double[] assignments = eval.getClusterAssignments();
            for (int i = 0; i < data.numInstances(); i++) {
                int clusterIndex = (int) assignments[i];
                double[] values = data.instance(i).toDoubleArray();
                dotWriter.write(String.format("  \"%d\" [pos=\"%f,%f\"];\n", i, values[0], values[1]));
                dotWriter.write(String.format("  \"%d\" -- \"%d\" [color=cluster%d];\n", i, clusterIndex, clusterIndex));
            }
            dotWriter.write("}\n");
            dotWriter.close();

            // Ejecutar Graphviz para convertir el archivo .dot en una imagen del gráfico de dispersión
            runCommand("dot -Tpng " + outputTxtFile.replace(".txt", ".dot") + " -o " + outputPngFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        process.waitFor();
    }
}
