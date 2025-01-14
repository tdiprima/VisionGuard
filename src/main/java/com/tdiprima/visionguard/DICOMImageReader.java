package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReader;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Reads a DICOM image file and converts it to a BufferedImage
 * 
 * @author tdiprima
 */
public class DICOMImageReader {

    public static BufferedImage readDICOMAsBufferedImage(File dicomFile) throws IOException {
        DicomImageReader reader = new DicomImageReader(new DicomImageReaderSpi());
        reader.setInput(ImageIO.createImageInputStream(dicomFile));
        return reader.read(0);
    }
}
