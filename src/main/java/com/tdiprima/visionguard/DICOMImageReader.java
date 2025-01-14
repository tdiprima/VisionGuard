package com.tdiprima.visionguard;

import org.dcm4che3.data.*;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.image.PhotometricInterpretation;
import org.dcm4che3.util.UIDUtils;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import javax.imageio.ImageIO;
import static org.dcm4che3.data.Tag.PhotometricInterpretation;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReader;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReaderSpi;
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

    public static void saveBufferedImageAsDICOM(BufferedImage image, File dicomFile) throws IOException {
        Attributes dataset = new Attributes();

        // Set essential DICOM attributes
        dataset.setString(Tag.SOPClassUID, VR.UI, UID.SecondaryCaptureImageStorage);
        dataset.setString(Tag.SOPInstanceUID, VR.UI, UIDUtils.createUID());
        dataset.setString(Tag.PatientName, VR.PN, "Anonymous");
        dataset.setString(Tag.PatientID, VR.LO, "12345");
        dataset.setDate(Tag.StudyDate, VR.DA, new Date());
        dataset.setDate(Tag.StudyTime, VR.TM, new Date());
        dataset.setString(Tag.Modality, VR.CS, "OT"); // Other

        // Handle image dimensions and pixel data
        int width = image.getWidth();
        int height = image.getHeight();
        int samplesPerPixel = 3; // RGB
        int bitsAllocated = 8;
        int bitsStored = 8;
        int highBit = 7;
        int pixelRepresentation = 0; // Unsigned integers

        dataset.setInt(Tag.Rows, VR.US, height);
        dataset.setInt(Tag.Columns, VR.US, width);
        dataset.setInt(Tag.SamplesPerPixel, VR.US, samplesPerPixel);
        dataset.setString(Tag.PhotometricInterpretation, VR.CS, "RGB");
        dataset.setInt(Tag.BitsAllocated, VR.US, bitsAllocated);
        dataset.setInt(Tag.BitsStored, VR.US, bitsStored);
        dataset.setInt(Tag.HighBit, VR.US, highBit);
        dataset.setInt(Tag.PixelRepresentation, VR.US, pixelRepresentation);

        // Extract pixel data
        byte[] pixelData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        dataset.setBytes(Tag.PixelData, VR.OW, pixelData);

        // Write to DICOM file
        try (DicomOutputStream dos = new DicomOutputStream(dicomFile)) {
            dos.writeDataset(dataset.createFileMetaInformation(UID.ImplicitVRLittleEndian), dataset);
        }
    }

}
