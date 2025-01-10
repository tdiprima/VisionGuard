package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * This is the "blueprint" for all detector implementations.
 *
 * @author tdiprima
 */
public interface TextDetector {
    
    // Default bounding box constraints
    int DEFAULT_MIN_WIDTH = 10;
    int DEFAULT_MIN_HEIGHT = 10;
    int DEFAULT_MAX_WIDTH = 500;
    int DEFAULT_MAX_HEIGHT = 500;

    // Configurable bounding box constraints
    void setBoundingBoxConstraints(int minWidth, int minHeight, int maxWidth, int maxHeight);

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
    public class DetectionResult {

        public BufferedImage modifiedImage;
        public List<TextRegion> regions;
        public String rawResponse; // New field

        public DetectionResult(BufferedImage modifiedImage, List<TextRegion> regions) {
            this.modifiedImage = modifiedImage;
            this.regions = regions;
        }

        public DetectionResult(BufferedImage modifiedImage, String rawResponse) {
            this.modifiedImage = modifiedImage;
            this.rawResponse = rawResponse;
        }
    }

    // Process an image and return detection results
    DetectionResult detect(BufferedImage image, Object metadata);

    // Configure the detector with parameters
    void setupParameters(String... params);

    // Apply the specified action to detected text
    public enum Action {
        OUTLINE,
        MASK,
        MOVE_TO_FOLDER,
        QUARANTINE
    }

    void applyAction(Action action, DetectionResult result, String outputPath, String originalFileName);
}
