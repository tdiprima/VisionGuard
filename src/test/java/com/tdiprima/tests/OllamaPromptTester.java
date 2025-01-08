package com.tdiprima.tests;

import com.tdiprima.visionguard.OllamaTextDetector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class OllamaPromptTester {

    public static void main(String[] args) {
        String imagePath = "/images/example.png";
        // String systemPrompt = "You are a JSON output generator. Your sole task is to analyze the input image and extract text regions. Your response must strictly conform to the format {'regions': [{'x': <x-coordinate>, 'y': <y-coordinate>, 'width': <width>, 'height': <height>, 'text': '<detected-text>'}, ...]}.";
        // String prompt = "Find all text in the provided image and return their bounding boxes in JSON format. Follow the structure strictly.";
        // RESULT: "Hello World"
        
        // Simplify the Task
        // String systemPrompt = "You are a text recognition assistant. Your task is to analyze images and extract visible text. If no text is detected, respond with 'No text found.' Do not add any explanations or additional formatting.";
        // String prompt = "Describe all visible text in the image without bounding boxes. Only provide the text.";
        // RESULT: "I'm ready when you are. What's the image?"
        
        // Adjust the System and Task Prompts
        String systemPrompt = "You are an image text recognition AI. Only return detected text and their bounding boxes. If no text is found, return {'regions': []}. Do not generate text or bounding boxes that are not clearly visible in the image.";
        String prompt = "Analyze the provided image and detect all text regions. If no text is found, respond with {'regions': []}. Ensure outputs are accurate and based solely on the image content.";
        // RESULT: "Hello World"

        // Some models may rely on default responses; include a confidence filter.  
        systemPrompt = systemPrompt + " Only output text regions with high confidence. If confidence is below a threshold or no text is detected, return {'regions': []}.";
        // RESULT: "I'll need you to provide the image for analysis..."

        // Load the image
        BufferedImage image;
        try {
            InputStream input = OllamaPromptTester.class.getResourceAsStream(imagePath);
            image = ImageIO.read(input);
            if (image == null) {
                throw new IOException("Failed to load image");
            }
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Initialize OllamaTextDetector
        OllamaTextDetector ollamaDetector = new OllamaTextDetector();
        ollamaDetector.setupParameters("http://localhost:11434/api/generate");

        // Perform detection with hard-coded prompts
        try {
            String responseJson = ollamaDetector.detectWithDynamicPrompts(image, systemPrompt, prompt);
            System.out.println("Response JSON:\n" + responseJson);
        } catch (Exception e) {
            System.err.println("Error during text detection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
