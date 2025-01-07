package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * This is the "blueprint" for all detector implementations.
 * 
 * @author tdiprima
 */
public interface TextDetector {
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

    // Process the BufferedImage and DICOM metadata
    DetectionResult detect(BufferedImage image, Object dicomMetadata);

    // Setup parameters specific to this detector
    void setupParameters(String... params);

    // Encapsulate detection results
    class DetectionResult {
        public BufferedImage modifiedImage;
        public List<TextRegion> regions;

        public DetectionResult(BufferedImage modifiedImage, List<TextRegion> regions) {
            this.modifiedImage = modifiedImage;
            this.regions = regions;
        }
    }
}
