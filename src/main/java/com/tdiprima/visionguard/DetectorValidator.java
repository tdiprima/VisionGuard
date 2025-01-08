package com.tdiprima.visionguard;

import com.tdiprima.visionguard.TextDetector.DetectionResult;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DetectorValidator {

    public static void validate(DetectionResult result1, DetectionResult result2, String outputPath) {
        List<TextDetector.TextRegion> regions1 = result1.regions;
        List<TextDetector.TextRegion> regions2 = result2.regions;

        List<TextDetector.TextRegion> missedRegions = new ArrayList<>();
        List<TextDetector.TextRegion> mismatchedRegions = new ArrayList<>();

        // Compare regions between results
        for (TextDetector.TextRegion r1 : regions1) {
            boolean matched = false;
            for (TextDetector.TextRegion r2 : regions2) {
                if (overlaps(r1, r2) && similarText(r1.text, r2.text)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                missedRegions.add(r1);
            }
        }

        for (TextDetector.TextRegion r2 : regions2) {
            boolean matched = false;
            for (TextDetector.TextRegion r1 : regions1) {
                if (overlaps(r2, r1) && similarText(r2.text, r1.text)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                mismatchedRegions.add(r2);
            }
        }

        // Save discrepancy report
        saveDiscrepancyReport(missedRegions, mismatchedRegions, outputPath);
    }

    private static boolean overlaps(TextDetector.TextRegion r1, TextDetector.TextRegion r2) {
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

    private static void saveDiscrepancyReport(List<TextDetector.TextRegion> missedRegions,
                                              List<TextDetector.TextRegion> mismatchedRegions,
                                              String outputPath) {
        // Save missed and mismatched regions to a report file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("Missed Regions:\n");
            for (TextDetector.TextRegion r : missedRegions) {
                writer.write(String.format("Text: %s, Bounding Box: [%d, %d, %d, %d]\n",
                        r.text, r.x, r.y, r.width, r.height));
            }

            writer.write("\nMismatched Regions:\n");
            for (TextDetector.TextRegion r : mismatchedRegions) {
                writer.write(String.format("Text: %s, Bounding Box: [%d, %d, %d, %d]\n",
                        r.text, r.x, r.y, r.width, r.height));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
