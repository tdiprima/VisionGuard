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
    public String quarantinePath = "quarantine";
    public String moveToFolderPath = "output";

    // Load parameters from CLI arguments
    public static DetectorConfig fromArgs(String[] args) {
        DetectorConfig config = new DetectorConfig();
        for (String arg : args) {
            if (arg.startsWith("--minWidth=")) config.minWidth = Integer.parseInt(arg.split("=")[1]);
            if (arg.startsWith("--minHeight=")) config.minHeight = Integer.parseInt(arg.split("=")[1]);
            if (arg.startsWith("--maxWidth=")) config.maxWidth = Integer.parseInt(arg.split("=")[1]);
            if (arg.startsWith("--maxHeight=")) config.maxHeight = Integer.parseInt(arg.split("=")[1]);
            if (arg.startsWith("--quarantinePath=")) config.quarantinePath = arg.split("=")[1];
            if (arg.startsWith("--moveToFolderPath=")) config.moveToFolderPath = arg.split("=")[1];
        }
        return config;
    }
}
