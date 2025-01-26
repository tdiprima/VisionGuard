import pydicom


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
    if "BurnedInAnnotation" in dicom and dicom.BurnedInAnnotation == "YES":
        print(
            "Burned-in text detected. You may want to redact image data.")  # Handle redaction of burned-in text if applicable

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
input_file = "input.dcm"
output_file = "output_redacted.dcm"
redact_dicom(input_file, output_file)
print(f"Redacted DICOM saved to {output_file}")
