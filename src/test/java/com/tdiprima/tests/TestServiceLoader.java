package com.tdiprima.tests;

import java.util.ServiceLoader;
import com.tdiprima.visionguard.TextDetector;

/**
 *
 * @author tdiprima
 */
public class TestServiceLoader {
    public static void main(String[] args) {
        ServiceLoader<TextDetector> loader = ServiceLoader.load(TextDetector.class);

        if (!loader.iterator().hasNext()) {
            System.err.println("No implementations found for TextDetector.");
            return;
        }

        TextDetector detector = loader.iterator().next();
        System.out.println("TextDetector implementation loaded: " + detector.getClass().getName());
    }
}
