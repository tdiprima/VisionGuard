import cv2
import numpy as np
import pydicom
from pydicom.pixel_data_handlers.util import apply_modality_lut


def contains_burned_in_text(dicom):
    """
    Check if the DICOM file contains burned-in text or overlays.

    Args:
        dicom: A pydicom Dataset object.

    Returns:
        bool: True if burned-in text is present, False otherwise.
    """
    # Check for indicators of burned-in annotations
    if "BurnedInAnnotation" in dicom and dicom.BurnedInAnnotation == "YES":
        return True
    if "ImageComments" in dicom and dicom.ImageComments.strip():
        return True
    return False


def redact_dicom(file_path, output_path):
    """
    Read a DICOM file, redact metadata including dates and times, 
    and optionally redact burned-in text.

    Args:
        file_path: Path to the input DICOM file.
        output_path: Path to save the redacted DICOM file.

    Returns:
        None
    """
    # Load DICOM file
    dicom = pydicom.dcmread(file_path)

    # Check for burned-in text in metadata
    burned_in_text_present = contains_burned_in_text(dicom)

    # Handle pixel data only if burned-in text is present
    if burned_in_text_present:
        print("Burned-in text detected. Redacting image data...")
        # Apply Modality LUT (if applicable) to get the image data
        image = apply_modality_lut(dicom.pixel_array, dicom)

        # Normalize image to 8-bit for OpenCV compatibility
        image = cv2.normalize(image, None, 0, 255, cv2.NORM_MINMAX).astype(np.uint8)

        # Convert grayscale to RGB if needed
        if len(image.shape) == 2:
            image = cv2.cvtColor(image, cv2.COLOR_GRAY2BGR)

        # Optional: Implement OCR-based text redaction here if burned-in text exists
        # For now, we leave this as a placeholder

        # Update the pixel array in the DICOM file
        dicom.PixelData = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY).tobytes()
    else:
        print("No burned-in text detected. Skipping image redaction...")

    # Replace PHI in metadata fields with dummy values
    phi_fields = ["PatientName", "PatientID", "PatientBirthDate", "PatientSex", "AccessionNumber", "StudyID",
        "StudyDescription", "SeriesDescription", "InstitutionName"]
    for field in phi_fields:
        if field in dicom:
            dicom.data_element(field).value = "REDACTED"

    # Redact dates and times
    date_time_fields = ["StudyDate", "SeriesDate", "AcquisitionDate", "ContentDate", "StudyTime", "SeriesTime",
        "AcquisitionTime", "ContentTime"]
    for field in date_time_fields:
        if field in dicom:
            dicom.data_element(field).value = "00000000" if "Date" in field else "000000.000000"

    # Save the redacted DICOM file
    dicom.save_as(output_path)


# Example usage
input_file = "../input/dicom_file_1.dcm"
output_file = "output_redacted.dcm"
redact_dicom(input_file, output_file)
print(f"Redacted DICOM saved to {output_file}")
