package com.tdiprima.visionguard;

import com.tdiprima.visionguard.TextDetector.DetectionResult;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.ServiceLoader;

public class VisionGuard {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java VisionGuard <imagePath> <action> <outputPath> <reportPath>");
            System.out.println("Actions: OUTLINE, MASK, MOVE_TO_FOLDER");
            return;
        }

        String imagePath = args[0];
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

        // Load the image
        BufferedImage image;
        try {
            InputStream input = VisionGuard.class.getResourceAsStream(imagePath);
            image = ImageIO.read(input);
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
        ollamaDetector.setupParameters("http://localhost:11434/api/generate");

        // Perform detection
        DetectionResult tesseractResult = tesseractDetector.detect(image, null);
        DetectionResult ollamaResult = ollamaDetector.detect(image, null);

        // Apply the selected action for Tesseract results
        tesseractDetector.applyAction(action, tesseractResult, outputPath);

        // Validate results and generate a discrepancy report
//        DetectorValidator.validate(tesseractResult, ollamaResult, reportPath);

        System.out.println("Processing completed.");
        System.out.println("Output saved to: " + outputPath);
//        System.out.println("Discrepancy report saved to: " + reportPath);
    }
}
