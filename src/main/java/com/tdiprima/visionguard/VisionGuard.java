package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ServiceLoader;

/**
 * Load and use the SPI
 *
 * @author tdiprima
 */
public class VisionGuard {

    public static void main(String[] args) {
        // Load the SPI
        ServiceLoader<TextDetector> loader = ServiceLoader.load(TextDetector.class);
        TextDetector detector = loader.iterator().next();

        // Setup the detector (Tesseract data path)
        detector.setupParameters("/usr/local/Cellar/tesseract/5.5.0/share/tessdata/", "eng");

        // Load an image
        BufferedImage image;
        try {
            image = ImageIO.read(VisionGuard.class.getResourceAsStream("/images/example.png"));
            if (image == null) {
                throw new IOException("Failed to load image resource");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Perform detection
        TextDetector.DetectionResult result = detector.detect(image, null);

        // Process results
        System.out.println("Detected Text Regions:");
        for (TextDetector.TextRegion region : result.regions) {
            System.out.println("Text: " + region.text);
            System.out.println("Bounding Box: [" + region.x + ", " + region.y + ", "
                    + region.width + ", " + region.height + "]");
        }

        // Save modified image
        try {
            ImageIO.write(result.modifiedImage, "png", new File("output.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
