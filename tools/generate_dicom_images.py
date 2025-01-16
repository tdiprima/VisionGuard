# Creates a blank DICOM image, overlays some dummy Protected Health Information (PHI), and saves the resulting file.
import datetime

import numpy as np
import pydicom
from PIL import Image, ImageDraw, ImageFont
from pydicom.dataset import FileDataset, FileMetaDataset
from pydicom.uid import generate_uid, ExplicitVRLittleEndian


def create_dicom_with_phi(output_filename="dicom_with_phi.dcm"):
    # Create a blank image (grayscale)
    img_width, img_height = 512, 512
    blank_image = np.zeros((img_height, img_width), dtype=np.uint8)

    # Convert to a PIL image to add text (burned-in PHI)
    pil_img = Image.fromarray(blank_image)
    draw = ImageDraw.Draw(pil_img)

    # try:
    #     font = ImageFont.truetype("Arial.ttf", size=12)
    # except IOError:
    #     font = ImageFont.load_default()  # Fallback to default font
    font = ImageFont.load_default()

    # Add dummy PHI (burned-in text)
    phi_text = [
        "Name: John Doe",
        "DOB: 1980-01-01",
        "ID: 123456789",
        "Exam Date: 2025-01-14"
    ]
    y = 10
    for line in phi_text:
        draw.text((10, y), line, fill=255, font=font)
        y += 15

    # Convert back to NumPy array
    burned_in_image = np.array(pil_img)

    # Create file meta information
    file_meta = FileMetaDataset()
    file_meta.MediaStorageSOPClassUID = pydicom.uid.SecondaryCaptureImageStorage  # Example: CT Image Storage
    file_meta.MediaStorageSOPInstanceUID = generate_uid()
    file_meta.ImplementationClassUID = generate_uid()
    file_meta.TransferSyntaxUID = ExplicitVRLittleEndian

    # Create a DICOM dataset
    ds = FileDataset(output_filename, {}, file_meta=file_meta, preamble=b"\0" * 128)

    # Set DICOM metadata
    ds.PatientName = "John Doe"
    ds.PatientID = "123456789"
    ds.PatientBirthDate = "19800101"
    ds.StudyDate = datetime.date.today().strftime("%Y%m%d")
    ds.Modality = "OT"  # Other
    ds.SeriesInstanceUID = generate_uid()
    ds.StudyInstanceUID = generate_uid()
    ds.SOPClassUID = file_meta.MediaStorageSOPClassUID
    ds.SOPInstanceUID = file_meta.MediaStorageSOPInstanceUID
    ds.is_little_endian = True
    ds.is_implicit_VR = False

    # Set pixel data
    ds.Rows, ds.Columns = burned_in_image.shape
    ds.PhotometricInterpretation = "MONOCHROME2"
    ds.SamplesPerPixel = 1
    ds.BitsAllocated = 8
    ds.BitsStored = 8
    ds.HighBit = 7
    ds.PixelRepresentation = 0
    ds.PixelData = burned_in_image.tobytes()

    # Save the DICOM file
    ds.file_meta = file_meta
    ds.save_as(output_filename, write_like_original=False)
    print(f"DICOM file with burned-in PHI saved as {output_filename}")


# Run the function
if __name__ == "__main__":
    create_dicom_with_phi("dicom_with_phi.dcm")
