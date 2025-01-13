package com.tdiprima.visionguard;

import com.tdiprima.visionguard.TextDetector.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ServiceLoader;

/**
 * This is is the main application class that orchestrates the text detection process, 
 * integrates multiple detectors, applies specified actions, and generates discrepancy 
 * reports based on the results.
 * 
 * @author tdiprima
 */
public class VisionGuard {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java VisionGuard <imagePath> <action> <outputPath> <reportPath>");
            System.out.println("Actions: OUTLINE, MASK, MOVE_TO_FOLDER, QUARANTINE");
            System.out.println("Optional parameters:");
            System.out.println("  --minWidth=X         Minimum width of bounding boxes");
            System.out.println("  --minHeight=Y        Minimum height of bounding boxes");
            System.out.println("  --maxWidth=A         Maximum width of bounding boxes");
            System.out.println("  --maxHeight=B        Maximum height of bounding boxes");
            System.out.println("  --quarantinePath=path   Specify quarantine folder (action=QUARANTINE)");
            System.out.println("  --moveToFolderPath=path Specify move-to-folder path (action=MOVE_TO_FOLDER)");
            System.exit(1);
        }

        // Parse CLI arguments
        String imagePath = args[0];
        String originalFileName = new File(imagePath).getName(); // Extract the original file name
        String actionStr = args[1].toUpperCase();
        String outputPath = args[2];
        String reportPath = args[3];

        TextDetector.Action action;
        try {
            action = TextDetector.Action.valueOf(actionStr);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid action. Use OUTLINE, MASK, MOVE_TO_FOLDER, or QUARANTINE.");
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

        // Load the image
        BufferedImage image;
        try {
            image = ImageIO.read(new File(imagePath));
            if (image == null) {
                throw new IOException("Failed to load image. Please check if the file exists and is a valid image format.");
            }
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
            e.printStackTrace();
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

        // Perform detection
        DetectionResult tesseractResult = tesseractDetector.detect(image, null);
        DetectionResult ollamaResult = ollamaDetector.detect(image, null);

        // Apply the selected action for Tesseract results
        tesseractDetector.applyAction(action, tesseractResult, outputPath, originalFileName);

        // Validate results and generate a discrepancy report
        DetectorValidator.validate(tesseractResult, ollamaResult, reportPath);
        System.out.println("Processing completed.");
        System.out.println("Output saved to: " + outputPath);
        System.out.println("Discrepancy report saved to: " + reportPath);
        System.exit(0);
    }

    private static boolean isPathActionConflict(TextDetector.Action action, DetectorConfig config) {
        return (action == TextDetector.Action.QUARANTINE && config.moveToFolderPath != null) ||
               (action == TextDetector.Action.MOVE_TO_FOLDER && config.quarantinePath != null);
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
