package com.tdiprima.visionguard;

import com.tdiprima.visionguard.TextDetector.DetectionResult;
import com.tdiprima.visionguard.TextDetector.TextRegion;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DetectorValidator {

    public static void validate(DetectionResult tesseractResult, DetectionResult ollamaResult, String outputPath) {
        List<TextRegion> tesseractRegions = tesseractResult.regions;
        List<TextRegion> ollamaRegions = ollamaResult.regions;

        List<TextRegion> missedRegions = new ArrayList<>();
        List<TextRegion> mismatchedRegions = new ArrayList<>();

        // Compare Tesseract regions to Ollama
        for (TextRegion tesseractRegion : tesseractRegions) {
            boolean matched = false;
            for (TextRegion ollamaRegion : ollamaRegions) {
                if (overlaps(tesseractRegion, ollamaRegion) && similarText(tesseractRegion.text, ollamaRegion.text)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                missedRegions.add(tesseractRegion); // Regions found by Tesseract but not by Ollama
            }
        }

        // Compare Ollama regions to Tesseract
        for (TextRegion ollamaRegion : ollamaRegions) {
            boolean matched = false;
            for (TextRegion tesseractRegion : tesseractRegions) {
                if (overlaps(ollamaRegion, tesseractRegion) && similarText(ollamaRegion.text, tesseractRegion.text)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                mismatchedRegions.add(ollamaRegion); // Regions found by Ollama but not by Tesseract
            }
        }

        // Save discrepancy report with more context
        saveDiscrepancyReport(missedRegions, mismatchedRegions, outputPath);
    }

    private static boolean overlaps(TextRegion r1, TextRegion r2) {
        // Check if bounding boxes overlap significantly
        int intersectionArea = Math.max(0, Math.min(r1.x + r1.width, r2.x + r2.width) - Math.max(r1.x, r2.x))
                * Math.max(0, Math.min(r1.y + r1.height, r2.y + r2.height) - Math.max(r1.y, r2.y));
        int area1 = r1.width * r1.height;
        int area2 = r2.width * r2.height;
        double overlapRatio = (double) intersectionArea / Math.min(area1, area2);
        return overlapRatio > 0.5; // Adjust threshold as needed
    }

    private static boolean similarText(String text1, String text2) {
        // Compare text content (e.g., using Levenshtein distance or exact match)
        return text1.equalsIgnoreCase(text2);
    }

    private static void saveDiscrepancyReport(List<TextRegion> missedRegions, List<TextRegion> mismatchedRegions, String outputPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("Missed Regions (Found by Tesseract but not by Ollama):\n");
            for (TextRegion r : missedRegions) {
                writer.write(String.format("Text: %s, Bounding Box: [%d, %d, %d, %d]\n",
                        r.text.trim(), r.x, r.y, r.width, r.height));
            }

            writer.write("\nMismatched Regions (Found by Ollama but not by Tesseract):\n");
            for (TextRegion r : mismatchedRegions) {
                writer.write(String.format("Text: %s, Bounding Box: [%d, %d, %d, %d]\n",
                        r.text, r.x, r.y, r.width, r.height));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
