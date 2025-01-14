package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReader;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import org.dcm4che3.io.DicomInputStream;

/**
 * Reads a DICOM image file and converts it to a BufferedImage
 *
 * @author tdiprima
 */
public class DICOMImageReader {

    public static BufferedImage readDICOMAsBufferedImage(File dicomFile) throws IOException {
        DicomImageReader reader = new DicomImageReader(new DicomImageReaderSpi());
        try {
            reader.setInput(ImageIO.createImageInputStream(dicomFile));

            // Handle missing transfer syntax by explicitly setting default
            DicomInputStream dicomInputStream = new DicomInputStream(dicomFile);
            dicomInputStream.setIncludeBulkData(DicomInputStream.IncludeBulkData.URI);
            dicomInputStream.readDatasetUntilPixelData(); // Attempt to read until pixel data

            return reader.read(0);
        } catch (IOException e) {
            throw new IOException("Failed to read DICOM file: " + dicomFile.getName(), e);
        }

    }
}
