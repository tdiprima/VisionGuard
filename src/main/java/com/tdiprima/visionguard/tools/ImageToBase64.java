package com.tdiprima.visionguard.tools;

import java.io.*;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 *
 * @author tdiprima
 */
public class ImageToBase64 {

    public static void main(String[] args) {
        // Path to the input image
        String imagePath = "src/main/resources/images/example.png";

        // Path to save the Base64 string
        String outputPath = "base64_image.txt";

        try {
            // Read the image
             File imageFile = new File(imagePath);
             BufferedImage image = ImageIO.read(imageFile);
            
            // Load the image as an InputStream (image should be in the resources folder)
//            InputStream inputStream = ImageToBase64.class.getResourceAsStream(imagePath);
//            if (inputStream == null) {
//                throw new IOException("Image resource not found. Check the resource path.");
//            }

            // Read the image from InputStream
//            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("Image could not be loaded. Check the file path or format.");
            }

            // Convert the image to Base64
            String base64Image = convertImageToBase64(image, "png");

            // Save the Base64 string to a file
            saveToFile(base64Image, outputPath);

            System.out.println("Base64 string saved successfully to: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("An error occurred while processing the image.");
        }
    }

    // Converts a BufferedImage to a Base64 string
    private static String convertImageToBase64(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        baos.flush();
        byte[] imageBytes = baos.toByteArray();
        baos.close();

        // Encode byte array to Base64 string
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    // Saves a string to a text file
    private static void saveToFile(String data, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(data);
        }
    }
}
