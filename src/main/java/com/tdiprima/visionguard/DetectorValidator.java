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
            writer.write("\n*** Tesseract Detected Regions ***\n\n");
            if (tesseractResult.regions != null && !tesseractResult.regions.isEmpty()) {
                for (TextRegion region : tesseractResult.regions) {
                    writer.write(String.format("Text: '%s', Bounding Box: [x: %d, y: %d, width: %d, height: %d]\n",
                            region.text.trim(), region.x, region.y, region.width, region.height));
                }
            } else {
                writer.write("No regions detected by Tesseract.\n");
            }

            if (ollamaResult != null) {
                writer.write("\n*** Ollama Response ***\n\n");
                if (ollamaResult.rawResponse != null && !ollamaResult.rawResponse.isEmpty()) {
//                    writer.write("Raw Ollama Response: " + ollamaResult.rawResponse + "\n");

                    // Extract meaningful text from Ollama response if required
                    List<String> ollamaTexts = extractTextFromOllamaResponse(ollamaResult.rawResponse);
                    if (!ollamaTexts.isEmpty()) {
                        writer.write("\nExtracted Texts from Ollama:\n");
                        for (String text : ollamaTexts) {
                            writer.write("- " + text + "\n");
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
        // Stub: Replace with actual parsing logic as needed
        return List.of(rawResponse.split("\n")); // Example: Split response by newlines
    }

    private static void compareResults(DetectionResult tesseractResult, DetectionResult ollamaResult, BufferedWriter writer) throws IOException {
        if (tesseractResult.regions == null || ollamaResult.rawResponse == null) {
            writer.write("\nComparison skipped due to missing data.\n");
            return;
        }

        List<String> ollamaTexts = extractTextFromOllamaResponse(ollamaResult.rawResponse);

        writer.write("\n*** Comparison of Tesseract and Ollama Results ***\n\n");
        for (TextRegion tesseractRegion : tesseractResult.regions) {
            String tesseractText = tesseractRegion.text.trim();
            boolean matchFound = ollamaTexts.stream().anyMatch(ollamaText -> ollamaText.contains(tesseractText));

            if (matchFound) {
                writer.write(String.format("Matched: '%s'\n", tesseractText));
            } else {
                writer.write(String.format("Not Matched: '%s'\n", tesseractText));
            }
        }
    }
}
