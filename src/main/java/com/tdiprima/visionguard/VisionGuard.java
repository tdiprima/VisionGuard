package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.ServiceLoader;

/**
 * Load and use the SPI
 *
 * @author tdiprima
 */
public class VisionGuard {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java VisionGuard <imagePath> <action> <outputPath>");
            System.out.println("Actions: OUTLINE, MASK, MOVE_TO_FOLDER");
            return;
        }
        

        System.out.println("*********");
        System.out.println("DYLD_LIBRARY_PATH: " + System.getenv("DYLD_LIBRARY_PATH"));
        System.out.println("java.library.path: " + System.getProperty("java.library.path"));
        System.out.println("*********");

        String imagePath = args[0];
        String actionStr = args[1].toUpperCase();
        String outputPath = args[2];

        TextDetector.Action action;
        try {
            action = TextDetector.Action.valueOf(actionStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid action. Use OUTLINE, MASK, or MOVE_TO_FOLDER.");
            return;
        }

        // Load the SPI
        ServiceLoader<TextDetector> loader = ServiceLoader.load(TextDetector.class);
        TextDetector detector = loader.iterator().next();

        // Setup the detector (Tesseract data path and language)
        detector.setupParameters("/usr/local/Cellar/tesseract/5.5.0/share/tessdata/", "eng");

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

        // Perform detection
        TextDetector.DetectionResult result = detector.detect(image, null);

        // Apply the selected action
        detector.applyAction(action, result, outputPath);

        System.out.println("Processing completed. Output saved to: " + outputPath);
    }
}
