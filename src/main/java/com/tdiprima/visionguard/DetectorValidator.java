package com.tdiprima.visionguard;

import com.tdiprima.visionguard.TextDetector.DetectionResult;
import com.tdiprima.visionguard.TextDetector.TextRegion;
import java.util.List;

public class DetectorValidator {

    public static void validate(DetectionResult tesseractResult, DetectionResult ollamaResult, String reportPath) {
        System.out.println("\n*** Tesseract Detected Regions ***\n");
        if (tesseractResult.regions != null && !tesseractResult.regions.isEmpty()) {
            for (TextRegion region : tesseractResult.regions) {
                System.out.printf("Text: '%s', Bounding Box: [x: %d, y: %d, width: %d, height: %d]\n",
                        region.text, region.x, region.y, region.width, region.height);
            }
        } else {
            System.out.println("No regions detected by Tesseract.");
        }

        System.out.println("\n*** Ollama Response ***\n");
        if (ollamaResult.rawResponse != null && !ollamaResult.rawResponse.isEmpty()) {
            System.out.println("Raw Ollama Response: " + ollamaResult.rawResponse);

            // Optional: Extract meaningful text from Ollama response if required
            List<String> ollamaTexts = extractTextFromOllamaResponse(ollamaResult.rawResponse);
            if (!ollamaTexts.isEmpty()) {
                System.out.println("\nExtracted Texts from Ollama:");
                for (String text : ollamaTexts) {
                    System.out.println("- " + text);
                }
            }
        } else {
            System.out.println("No response received from Ollama.");
        }

        // Optional: Compare Tesseract detected regions and extracted Ollama texts
        compareResults(tesseractResult, ollamaResult);
    }

    private static List<String> extractTextFromOllamaResponse(String rawResponse) {
        // Stub: Replace with actual parsing logic as needed
        return List.of(rawResponse.split("\n")); // Example: Split response by newlines
    }

    private static void compareResults(DetectionResult tesseractResult, DetectionResult ollamaResult) {
        if (tesseractResult.regions == null || ollamaResult.rawResponse == null) {
            System.out.println("Comparison skipped due to missing data.");
            return;
        }

        List<String> ollamaTexts = extractTextFromOllamaResponse(ollamaResult.rawResponse);

        System.out.println("\n*** Comparison of Tesseract and Ollama Results ***\n");
        for (TextRegion tesseractRegion : tesseractResult.regions) {
            String tesseractText = tesseractRegion.text.trim();
            boolean matchFound = ollamaTexts.stream().anyMatch(ollamaText -> ollamaText.contains(tesseractText));

            if (matchFound) {
                System.out.printf("Matched: '%s'\n", tesseractText);
            } else {
                System.out.printf("Not Matched: '%s'\n", tesseractText);
            }
        }
    }
}
