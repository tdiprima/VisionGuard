package com.tdiprima.visionguard;

import com.tdiprima.visionguard.TextDetector.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ServiceLoader;
import javax.imageio.ImageIO;

/**
 * This is is the main application class that orchestrates the text detection process, 
 * integrates multiple detectors, applies specified actions, and generates discrepancy 
 * reports based on the results.
 * 
 * Updated to process all images in a directory instead of a single image.
 * 
 * @author tdiprima
 */
public class VisionGuard {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java VisionGuard <directoryPath> <action> <outputPath> <reportPath>");
            System.out.println("Actions: OUTLINE, MASK, EXPORT_TO_FOLDER, FLAG_FOR_REVIEW");
            System.out.println("Optional parameters:");
            System.out.println("  --minWidth=X         Minimum width of bounding boxes");
            System.out.println("  --minHeight=Y        Minimum height of bounding boxes");
            System.out.println("  --maxWidth=A         Maximum width of bounding boxes");
            System.out.println("  --maxHeight=B        Maximum height of bounding boxes");
            System.out.println("  --quarantinePath=path   Specify quarantine folder (action=FLAG_FOR_REVIEW)");
            System.out.println("  --moveToFolderPath=path Specify move-to-folder path (action=EXPORT_TO_FOLDER)");
            System.exit(1);
        }

        // Parse CLI arguments
        String directoryPath = args[0];
        String actionStr = args[1].toUpperCase();
        String outputPath = args[2];
        String reportPath = args[3];

        TextDetector.Action action;
        try {
            action = TextDetector.Action.valueOf(actionStr);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid action. Use OUTLINE, MASK, EXPORT_TO_FOLDER, or FLAG_FOR_REVIEW.");
            System.exit(1);
            return;
        }

        // Load configuration from CLI arguments
        DetectorConfig config = DetectorConfig.fromArgs(args);

        // Validate mutual exclusivity of paths and actions
        if (isPathActionConflict(action, config)) {
            System.err.println("Error: Specified paths conflict with the selected action.");
            System.exit(1);
            return;
        }

        // Initialize detectors
        TextDetector tesseractDetector = loadDetector(TesseractTextDetector.class);
        TextDetector ollamaDetector = loadDetector(OllamaTextDetector.class);

        if (tesseractDetector == null || ollamaDetector == null) {
            System.err.println("Failed to load both TesseractTextDetector and OllamaTextDetector.");
            System.exit(1);
            return;
        }

        // Setup detectors
        tesseractDetector.setupParameters("/usr/local/Cellar/tesseract/5.5.0/share/tessdata/", "eng");
        tesseractDetector.setBoundingBoxConstraints(config.minWidth, config.minHeight, config.maxWidth, config.maxHeight);
        ollamaDetector.setupParameters("http://localhost:11434/api/generate");

        // Initialize the detector with the configuration
        tesseractDetector.initialize(config);

        // Process all files in the directory
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            System.err.println("The specified path is not a directory: " + directoryPath);
            System.exit(1);
            return;
        }

        File[] files = directory.listFiles((dir, name) -> name.matches(".*\\.(jpg|jpeg|png|bmp)$"));
        if (files == null || files.length == 0) {
            System.err.println("No image files found in the directory: " + directoryPath);
            System.exit(1);
            return;
        }

        for (File file : files) {
            System.out.println("Processing file: " + file.getName());
            try {
                BufferedImage image = ImageIO.read(file);
                if (image == null) {
                    throw new IOException("Failed to load image. Skipping: " + file.getName());
                }

                // Perform detection
                DetectionResult tesseractResult = tesseractDetector.detect(image, null);
                DetectionResult ollamaResult = ollamaDetector.detect(image, null);

                // Apply the selected action for Tesseract results
                tesseractDetector.applyAction(action, tesseractResult, outputPath, file.getName());

                // Validate results and generate a discrepancy report
                String individualReportPath = reportPath + "/" + file.getName() + "_report.txt";
                DetectorValidator.validate(tesseractResult, ollamaResult, individualReportPath);

                System.out.println("File processed: " + file.getName());
            } catch (IOException e) {
                System.err.println("Error processing file: " + file.getName() + ". Skipping.");
                e.printStackTrace();
            }
        }

        System.out.println("All files in the directory have been processed.");
        System.exit(0);
    }

    private static boolean isPathActionConflict(TextDetector.Action action, DetectorConfig config) {
        return (action == TextDetector.Action.FLAG_FOR_REVIEW && config.moveToFolderPath != null) ||
               (action == TextDetector.Action.EXPORT_TO_FOLDER && config.quarantinePath != null);
    }

    private static <T extends TextDetector> T loadDetector(Class<T> detectorClass) {
        return ServiceLoader.load(TextDetector.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(detectorClass::isInstance)
                .map(detectorClass::cast)
                .findFirst()
                .orElse(null);
    }
}

