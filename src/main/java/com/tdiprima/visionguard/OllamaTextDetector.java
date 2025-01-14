package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * A text detection implementation that uses an external API to extract text
 * from images, with support for configurable server endpoints.
 *
 * @author tdiprima
 */
public class OllamaTextDetector implements TextDetector {

    private String ollamaServerUrl;
    private static final Logger logger = Logger.getLogger(OllamaTextDetector.class.getName());

    @Override
    public void initialize(DetectorConfig config) {
        // Removed quarantineFolderPath and moveToFolderPath usage
    }

    @Override
    public void setBoundingBoxConstraints(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        // Ollama doesn't return bounding boxes (vision model is not good at it)
        System.out.println("setBoundingBoxConstraints is not applicable for OllamaTextDetector.");
    }

    @Override
    public void setupParameters(String... params) {
        if (params.length < 1) {
            throw new IllegalArgumentException("Ollama server URL is required.");
        }
        this.ollamaServerUrl = params[0];
    }

    @Override
    public DetectionResult detect(BufferedImage image) {
        String response = "";
        try {
            // Encode the image to Base64 (convert to PNG for Ollama compatibility)
            String base64Image = encodeImageToBase64(image);
            if (base64Image == null) {
                throw new IllegalArgumentException("Failed to encode image to Base64.");
            }

            // Create the JSON payload
            String jsonPayload = String.format(
                    """
                    {
                        "model": "llama3.2-vision",
                        "prompt": "Extract all text from the attached image",
                        "stream": false,
                        "images": ["%s"]
                    }
                    """, base64Image);

            // Send the POST request
            String responseJson = sendPostRequest(ollamaServerUrl, jsonPayload);

            // Parse and extract the response
            JsonObject responseObject = JsonParser.parseString(responseJson).getAsJsonObject();
            if (responseObject.has("response")) {
                response = responseObject.get("response").getAsString();
                System.out.println("Response from Ollama: " + response);
            } else {
                logger.log(Level.WARNING, "Response field not found in the response JSON.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during detection: {0}", e.getMessage());
            e.printStackTrace();
        }
        return new DetectionResult(image, response);
    }

    @Override
    public void applyAction(Action action, DetectionResult result, String outputPath, String originalFileName) {
        switch (action) {
            case OUTLINE:
                System.out.println("OUTLINE action is not applicable for OllamaTextDetector.");
                break;

            case MASK:
                System.out.println("MASK action is not applicable for OllamaTextDetector.");
                break;

            case BURN:
                System.out.println("BURN action is not applicable for OllamaTextDetector.");
                break;

            case EXPORT_TO_FOLDER:
                saveImageWithMetadata(result.modifiedImage, result.rawResponse, outputPath, originalFileName);
                break;

            case FLAG_FOR_REVIEW:
                BufferedImage flaggedImage = addWatermark(result.modifiedImage, "QUARANTINE");
                moveImageToFolder(flaggedImage, outputPath, originalFileName);
                break;

            default:
                throw new UnsupportedOperationException("Action not supported for OllamaTextDetector: " + action);
        }
    }

    private String encodeImageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to encode image to Base64: {0}", e.getMessage());
            return null;
        }
    }

    private String sendPostRequest(String urlString, String jsonPayload) throws IOException {
        System.out.println("*** QUERYING LLAMA VISION MODEL ***");
        StringBuilder response = new StringBuilder();
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // Send the JSON payload
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes());
                os.flush();
            }

            // Read the response
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return response.toString();
    }

    private void saveImageWithMetadata(BufferedImage image, String rawResponse, String outputPath, String originalFileName) {
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

            // Save raw response
            File metadataFile = new File(folder, baseName + "_" + System.currentTimeMillis() + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile))) {
                writer.write("Ollama Response:\n" + rawResponse);
            }

            logger.log(Level.INFO, "Image and metadata saved to: {0}", outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save image or metadata: {0}", e.getMessage());
        }
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
}
