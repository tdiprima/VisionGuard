import cv2
import numpy as np
import pydicom
from pydicom.pixel_data_handlers.util import apply_modality_lut


def detect_and_redact_text(image):
    """
    Detect and redact text from an image using OpenCV.

    Args:
        image: Input image as a NumPy array.

    Returns:
        Redacted image as a NumPy array.
    """
    # Convert to grayscale for text detection
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Use OpenCV's thresholding to highlight text regions
    _, binary = cv2.threshold(gray, 200, 255, cv2.THRESH_BINARY_INV)

    # Find contours for text regions
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    # Iterate through contours and redact detected text regions
    for contour in contours:
        x, y, w, h = cv2.boundingRect(contour)
        # Redact by filling with black (0) or white (255)
        cv2.rectangle(image, (x, y), (x + w, y + h), (0, 0, 0), thickness=-1)

    return image


def redact_dicom(file_path, output_path):
    """
    Read a DICOM file, redact burned-in text, and replace PHI in metadata.

    Args:
        file_path: Path to the input DICOM file.
        output_path: Path to save the redacted DICOM file.

    Returns:
        None
    """
    # Load DICOM file
    dicom = pydicom.dcmread(file_path)

    # Apply Modality LUT (if applicable) to get the image data
    image = apply_modality_lut(dicom.pixel_array, dicom)

    # Normalize image to 8-bit for OpenCV compatibility
    image = cv2.normalize(image, None, 0, 255, cv2.NORM_MINMAX).astype(np.uint8)

    # Convert grayscale to RGB if needed
    if len(image.shape) == 2:
        image = cv2.cvtColor(image, cv2.COLOR_GRAY2BGR)

    # Detect and redact text
    redacted_image = detect_and_redact_text(image)

    # Update the pixel array in the DICOM file
    dicom.PixelData = redacted_image.tobytes()

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
