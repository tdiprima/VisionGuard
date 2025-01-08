package com.tdiprima.visionguard;

import net.sourceforge.tess4j.Tesseract;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation using Tesseract
 *
 * @author tdiprima
 */
public class TesseractTextDetector implements TextDetector {

    private Tesseract tesseract;

    @Override
    public void setupParameters(String... params) {
        tesseract = new Tesseract();
        tesseract.setDatapath(params[0]); // Path to Tesseract data
        if (params.length > 1) {
            tesseract.setLanguage(params[1]); // Language (e.g., "eng")
        }
    }

    @Override
    public DetectionResult detect(BufferedImage image, Object dicomMetadata) {
        List<TextRegion> regions = new ArrayList<>();
        BufferedImage modifiedImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = modifiedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        try {
            // Set Tesseract page segmentation mode to single block of text
            tesseract.setPageSegMode(1); // PSM_SINGLE_BLOCK

            // Perform OCR and get word-level bounding boxes
            var result = tesseract.getWords(image, 1); // Level 1: WORD bounding boxes
            for (var word : result) {
                int x = word.getBoundingBox().x;
                int y = word.getBoundingBox().y;
                int width = word.getBoundingBox().width;
                int height = word.getBoundingBox().height;

                // Add text region to the list
                regions.add(new TextRegion(x, y, width, height, word.getText()));

                // Draw the red box over the detected text
                g2d.setColor(new Color(255, 0, 0, 128));
                g2d.fillRect(x, y, width, height);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        g2d.dispose();
        return new DetectionResult(modifiedImage, regions);
    }

    @Override
    public void applyAction(Action action, DetectionResult result, String outputPath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
