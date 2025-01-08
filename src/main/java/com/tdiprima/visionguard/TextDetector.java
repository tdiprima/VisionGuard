package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * This is the "blueprint" for all detector implementations.
 * 
 * @author tdiprima
 */
public interface TextDetector {

    // Represents a detected region of text
    class TextRegion {
        public int x, y, width, height;
        public String text;

        public TextRegion(int x, int y, int width, int height, String text) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
        }
    }

    // Encapsulates detection results
    class DetectionResult {
        public BufferedImage modifiedImage;
        public List<TextRegion> regions;

        public DetectionResult(BufferedImage modifiedImage, List<TextRegion> regions) {
            this.modifiedImage = modifiedImage;
            this.regions = regions;
        }
    }

    // Process an image and return detection results
    DetectionResult detect(BufferedImage image, Object metadata);

    // Configure the detector with parameters
    void setupParameters(String... params);

    // Apply the specified action to detected text
    enum Action {
        OUTLINE,
        MASK,
        MOVE_TO_FOLDER
    }
    void applyAction(Action action, DetectionResult result, String outputPath);
}

