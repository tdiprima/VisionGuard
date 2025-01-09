package com.tdiprima.tests;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Test reading the example image
 * 
 * @author tdiprima
 */
public class TestReadImage {
    public static void main(String[] args) {
        try (InputStream is = TestReadImage.class.getResourceAsStream("/images/example.png")) {
            BufferedImage image = ImageIO.read(is);
            System.out.println("Image loaded successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

