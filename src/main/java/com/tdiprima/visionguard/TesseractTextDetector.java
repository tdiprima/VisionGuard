package com.tdiprima.visionguard;

import net.sourceforge.tess4j.Tesseract;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import java.util.logging.Logger;

/**
 * Concrete implementation using Tesseract
 *
 * @author tdiprima
 */
public class TesseractTextDetector implements TextDetector {

    private Tesseract tesseract;
    private static final Logger logger = Logger.getLogger(TesseractTextDetector.class.getName());

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return the original image along with the detected regions
        return new DetectionResult(image, regions);
    }

    @Override
    public void applyAction(Action action, DetectionResult result, String outputPath, String originalFileName) {
        switch (action) {
            case OUTLINE:
                BufferedImage outlinedImage = outlineTextRegions(result.modifiedImage, result.regions);
                saveImage(outlinedImage, outputPath, originalFileName);
                break;

            case MASK:
                BufferedImage maskedImage = maskTextRegions(result.modifiedImage, result.regions);
                saveImage(maskedImage, outputPath, originalFileName);
                break;

            case MOVE_TO_FOLDER:
                moveImageToFolder(result.modifiedImage, outputPath, originalFileName);
                break;

            case QUARANTINE:
                String quarantinePath = outputPath + File.separator + "quarantine";
                System.out.println("Quarantine action triggered. Path: " + quarantinePath);
                moveImageToFolder(result.modifiedImage, quarantinePath, originalFileName);
                break;

            default:
                throw new UnsupportedOperationException("Action not supported: " + action);
        }
    }

    // Utility to draw bounding boxes on the image
    private BufferedImage outlineTextRegions(BufferedImage image, List<TextRegion> regions) {
        BufferedImage outlinedImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = outlinedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        g2d.setColor(new Color(255, 0, 0, 128)); // Red semi-transparent
        for (TextRegion region : regions) {
            g2d.drawRect(region.x, region.y, region.width, region.height);
        }

        g2d.dispose();
        return outlinedImage;
    }

    // Utility to save an image to disk
    private void saveImage(BufferedImage image, String outputPath, String originalFileName) {
        String baseName = originalFileName.replaceAll("\\.\\w+$", ""); // Strip extension
        String fileName = baseName + "_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(outputPath, fileName);

        System.out.println("Attempting to save image: " + outputFile.getAbsolutePath());

        try {
            ImageIO.write(image, "png", outputFile);
            System.out.println("Image saved successfully to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Utility to mask text regions
    private BufferedImage maskTextRegions(BufferedImage image, List<TextRegion> regions) {
        BufferedImage maskedImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = maskedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        g2d.setColor(new Color(0, 0, 0, 255)); // Black mask
        // g2d.setColor(new Color(255, 0, 0, 128)); // Red semi-transparent
        for (TextRegion region : regions) {
            g2d.fillRect(region.x, region.y, region.width, region.height);
        }

        g2d.dispose();
        return maskedImage;
    }

    // Utility to move an image to a specific folder
    private void moveImageToFolder(BufferedImage image, String outputFolderPath, String originalFileName) {
        File folder = new File(outputFolderPath);
        if (!folder.exists() && !folder.mkdirs()) {
            System.err.println("Failed to create output folder: " + outputFolderPath);
            return;
        }

        String baseName = originalFileName.replaceAll("\\.\\w+$", ""); // Strip extension
        String fileName = baseName + "_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(folder, fileName);

        System.out.println("Saving image to quarantine folder: " + outputFile.getAbsolutePath());

        try {
            ImageIO.write(image, "png", outputFile);
            System.out.println("Image saved to quarantine successfully.");
        } catch (IOException e) {
            System.err.println("Failed to save image to quarantine: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
