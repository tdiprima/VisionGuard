package com.tdiprima.visionguard;

import com.tdiprima.visionguard.TextDetector.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.ServiceLoader;

public class VisionGuard {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java VisionGuard <imagePath> <action> <outputPath> <reportPath>");
            System.out.println("Actions: OUTLINE, MASK, MOVE_TO_FOLDER, QUARANTINE");
            return;
        }

        String imagePath = args[0];
        String originalFileName = new File(imagePath).getName(); // Extract the original file name
        String actionStr = args[1].toUpperCase();
        String outputPath = args[2];
        String reportPath = args[3];

        TextDetector.Action action;
        try {
            action = TextDetector.Action.valueOf(actionStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid action. Use OUTLINE, MASK, or MOVE_TO_FOLDER.");
            return;
        }

        // Load configuration from CLI arguments
        DetectorConfig config = DetectorConfig.fromArgs(args);

        // Load the image
        BufferedImage image;
        try {
            InputStream input = VisionGuard.class.getResourceAsStream(imagePath);
            image = ImageIO.read(input);
            // image = ImageIO.read(new File(imagePath));
            if (image == null) {
                throw new IOException("Failed to load image");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Initialize detectors
        ServiceLoader<TextDetector> loader = ServiceLoader.load(TextDetector.class);
        TextDetector tesseractDetector = null;
        TextDetector ollamaDetector = null;

        for (TextDetector detector : loader) {
            if (detector instanceof TesseractTextDetector) {
                tesseractDetector = detector;
            } else if (detector instanceof OllamaTextDetector) {
                ollamaDetector = detector;
            }
        }

        if (tesseractDetector == null || ollamaDetector == null) {
            System.err.println("Failed to load both TesseractTextDetector and OllamaTextDetector.");
            return;
        }

        // Setup detectors
        tesseractDetector.setupParameters("/usr/local/Cellar/tesseract/5.5.0/share/tessdata/", "eng");
        tesseractDetector.setBoundingBoxConstraints(15, 15, 400, 400);
        ollamaDetector.setupParameters("http://localhost:11434/api/generate");

        // Initialize the detector with the configuration
        tesseractDetector.initialize(config);


        // Perform detection
        DetectionResult tesseractResult = tesseractDetector.detect(image, null);

//        DetectionResult ollamaResult = ollamaDetector.detect(image, null);

        // Apply the selected action for Tesseract results
        tesseractDetector.applyAction(action, tesseractResult, outputPath, originalFileName);
        // tesseractDetector.applyAction(action, tesseractResult, outputPath, new File(imagePath).getName());

        // Validate results and generate a discrepancy report
//        DetectorValidator.validate(tesseractResult, ollamaResult, reportPath);
        System.out.println("Processing completed.");
        System.out.println("Output saved to: " + outputPath);
        System.out.println("Discrepancy report saved to: " + reportPath);

//        System.out.println("Action: " + action);
//        System.out.println("Output Path: " + outputPath);
//        System.out.println("Original File Name: " + originalFileName);

    }
}
