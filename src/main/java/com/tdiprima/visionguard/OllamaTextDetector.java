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
        // No configuration required beyond server URL setup
    }

    @Override
    public void setBoundingBoxConstraints(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        // Ollama doesn't return bounding boxes (vision model is not good at it)
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
            String response = responseObject.has("response") ? responseObject.get("response").getAsString() : "";

            return new DetectionResult(image, response);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Detection error: {0}", e.getMessage());
            return new DetectionResult(image, "");
        }
    }

    @Override
    public void applyAction(Action action, DetectionResult result, String outputPath, String originalFileName) {
        if (action == Action.EXPORT_TO_FOLDER) {
            saveResponseToFile(result.rawResponse, outputPath, originalFileName);
        } else {
            logger.warning("Action " + action + " is not supported by OllamaTextDetector.");
        }
    }

    private String encodeImageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to encode image: {0}", e.getMessage());
            return null;
        }
    }

    private String sendPostRequest(String urlString, String jsonPayload) throws IOException {
        System.out.println("*** QUERYING LLAMA VISION MODEL ***");
        HttpURLConnection connection = null;
        StringBuilder response = new StringBuilder();
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

    private void saveResponseToFile(String response, String outputPath, String originalFileName) {
        File outputFile = new File(outputPath, originalFileName + "_response.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("Ollama Response:\n" + response);
            logger.info("Response saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save response: {0}", e.getMessage());
        }
    }
}
