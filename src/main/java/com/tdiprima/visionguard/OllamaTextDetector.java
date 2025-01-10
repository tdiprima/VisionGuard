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
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
                """,
                    base64Image
            );

            // Send the POST request
            String responseJson = sendPostRequest(ollamaServerUrl, jsonPayload);

            // Parse and print the response field
            JsonObject responseObject = JsonParser.parseString(responseJson).getAsJsonObject();

            if (responseObject.has("response")) {
                response = responseObject.get("response").getAsString();
                System.out.println("Response from Ollama: " + response);
            } else {
                System.out.println("Response field not found in the response JSON.");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during detection: {0}", e.getMessage());
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

    private String sendPostRequest(String urlString, String jsonPayload) {
        System.out.println("*** QUERYING LLAMA VISION MODEL ***");
        StringBuilder response = new StringBuilder();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
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
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending POST request: {0}", e.getMessage());
            e.printStackTrace();
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
        case OUTLINE:
            BufferedImage outlinedImage = outlineTextRegions(result.modifiedImage, result.regions);
            saveImage(outlinedImage, outputPath, originalFileName);
            break;

        case MASK:
            BufferedImage maskedImage = maskTextRegions(result.modifiedImage, result.regions);
            saveImage(maskedImage, outputPath, originalFileName);
            break;

        case MOVE_TO_FOLDER:
            moveImageToFolder(result.modifiedImage, outputPath, originalFileName);
            break;

        case QUARANTINE:
            String quarantinePath = outputPath + "/quarantine/";
            moveImageToFolder(result.modifiedImage, quarantinePath, originalFileName);
            break;

        default:
            throw new UnsupportedOperationException("Action not supported: " + action);
    }
}


    private BufferedImage outlineTextRegions(BufferedImage image, List<TextRegion> regions) {
        BufferedImage outlinedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outlinedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.setColor(new java.awt.Color(255, 0, 0, 128));
        for (TextRegion region : regions) {
            g2d.drawRect(region.x, region.y, region.width, region.height);
        }
        g2d.dispose();
        return outlinedImage;
    }

    private BufferedImage maskTextRegions(BufferedImage image, List<TextRegion> regions) {
        BufferedImage maskedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = maskedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.setColor(new java.awt.Color(0, 0, 0, 255));
        for (TextRegion region : regions) {
            g2d.fillRect(region.x, region.y, region.width, region.height);
        }
        g2d.dispose();
        return maskedImage;
    }

    private void saveImage(BufferedImage image, String outputPath, String originalFileName) {
        try {
            String baseName = originalFileName.replaceAll("\\.\\w+$", ""); // Strip extension
            String fileName = baseName + "_" + System.currentTimeMillis() + ".png";
            File outputFile = new File(outputPath, fileName);
            ImageIO.write(image, "png", outputFile);
            logger.log(Level.INFO, "Image saved to: {0}", outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }

    private void moveImageToFolder(BufferedImage image, String outputFolderPath, String originalFileName) {
        File folder = new File(outputFolderPath);
        if (!folder.exists() && !folder.mkdirs()) {
            System.err.println("Failed to create output folder: " + outputFolderPath);
            return;
        }

        String baseName = originalFileName.replaceAll("\\.\\w+$", ""); // Strip extension
        String fileName = baseName + "_" + System.currentTimeMillis() + "_ollama.png";
        File outputFile = new File(folder, fileName);

        saveImage(image, outputFile.getAbsolutePath(), originalFileName);
    }

    @Override
    public void setBoundingBoxConstraints(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
