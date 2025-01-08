package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import javax.imageio.ImageIO;

public class OllamaHelpers {

    // Convert BufferedImage to Base64 string
    public static String encodeImageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos); // TODO: Assuming the image is in PNG format
            baos.flush();
            byte[] imageBytes = baos.toByteArray();
            baos.close();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            System.err.println("Error encoding image to Base64: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Send an HTTP POST request with JSON payload
    public static String sendPostRequest(String urlString, String jsonPayload) {
        StringBuilder response = new StringBuilder();
        HttpURLConnection connection = null;
        
        System.out.println("*** QUERYING LLAMA VISION MODEL ***");

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
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

        } catch (Exception e) {
            System.err.println("Error sending POST request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response.toString();
    }
}
