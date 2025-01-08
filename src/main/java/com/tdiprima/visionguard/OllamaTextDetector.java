package com.tdiprima.visionguard;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class OllamaTextDetector implements TextDetector {

    private String ollamaServerUrl;

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
            // Convert image to a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] imageBytes = baos.toByteArray();

            // Create JSON payload
            String payload = String.format(
                    "{\"model\": \"llama3.2-vision:latest\", \"prompt\": \"Find all text and return bounding boxes. Just return a JSON object with a 'regions' array containing objects containing x, y, width, height, and text.\", \"image\": \"%s\"}",
                    java.util.Base64.getEncoder().encodeToString(imageBytes)
            );

            // Send POST request
            URL url = new URL(ollamaServerUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.getOutputStream().write(payload.getBytes());

            // Read response
            InputStream responseStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();

            // Parse response JSON
            String responseJson = responseBuilder.toString();
            System.out.println("responseJson: " + responseJson);
            regions = parseOllamaResponse(responseJson);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DetectionResult(image, regions);
    }

    private List<TextRegion> parseOllamaResponse(String responseJson) {
        List<TextRegion> regions = new ArrayList<>();
        try {
            // Use a JSON library like Jackson or Gson
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(responseJson).getAsJsonObject();
            com.google.gson.JsonArray regionsArray = jsonObject.getAsJsonArray("regions");

            for (com.google.gson.JsonElement element : regionsArray) {
                com.google.gson.JsonObject regionObject = element.getAsJsonObject();
                int x = regionObject.get("x").getAsInt();
                int y = regionObject.get("y").getAsInt();
                int width = regionObject.get("width").getAsInt();
                int height = regionObject.get("height").getAsInt();
                String text = regionObject.get("text").getAsString();

                regions.add(new TextRegion(x, y, width, height, text));
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
            System.out.println("Image saved to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }

}
