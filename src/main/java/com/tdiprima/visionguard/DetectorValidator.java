package com.tdiprima.visionguard;

import com.tdiprima.visionguard.TextDetector.DetectionResult;
import com.tdiprima.visionguard.TextDetector.TextRegion;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * A utility class that validates and compares the detection results from
 * multiple text detectors, generating a report to highlight discrepancies or
 * confirm consistency.
 *
 * @author tdiprima
 */
public class DetectorValidator {

    public static void validate(DetectionResult tesseractResult, DetectionResult ollamaResult, String reportPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath))) {
            writer.write("*** Tesseract Detected Regions ***\n\n");
            if (tesseractResult.regions != null && !tesseractResult.regions.isEmpty()) {
                for (TextRegion region : tesseractResult.regions) {
                    writer.write(String.format("Text:\n%s, \n\nBounding Box:\n[x: %d, y: %d, width: %d, height: %d]\n",
                            region.text.trim(), region.x, region.y, region.width, region.height));
                }
            } else {
                writer.write("No regions detected by Tesseract.\n");
            }

            if (ollamaResult != null) {
                writer.write("\n\n*** Ollama Response ***\n\n");
                if (ollamaResult.rawResponse != null && !ollamaResult.rawResponse.isEmpty()) {
//                    writer.write("Raw Ollama Response: " + ollamaResult.rawResponse + "\n");

                    // Extract meaningful text from Ollama response if required
                    List<String> ollamaTexts = extractTextFromOllamaResponse(ollamaResult.rawResponse);
                    if (!ollamaTexts.isEmpty()) {
                        writer.write("Extracted Texts from Ollama:\n");
                        for (String text : ollamaTexts) {
                            writer.write(text + "\n");
                        }
                    }
                } else {
                    writer.write("No response received from Ollama.\n");
                }

                // Compare Tesseract detected regions and extracted Ollama texts
                compareResults(tesseractResult, ollamaResult, writer);
            } else {
                writer.write("\n*** Ollama Detection Skipped ***\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> extractTextFromOllamaResponse(String rawResponse) {
        return List.of(rawResponse.split("\n")); // Split response by newlines
    }

    private static void compareResults(DetectionResult tesseractResult, DetectionResult ollamaResult, BufferedWriter writer) throws IOException {
        if (tesseractResult.regions == null || ollamaResult.rawResponse == null) {
            writer.write("\nComparison skipped due to missing data.\n");
            return;
        }

    // Extract and filter Ollama texts
    List<String> ollamaTexts = extractTextFromOllamaResponse(ollamaResult.rawResponse).stream()
            .filter(text -> !text.trim().isEmpty()) // Exclude empty strings
            .filter(text -> !text.startsWith("The text in the image is:")) // Exclude predefined phrases
            .toList();

        writer.write("\n\n*** Comparison of Tesseract and Ollama Results ***\n\n");

        // Check Tesseract detections against Ollama results
        writer.write("Texts detected by Tesseract but not matched in Ollama:\n");
        for (TextRegion tesseractRegion : tesseractResult.regions) {
            String tesseractText = tesseractRegion.text.trim();
            boolean matchFound = ollamaTexts.stream().anyMatch(ollamaText -> ollamaText.contains(tesseractText));

            if (!matchFound) {
                writer.write(String.format("%s\n", tesseractText));
            }
        }

        // Check Ollama detections against Tesseract results
        writer.write("\nTexts detected by Ollama but not matched in Tesseract:\n");
        List<String> tesseractTexts = tesseractResult.regions.stream()
                .map(region -> region.text.trim())
                .toList();
        for (String ollamaText : ollamaTexts) {
            boolean matchFound = tesseractTexts.stream().anyMatch(tesseractText -> ollamaText.contains(tesseractText));

            if (!matchFound) {
                writer.write(String.format("%s\n", ollamaText));
            }
        }

        writer.write("\n*** End of Comparison ***\n");
    }

}
