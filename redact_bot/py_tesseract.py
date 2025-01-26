import cv2
import numpy as np
import pydicom
import pytesseract
from pydicom.pixel_data_handlers.util import apply_modality_lut


# Ensure Tesseract is installed and accessible
# Install it from: https://github.com/tesseract-ocr/tesseract
# Python binding: pip install pytesseract

def redact_burned_in_text(image):
    """
    Detect and redact text regions using Tesseract OCR.

    Args:
        image: Input image as a NumPy array.

    Returns:
        Redacted image as a NumPy array.
    """
    # Convert to grayscale for OCR
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Use Tesseract to detect text regions
    # Get bounding boxes for detected text
    data = pytesseract.image_to_data(gray, output_type=pytesseract.Output.DICT)

    for i in range(len(data["text"])):
        if int(data["conf"][i]) > 60:  # Confidence threshold
            x, y, w, h = data["left"][i], data["top"][i], data["width"][i], data["height"][i]
            # Redact the detected text region
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

    # Redact burned-in text
    redacted_image = redact_burned_in_text(image)

    # Update the pixel array in the DICOM file
    dicom.PixelData = cv2.cvtColor(redacted_image, cv2.COLOR_BGR2GRAY).tobytes()

    # Replace PHI in metadata fields with dummy values
    phi_fields = ["PatientName", "PatientID", "PatientBirthDate", "PatientSex", "AccessionNumber", "StudyID",
        "StudyDescription", "SeriesDescription", "InstitutionName"]
    for field in phi_fields:
        if field in dicom:
            dicom.data_element(field).value = "REDACTED"

    # Save the redacted DICOM file
    dicom.save_as(output_path)


# Example usage
input_file = "../input/decompressed.dcm"
output_file = "output_redacted.dcm"
redact_dicom(input_file, output_file)
print(f"Redacted DICOM saved to {output_file}")
