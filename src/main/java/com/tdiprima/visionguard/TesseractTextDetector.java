package com.tdiprima.visionguard;

import net.sourceforge.tess4j.Tesseract;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * A text detection implementation that leverages the Tesseract OCR library to
 * extract text and bounding box information from images, with configurable
 * constraints and actions.
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

    @Override
    public void setBoundingBoxConstraints(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        if (minWidth > maxWidth || minHeight > maxHeight) {
            throw new IllegalArgumentException("Invalid bounding box constraints: min must be <= max.");
        }
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
    public DetectionResult detect(BufferedImage image) {
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

                // System.out.printf("Detected region: [x=%d, y=%d, width=%d, height=%d, text='%s']%n", x, y, width, height, word.getText().trim());
                // Apply size constraints
                if (width >= minWidth && height >= minHeight && width <= maxWidth && height <= maxHeight) {
                    regions.add(new TextRegion(x, y, width, height, word.getText()));
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during Tesseract OCR: {0}", e.getMessage());
            // throw new RuntimeException("Unexpected error during OCR processing: " + e.getMessage(), e);
            return new DetectionResult(image, regions); // Return image with empty regions
        }

        return new DetectionResult(image, regions);
    }

    @Override
    public void applyAction(Action action, DetectionResult result, String outputPath, String originalFileName) {
        if (result.regions == null || result.regions.isEmpty()) {
            logger.info("No valid text regions detected. Skipping action for: " + originalFileName);
            return;
        }

        switch (action) {
            case OUTLINE:
                BufferedImage outlinedImage = outlineTextRegions(result.modifiedImage, result.regions);
                saveImage(outlinedImage, outputPath, originalFileName);
                break;

            case MASK:
                BufferedImage maskedImage = maskTextRegions(result.modifiedImage, result.regions);
                saveImage(maskedImage, outputPath, originalFileName);
                break;

            case BURN:
                BufferedImage burnedImage = burnTextRegions(result.modifiedImage, result.regions);
                saveImage(burnedImage, outputPath, originalFileName);
                break;

            case EXPORT_TO_FOLDER:
                saveImageWithMetadata(result.modifiedImage, result.regions, outputPath, originalFileName);
                break;

            case FLAG_FOR_REVIEW:
                BufferedImage flaggedImage = addWatermark(result.modifiedImage, "QUARANTINE");
                saveImage(flaggedImage, outputPath, originalFileName);
                break;

            default:
                throw new UnsupportedOperationException("Action not supported: " + action);
        }
    }

    // Utility to draw bounding boxes on the image
    private BufferedImage outlineTextRegions(BufferedImage image, List<TextRegion> regions) {
        BufferedImage outlinedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outlinedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        g2d.setColor(new Color(255, 0, 0, 128)); // Red semi-transparent

        if (regions == null || regions.isEmpty()) {
            System.out.println("No regions to outline.");
            g2d.dispose();
            return image; // Return original image if no regions found
        }

        for (TextRegion region : regions) {
            // System.out.printf("Drawing bounding box: x=%d, y=%d, width=%d, height=%d%n", region.x, region.y, region.width, region.height)
            g2d.drawRect(region.x, region.y, region.width, region.height);
        }

        g2d.dispose();
        return outlinedImage;
    }

    private BufferedImage addWatermark(BufferedImage image, String watermarkText) {
        BufferedImage watermarkedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = watermarkedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        // Configure watermark properties
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.setColor(new Color(255, 0, 0, 128)); // Semi-transparent red
        FontMetrics metrics = g2d.getFontMetrics();
        int x = (image.getWidth() - metrics.stringWidth(watermarkText)) / 2;
        int y = image.getHeight() / 2;

        g2d.drawString(watermarkText, x, y);
        g2d.dispose();

        return watermarkedImage;
    }

    private void saveImageWithMetadata(BufferedImage image, List<TextRegion> regions, String outputPath, String originalFileName) {
        File folder = new File(outputPath);
        if (!folder.exists() && !folder.mkdirs()) {
            logger.log(Level.SEVERE, "Failed to create output folder: {0}", outputPath);
            return;
        }

        String baseName = originalFileName.replaceAll("\\.\\w+$", ""); // Strip extension
        String fileName = baseName + "_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(folder, fileName);

        try {
            // Save the image
            ImageIO.write(image, "png", outputFile);

            // Save metadata
            File metadataFile = new File(folder, baseName + "_" + System.currentTimeMillis() + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile))) {
                for (TextRegion region : regions) {
                    writer.write(String.format("Text: '%s', Bounding Box: [x: %d, y: %d, width: %d, height: %d]%n",
                            region.text.trim(), region.x, region.y, region.width, region.height));
                }
            }

            logger.log(Level.INFO, "Image and metadata saved to: {0}", outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save image or metadata: {0}", e.getMessage());
        }
    }

    // Utility to save an image to disk
    private void saveImage(BufferedImage image, String outputPath, String originalFileName) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        File outputFile = new File(outputPath, baseName + "." + extension);

        try {
            if (extension.equals("dcm") || extension.equals("dicom")) {
                DICOMImageReader.saveBufferedImageAsDICOM(image, outputFile);
            } else {
                ImageIO.write(image, extension, outputFile);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save image: {0}", e.getMessage());
        }
    }

    // Utility to mask text regions
    private BufferedImage maskTextRegions(BufferedImage image, List<TextRegion> regions) {
        BufferedImage maskedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = maskedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        g2d.setColor(Color.BLACK);

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
            logger.log(Level.SEVERE, "Failed to create output folder: {0}", outputFolderPath);
            return;
        }

        String baseName = originalFileName.replaceAll("\\.\\w+$", ""); // Strip extension
        String fileName = baseName + "_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(folder, fileName);

        try {
            ImageIO.write(image, "png", outputFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save image to folder: {0}", e.getMessage());
        }
    }

    private BufferedImage burnTextRegions(BufferedImage image, List<TextRegion> regions) {
        BufferedImage burnedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = burnedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        g2d.setColor(new Color(255, 0, 0, 128)); // Semi-transparent red

        for (TextRegion region : regions) {
            g2d.fillRect(region.x, region.y, region.width, region.height);
        }

        g2d.dispose();
        return burnedImage;
    }

}
