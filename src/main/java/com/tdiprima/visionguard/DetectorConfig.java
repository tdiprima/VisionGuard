package com.tdiprima.visionguard;

/**
 * Manage parameters centrally
 * 
 * @author tdiprima
 */
public class DetectorConfig {
    public int minWidth = 10;
    public int minHeight = 10;
    public int maxWidth = 500;
    public int maxHeight = 500;
    public boolean enableOllama = false;

    // Load parameters from CLI arguments
    public static DetectorConfig fromArgs(String[] args) {
        DetectorConfig config = new DetectorConfig();
        try {
            for (String arg : args) {
                if (arg.startsWith("--minWidth=")) {
                    config.minWidth = parsePositiveInt(arg.split("=")[1], config.minWidth, "minWidth");
                }
                if (arg.startsWith("--minHeight=")) {
                    config.minHeight = parsePositiveInt(arg.split("=")[1], config.minHeight, "minHeight");
                }
                if (arg.startsWith("--maxWidth=")) {
                    config.maxWidth = parsePositiveInt(arg.split("=")[1], config.maxWidth, "maxWidth");
                }
                if (arg.startsWith("--maxHeight=")) {
                    config.maxHeight = parsePositiveInt(arg.split("=")[1], config.maxHeight, "maxHeight");
                }
                if (arg.startsWith("--ollama=")) {
                    config.enableOllama = Boolean.parseBoolean(arg.split("=")[1]);
                }
            }

            // Additional validation for logical bounds
            if (config.minWidth > config.maxWidth) {
                System.err.println("Warning: minWidth exceeds maxWidth. Resetting to default values.");
                config.minWidth = 10;
                config.maxWidth = 500;
            }
            if (config.minHeight > config.maxHeight) {
                System.err.println("Warning: minHeight exceeds maxHeight. Resetting to default values.");
                config.minHeight = 10;
                config.maxHeight = 500;
            }
        } catch (Exception e) {
            System.err.println("Error parsing configuration arguments: " + e.getMessage());
            System.err.println("Using default configuration values.");
        }
        return config;
    }

    // Helper method to parse and validate positive integers
    private static int parsePositiveInt(String value, int defaultValue, String paramName) {
        try {
            int parsedValue = Integer.parseInt(value);
            if (parsedValue <= 0) {
                throw new IllegalArgumentException(paramName + " must be a positive integer.");
            }
            return parsedValue;
        } catch (NumberFormatException e) {
            System.err.println("Invalid value for " + paramName + ": " + value + ". Using default value: " + defaultValue);
            return defaultValue;
        }
    }
}
