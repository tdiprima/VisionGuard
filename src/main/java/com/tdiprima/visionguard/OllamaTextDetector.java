package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;

/**
 * A text detection implementation that uses an external API to extract text from 
 * images, with support for configurable server endpoints.
 * 
 * @author tdiprima
 */
public class OllamaTextDetector implements TextDetector {

    private String ollamaServerUrl;
    private static final Logger logger = Logger.getLogger(OllamaTextDetector.class.getName());
    private String quarantineFolderPath = DEFAULT_QUARANTINE_FOLDER;
    private String moveToFolderPath = DEFAULT_MOVE_FOLDER;

    @Override
    public void initialize(DetectorConfig config) {
        this.quarantineFolderPath = config.quarantinePath != null ? config.quarantinePath : DEFAULT_QUARANTINE_FOLDER;
        this.moveToFolderPath = config.moveToFolderPath != null ? config.moveToFolderPath : DEFAULT_MOVE_FOLDER;

        System.out.println("Using quarantine path: " + this.quarantineFolderPath);
        System.out.println("Using move-to-folder path: " + this.moveToFolderPath);
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
    public DetectionResult detect(BufferedImage image, Object metadata) {
        String response = "";
        try {
            // Encode the image to Base64
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
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Error during detection (Invalid Argument): {0}", e.getMessage());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error during detection (I/O Issue): {0}", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during detection: {0}", e.getMessage());
            e.printStackTrace();
        }
        return new DetectionResult(image, response);
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

    @Override
    public void applyAction(Action action, DetectionResult result, String outputPath, String originalFileName) {
        switch (action) {
            case MOVE_TO_FOLDER:
                moveImageToFolder(result.modifiedImage, moveToFolderPath, originalFileName);
                break;

            case QUARANTINE:
                moveImageToFolder(result.modifiedImage, quarantineFolderPath, originalFileName);
                break;

            default:
                throw new UnsupportedOperationException("Action not supported for OllamaTextDetector: " + action);
        }
    }

    private void moveImageToFolder(BufferedImage image, String outputFolderPath, String originalFileName) {
        File folder = new File(outputFolderPath);
        if (!folder.exists() && !folder.mkdirs()) {
            System.err.println("Failed to create output folder: " + outputFolderPath);
            return;
        }

        String baseName = originalFileName.replaceAll("\\.\\w+$", ""); // Strip extension
        String fileName = baseName + "_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(folder, fileName);

        try {
            ImageIO.write(image, "png", outputFile);
            System.out.println("Image saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }
}
