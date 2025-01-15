package com.tdiprima.visionguard;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * A a utility class that provides methods for image processing tasks, such as 
 * outlining text regions, adding watermarks, masking or burning specific areas, 
 * and saving images with metadata.
 * 
 * @author tdiprima
 */
public class ImageUtils {
    private static final Logger logger = Logger.getLogger(ImageUtils.class.getName());
    
    // Utility to draw bounding boxes on the image
    public static BufferedImage outlineTextRegions(BufferedImage image, List<TextDetector.TextRegion> regions) {
        BufferedImage outlinedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outlinedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        g2d.setColor(new Color(255, 0, 0, 128)); // Red semi-transparent

        if (regions == null || regions.isEmpty()) {
            System.out.println("No regions to outline.");
            g2d.dispose();
            return image; // Return original image if no regions found
        }

        for (TextDetector.TextRegion region : regions) {
            // System.out.printf("Drawing bounding box: x=%d, y=%d, width=%d, height=%d%n", region.x, region.y, region.width, region.height)
            g2d.drawRect(region.x, region.y, region.width, region.height);
        }

        g2d.dispose();
        return outlinedImage;
    }

    public static BufferedImage addWatermark(BufferedImage image, String watermarkText) {
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

    public static void saveImageWithMetadata(BufferedImage image, List<TextDetector.TextRegion> regions, String outputPath, String originalFileName) {
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
                for (TextDetector.TextRegion region : regions) {
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
    public static void saveImage(BufferedImage image, String outputPath, String originalFileName) {
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
    public static BufferedImage maskTextRegions(BufferedImage image, List<TextDetector.TextRegion> regions) {
        BufferedImage maskedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = maskedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        g2d.setColor(Color.BLACK);

        for (TextDetector.TextRegion region : regions) {
            g2d.fillRect(region.x, region.y, region.width, region.height);
        }

        g2d.dispose();
        return maskedImage;
    }

    public static BufferedImage burnTextRegions(BufferedImage image, List<TextDetector.TextRegion> regions) {
        BufferedImage burnedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = burnedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        g2d.setColor(new Color(255, 0, 0, 128)); // Semi-transparent red

        for (TextDetector.TextRegion region : regions) {
            g2d.fillRect(region.x, region.y, region.width, region.height);
        }

        g2d.dispose();
        return burnedImage;
    }

    public static BufferedImage burnDICOMTextRegions(BufferedImage image, List<TextDetector.TextRegion> regions) {
        BufferedImage burnedImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = burnedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        // Create a more visible effect for grayscale/DICOM using a solid white fill
        g2d.setColor(Color.WHITE);
        for (TextDetector.TextRegion region : regions) {
            g2d.fillRect(region.x, region.y, region.width, region.height);
        }

        g2d.dispose();
        return burnedImage;
    }
}
