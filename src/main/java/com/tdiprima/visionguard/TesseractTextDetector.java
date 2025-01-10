package com.tdiprima.visionguard;

import net.sourceforge.tess4j.Tesseract;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private int minWidth = DEFAULT_MIN_WIDTH;
    private int minHeight = DEFAULT_MIN_HEIGHT;
    private int maxWidth = DEFAULT_MAX_WIDTH;
    private int maxHeight = DEFAULT_MAX_HEIGHT;
    private String quarantineFolderPath = DEFAULT_QUARANTINE_FOLDER;
    private String moveToFolderPath = DEFAULT_MOVE_FOLDER;

    @Override
    public void setBoundingBoxConstraints(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    @Override
    public void initialize(DetectorConfig config) {
        this.minWidth = config.minWidth;
        this.minHeight = config.minHeight;
        this.maxWidth = config.maxWidth;
        this.maxHeight = config.maxHeight;
        this.quarantineFolderPath = config.quarantinePath != null ? config.quarantinePath : DEFAULT_QUARANTINE_FOLDER;
        this.moveToFolderPath = config.moveToFolderPath != null ? config.moveToFolderPath : "output";

        // Log default path usage
        if (config.quarantinePath == null) {
            System.out.println("Using default quarantine path: " + DEFAULT_QUARANTINE_FOLDER);
        }

        if (config.moveToFolderPath == null) {
            System.out.println("Using default move-to-folder path: output");
        }
    }

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

                System.out.printf("Detected region: [x=%d, y=%d, width=%d, height=%d, text='%s']%n", x, y, width, height, word.getText().trim());

                // Apply size constraints
                if (width >= minWidth && height >= minHeight && width <= maxWidth && height <= maxHeight) {
                    regions.add(new TextRegion(x, y, width, height, word.getText()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
                moveImageToFolder(result.modifiedImage, moveToFolderPath, originalFileName); // Use moveToFolderPath from config
                break;

            case QUARANTINE:
                moveImageToFolder(result.modifiedImage, quarantineFolderPath, originalFileName); // Use quarantineFolderPath from config
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

        if (regions == null || regions.isEmpty()) {
            System.out.println("No regions to outline.");
            return image; // Return original image if no regions found
        }

        for (TextRegion region : regions) {
            System.out.printf("Drawing bounding box: x=%d, y=%d, width=%d, height=%d%n",
                    region.x, region.y, region.width, region.height);
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
