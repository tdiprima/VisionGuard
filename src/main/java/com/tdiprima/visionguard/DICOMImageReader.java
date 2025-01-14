package com.tdiprima.visionguard;

import java.awt.image.BufferedImage;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReader;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.image.BufferedImageUtils;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.UIDUtils;

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

    public static void saveBufferedImageAsDICOM(BufferedImage image, File dicomFile) throws IOException {
        // Example implementation using dcm4che:
        try (DicomOutputStream dos = new DicomOutputStream(dicomFile)) {
            // Create a minimal DICOM structure
            Attributes dataset = new Attributes();
            dataset.setString(Tag.SOPClassUID, VR.UI, UID.SecondaryCaptureImageStorage);
            dataset.setString(Tag.SOPInstanceUID, VR.UI, UIDUtils.createUID());
            dataset.setDate(Tag.InstanceCreationDate, new Date());
            dataset.setDate(Tag.InstanceCreationTime, new Date());

            // Encode BufferedImage into pixel data
            BufferedImageUtils.writeToDataset(dataset, image);

            dos.writeDataset(dataset.createFileMetaInformation(UID.ImplicitVRLittleEndian), dataset);
        }
    }

}
