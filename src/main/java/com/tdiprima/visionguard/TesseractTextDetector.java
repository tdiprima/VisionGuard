package com.tdiprima.visionguard;

import net.sourceforge.tess4j.Tesseract;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public void setupParameters(String... params) {
        tesseract = new Tesseract();
        tesseract.setDatapath(params[0]); // Path to Tesseract data
        if (params.length > 1) {
            tesseract.setLanguage(params[1]); // Language (e.g., "eng")
        }
    }

    @Override
    public void initialize(DetectorConfig config) {
        this.minWidth = config.minWidth;
        this.minHeight = config.minHeight;
        this.maxWidth = config.maxWidth;
        this.maxHeight = config.maxHeight;
    }

    // Dynamically change constraints after initialization
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
                BufferedImage outlinedImage = ImageUtils.outlineTextRegions(result.modifiedImage, result.regions, originalFileName);
                ImageUtils.saveImage(outlinedImage, outputPath, originalFileName);
                break;

            case MASK:
                BufferedImage maskedImage = ImageUtils.maskTextRegions(result.modifiedImage, result.regions);
                ImageUtils.saveImage(maskedImage, outputPath, originalFileName);
                break;

            case BURN:
                String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
                BufferedImage burnedImage;
                if (extension.equals("dcm") || extension.equals("dicom")) {
                    burnedImage = ImageUtils.burnDICOMTextRegions(result.modifiedImage, result.regions);
                } else {
                    burnedImage = ImageUtils.burnTextRegions(result.modifiedImage, result.regions);
                }

                ImageUtils.saveImage(burnedImage, outputPath, originalFileName);
                break;

            case EXPORT_TO_FOLDER:
                ImageUtils.saveImageWithMetadata(result.modifiedImage, result.regions, outputPath, originalFileName);
                break;

            case FLAG_FOR_REVIEW:
                BufferedImage flaggedImage = ImageUtils.addWatermark(result.modifiedImage, "QUARANTINE");
                ImageUtils.saveImage(flaggedImage, outputPath, originalFileName);
                break;

            default:
                throw new UnsupportedOperationException("Action not supported: " + action);
        }
    }

}
