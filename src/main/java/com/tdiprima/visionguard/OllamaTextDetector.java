package com.tdiprima.visionguard;

import static com.tdiprima.visionguard.OllamaHelpers.encodeImageToBase64;
import static com.tdiprima.visionguard.OllamaHelpers.sendPostRequest;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import java.util.logging.Logger;

public class OllamaTextDetector implements TextDetector {

    private String ollamaServerUrl;
    private static final Logger logger = Logger.getLogger(OllamaTextDetector.class.getName());

    @Override
    public void setupParameters(String... params) {
        if (params.length < 1) {
            throw new IllegalArgumentException("Ollama server URL is required.");
        }
        this.ollamaServerUrl = params[0];
    }

    @Override
    public DetectionResult detect(BufferedImage image, Object metadata) {
        List<TextRegion> regions = new ArrayList<>();
        try {
            // Encode the image to Base64 using the helper function
            String base64Image = encodeImageToBase64(image);
            if (base64Image == null) {
                throw new IllegalArgumentException("Failed to encode image to Base64.");
            }

            // Create the JSON payload
            String payload = String.format(
                    "{"
                    + "\"model\": \"llama3.2-vision:latest\", "
                    + "\"system\": \"You are a JSON output generator. Your sole task is to analyze the input image and extract text regions. Your response must strictly conform to the format {'regions': [{'x': <x-coordinate>, 'y': <y-coordinate>, 'width': <width>, 'height': <height>, 'text': '<detected-text>'}, ...]}. You will not provide explanations, preambles, or any other output. If the image cannot be processed, respond only with {'regions': []}.\", "
                    + "\"prompt\": \"Find all text in the provided image and return their bounding boxes in JSON format. Follow the structure strictly. Do not include any other content or explanation.\", "
                    + "\"image\": \"%s\""
                    + "}",
                    base64Image
            );

            // Send the POST request using the helper function
            String responseJson = sendPostRequest(ollamaServerUrl, payload);
            System.out.println("responseJson: " + responseJson);

            // Parse the response JSON
            regions = parseOllamaResponse(responseJson);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DetectionResult(image, regions);
    }

    public String detectWithDynamicPrompts(BufferedImage image, String systemPrompt, String prompt) {
        // Adapt the method to include systemPrompt and prompt in the JSON payload dynamically
        String payload = String.format(
                "{"
                + "\"model\": \"llama3.2-vision:latest\", "
                + "\"system\": \"%s\", "
                + "\"prompt\": \"%s\", "
                + "\"image\": \"%s\""
                + "}",
                systemPrompt,
                prompt,
                encodeImageToBase64(image)
        );
        return sendPostRequest(ollamaServerUrl, payload);
    }

    private List<TextRegion> parseOllamaResponse(String responseJson) {
        List<TextRegion> regions = new ArrayList<>();
        StringBuilder reconstructedResponse = new StringBuilder();

        try {
            // Split the concatenated JSON objects
            String[] jsonObjects = responseJson.split("\\}\\{");

            for (int i = 0; i < jsonObjects.length; i++) {
                // Fix JSON fragments (add missing braces after splitting)
                if (i > 0) {
                    jsonObjects[i] = "{" + jsonObjects[i];
                }
                if (i < jsonObjects.length - 1) {
                    jsonObjects[i] = jsonObjects[i] + "}";
                }

                // Parse each object to extract the "response" field
                com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonObjects[i]).getAsJsonObject();
                if (jsonObject.has("response")) {
                    String responsePart = jsonObject.get("response").getAsString();
                    reconstructedResponse.append(responsePart.replace("\n", "").trim());
                }
            }

            // Parse the reconstructed JSON
            String finalResponseJson = reconstructedResponse.toString();
            com.google.gson.JsonObject finalResponseObject = com.google.gson.JsonParser.parseString(finalResponseJson).getAsJsonObject();
            if (finalResponseObject.has("regions")) {
                com.google.gson.JsonArray regionsArray = finalResponseObject.getAsJsonArray("regions");

                for (com.google.gson.JsonElement element : regionsArray) {
                    com.google.gson.JsonObject regionObject = element.getAsJsonObject();
                    int x = regionObject.get("x").getAsInt();
                    int y = regionObject.get("y").getAsInt();
                    int width = regionObject.get("width").getAsInt();
                    int height = regionObject.get("height").getAsInt();
                    String text = regionObject.get("text").getAsString();

                    regions.add(new TextRegion(x, y, width, height, text));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return regions;
    }

    @Override
    public void applyAction(Action action, DetectionResult result, String outputPath) {
        switch (action) {
            case OUTLINE:
                BufferedImage outlinedImage = outlineTextRegions(result.modifiedImage, result.regions);
                saveImage(outlinedImage, outputPath);
                break;

            case MASK:
                BufferedImage maskedImage = maskTextRegions(result.modifiedImage, result.regions);
                saveImage(maskedImage, outputPath);
                break;

            case MOVE_TO_FOLDER:
                moveImageToFolder(result.modifiedImage, outputPath);
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

    // Utility to mask text regions on the image
    private BufferedImage maskTextRegions(BufferedImage image, List<TextRegion> regions) {
        BufferedImage maskedImage = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = maskedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        g2d.setColor(new Color(0, 0, 0, 255)); // Black mask
        for (TextRegion region : regions) {
            g2d.fillRect(region.x, region.y, region.width, region.height);
        }

        g2d.dispose();
        return maskedImage;
    }

    // Utility to move an image to a specific folder
    private void moveImageToFolder(BufferedImage image, String outputFolderPath) {
        File folder = new File(outputFolderPath);
        if (!folder.exists() && !folder.mkdirs()) {
            System.err.println("Failed to create output folder: " + outputFolderPath);
            return;
        }

        String fileName = "ollama_detected_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(folder, fileName);

        saveImage(image, outputFile.getAbsolutePath());
        System.out.println("Image moved to folder: " + outputFile.getAbsolutePath());
    }

    // Utility to save an image to disk
    private void saveImage(BufferedImage image, String outputPath) {
        try {
            File outputFile = new File(outputPath);
            ImageIO.write(image, "png", outputFile);
            logger.log(Level.INFO, "Image saved to: {0}", outputPath);
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }

}
