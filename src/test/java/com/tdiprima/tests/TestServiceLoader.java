package com.tdiprima.tests;

import java.util.ServiceLoader;
import com.tdiprima.visionguard.TextPHIDetector;

/**
 *
 * @author tdiprima
 */
public class TestServiceLoader {
    public static void main(String[] args) {
        ServiceLoader<TextPHIDetector> loader = ServiceLoader.load(TextPHIDetector.class);

        if (!loader.iterator().hasNext()) {
            System.err.println("No implementations found for TextPHIDetector.");
            return;
        }

        TextPHIDetector detector = loader.iterator().next();
        System.out.println("TextPHIDetector implementation loaded: " + detector.getClass().getName());
    }
}
