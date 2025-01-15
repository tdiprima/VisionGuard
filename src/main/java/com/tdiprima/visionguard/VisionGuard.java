package com.tdiprima.visionguard;

import com.tdiprima.visionguard.TextDetector.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ServiceLoader;
import javax.imageio.ImageIO;

/**
 * This is the main application class that orchestrates the text detection
 * process, integrates multiple detectors, applies specified actions, and
 * generates discrepancy reports based on the results.
 *
 * @author tdiprima
 */
public class VisionGuard {

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("--help")) {
            printUsage();
            System.exit(0);
        }

        if (args.length < 4) {
            printUsage();
            System.exit(1);
        }

        // Parse CLI arguments
        String directoryPath = args[0];
        String actionStr = args[1].toUpperCase();
        String outputPath = args[2];
        String reportPath = args[3];
        boolean enableOllama = parseOptionalFlag(args, "--ollama", false);

        TextDetector.Action action = parseAction(actionStr);
        if (action == null) {
            System.exit(1);
        }

        DetectorConfig config = DetectorConfig.fromArgs(args);

        ensureDirectoryExists(outputPath);
        ensureDirectoryExists(reportPath);

        File[] imageFiles = listImageFiles(directoryPath);
        if (imageFiles == null || imageFiles.length == 0) {
            System.err.println("No image files found in the directory: " + directoryPath);
            System.exit(1);
        }

        // Initialize detectors
        TextDetector tesseractDetector = initializeTesseract(config);
        TextDetector ollamaDetector = enableOllama ? initializeOllama() : null;

        processFiles(imageFiles, tesseractDetector, ollamaDetector, action, outputPath, reportPath);

        System.out.println("All files in the directory have been processed.");
    }

    private static void printUsage() {
        System.out.println("Usage: java VisionGuard <directoryPath> <action> <outputPath> <reportPath>");
        System.out.println("Actions: OUTLINE, MASK, BURN, EXPORT_TO_FOLDER, FLAG_FOR_REVIEW");
        System.out.println("Optional parameters:");
        System.out.println("  --ollama=true/false  Enable or disable OllamaTextDetector (default: false)");
        System.out.println("  --minWidth=X         Minimum width of bounding boxes");
        System.out.println("  --minHeight=Y        Minimum height of bounding boxes");
        System.out.println("  --maxWidth=A         Maximum width of bounding boxes");
        System.out.println("  --maxHeight=B        Maximum height of bounding boxes");
    }

    private static boolean parseOptionalFlag(String[] args, String flag, boolean defaultValue) {
        for (String arg : args) {
            if (arg.startsWith(flag + "=")) {
                return Boolean.parseBoolean(arg.split("=")[1]);
            }
        }
        return defaultValue;
    }

    private static TextDetector.Action parseAction(String actionStr) {
        try {
            return TextDetector.Action.valueOf(actionStr);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid action. Use OUTLINE, MASK, EXPORT_TO_FOLDER, or FLAG_FOR_REVIEW.");
            return null;
        }
    }

    private static File[] listImageFiles(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            System.err.println("The specified path is not a directory: " + directoryPath);
            return null;
        }
        return directory.listFiles((dir, name) -> name.matches(".*\\.(jpg|jpeg|png|bmp|dicom|dcm)$"));
    }

    private static void ensureDirectoryExists(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Created directory: " + path);
            } else {
                System.err.println("Failed to create directory: " + path);
                System.exit(1);
            }
        }
    }

    private static TextDetector initializeTesseract(DetectorConfig config) {
        TextDetector detector = loadDetector(TesseractTextDetector.class);
        if (detector == null) {
            System.err.println("Failed to load TesseractTextDetector.");
            System.exit(1);
        }
        detector.setupParameters("/usr/local/Cellar/tesseract/5.5.0/share/tessdata/", "eng");
        detector.initialize(config);
        detector.setBoundingBoxConstraints(config.minWidth, config.minHeight, config.maxWidth, config.maxHeight);
        return detector;
    }

    private static TextDetector initializeOllama() {
        TextDetector detector = loadDetector(OllamaTextDetector.class);
        if (detector == null) {
            System.err.println("Failed to load OllamaTextDetector.");
            System.exit(1);
        }
        detector.setupParameters("http://localhost:11434/api/generate");
        return detector;
    }

    private static void processFiles(File[] files, TextDetector tesseractDetector, TextDetector ollamaDetector,
            TextDetector.Action action, String outputPath, String reportPath) {
        for (File file : files) {
            System.out.println("Processing file: " + file.getName());

            try {
                BufferedImage image;

                // Detect file type and preprocess DICOM if necessary
                if (file.getName().toLowerCase().endsWith(".dcm") || file.getName().toLowerCase().endsWith(".dicom")) {
                    image = DICOMImageReader.readDICOMAsBufferedImage(file);
                } else {
                    image = ImageIO.read(file);
                }

                if (image == null) {
                    // throw new IOException("Failed to load image. Skipping: " + file.getName());
                    System.out.println("Failed to load image. Skipping: " + file.getName());
                    continue;
                }

                // Run detection and actions
                DetectionResult tesseractResult = tesseractDetector.detect(image);
                if (tesseractResult.regions == null || tesseractResult.regions.isEmpty()) {
                    System.out.println("No valid text detected. Skipping actions for: " + file.getName());
                    continue;
                }
                DetectionResult ollamaResult = ollamaDetector != null ? ollamaDetector.detect(image) : null;

                tesseractDetector.applyAction(action, tesseractResult, outputPath, file.getName());

                if (ollamaDetector != null) {
                    ollamaDetector.applyAction(action, tesseractResult, outputPath, file.getName());
                }

                String individualReportPath = reportPath + "/" + file.getName() + "_report.txt";
                DetectorValidator.validate(tesseractResult, ollamaResult, individualReportPath);

                System.out.println("File processed: " + file.getName());
            } catch (IOException e) {
                System.err.println("Error processing file: " + file.getName() + ". Skipping.");
                e.printStackTrace();
            }
        }
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
