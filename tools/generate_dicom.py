"""
Creates two DICOM files in an output_dicom_files directory. Each file contains
a 512x512 image with dummy PHI, metadata, and zero-filled pixel data.
"""
import os
from datetime import datetime

import numpy as np
import pydicom
from pydicom.dataset import FileDataset
from pydicom.uid import ExplicitVRLittleEndian, generate_uid
from PIL import Image, ImageDraw, ImageFont


def create_dicom_file(filename, patient_name, patient_id, patient_sex, patient_dob):
    # Create a blank image (grayscale)
    rows, cols = 512, 512
    blank_image = np.zeros((rows, cols), dtype=np.uint8)

    # Convert to a PIL image to add text (burned-in PHI)
    pil_img = Image.fromarray(blank_image)
    draw = ImageDraw.Draw(pil_img)
    font = ImageFont.load_default()

    # Add dummy PHI (burned-in text)
    phi_text = [
        f"Name: {patient_name}",
        f"DOB: {patient_dob}",
        f"ID: {patient_id}",
        f"Sex: {patient_sex}",
        f"Exam Date: {datetime.now().strftime('%Y-%m-%d')}"
    ]
    y = 10
    for line in phi_text:
        draw.text((10, y), line, fill=255, font=font)
        y += 15

    # Convert back to NumPy array
    burned_in_image = np.array(pil_img)

    # Create file meta information
    file_meta = pydicom.dataset.FileMetaDataset()
    file_meta.MediaStorageSOPClassUID = pydicom.uid.SecondaryCaptureImageStorage
    file_meta.MediaStorageSOPInstanceUID = generate_uid()
    file_meta.TransferSyntaxUID = ExplicitVRLittleEndian

    # Create a DICOM dataset
    ds = FileDataset(filename, {}, file_meta=file_meta, preamble=b"\0" * 128)

    # Set creation date and time
    dt = datetime.now()
    ds.ContentDate = dt.strftime("%Y%m%d")
    ds.ContentTime = dt.strftime("%H%M%S")

    # Set dummy PHI
    ds.PatientName = patient_name
    ds.PatientID = patient_id
    ds.PatientBirthDate = patient_dob
    ds.PatientSex = patient_sex

    # Set general DICOM metadata
    ds.Modality = "OT"  # Other
    ds.StudyInstanceUID = generate_uid()
    ds.SeriesInstanceUID = generate_uid()
    ds.SOPInstanceUID = generate_uid()
    ds.StudyID = "12345"
    ds.SeriesNumber = "1"
    ds.InstanceNumber = "1"

    # Set pixel data
    ds.Rows, ds.Columns = burned_in_image.shape
    ds.BitsAllocated = 8
    ds.BitsStored = 8
    ds.HighBit = 7
    ds.PixelRepresentation = 0
    ds.SamplesPerPixel = 1
    ds.PhotometricInterpretation = "MONOCHROME2"

    # Ensure pixel data matches the specified attributes
    expected_size = ds.Rows * ds.Columns * (ds.BitsAllocated // 8)
    actual_size = len(burned_in_image.tobytes())
    if actual_size != expected_size:
        raise ValueError(f"Pixel data size mismatch: expected {expected_size} bytes, got {actual_size} bytes")

    ds.PixelData = burned_in_image.tobytes()

    # Save the DICOM file
    ds.save_as(filename, write_like_original=False)
    print(f"DICOM file '{filename}' created successfully.")


if __name__ == "__main__":
    output_dir = "output_dicom_files"
    os.makedirs(output_dir, exist_ok=True)

    # Create first DICOM file
    create_dicom_file(
        filename=os.path.join(output_dir, "dicom_file_1.dcm"),
        patient_name="John^Doe",
        patient_id="123456",
        patient_sex="M",
        patient_dob="19900101")

    # Create second DICOM file
    create_dicom_file(
        filename=os.path.join(output_dir, "dicom_file_2.dcm"),
        patient_name="Jane^Doe",
        patient_id="654321",
        patient_sex="F",
        patient_dob="20010101")
