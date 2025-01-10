<!-- cp /usr/local/lib/libtesseract.dylib /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/lib/ -->
# VisionGuard

VisionGuard is a Java-based tool for detecting and protecting sensitive text in images, such as Protected Health Information (PHI). It uses OCR tools like Tesseract and supports extensibility with Java Service Provider Interface (SPI). 

This tool is ideal for applications in healthcare, compliance, and data redaction systems.

## Features

- Detect and extract text regions from images.
- Mask sensitive information (e.g., PHI) in images.
- Modular architecture with SPI for adding custom detectors.
- Cross-validation between Tesseract OCR and llama3.2-vision model.
- Generates detailed reports highlighting discrepancies between detectors.

## Usage

### Prerequisites
- Java 8 or later
- Maven
- [Tesseract OCR](https://github.com/tesseract-ocr/tessdata)
- Llama 3.2 Vision model (optional, for cross-validation)

### Setup and Run

1. Install Tesseract:

   ```sh
   brew install tesseract
   ```

2. Build the project:

   ```sh
   mvn clean install
   ```

3. Run the program:

   ```sh
   Usage: java VisionGuard <imagePath> <action> <outputPath> <reportPath>
   Actions: OUTLINE, MASK, MOVE_TO_FOLDER, QUARANTINE
   Optional parameters:
     --minWidth=X         Minimum width of bounding boxes
     --minHeight=Y        Minimum height of bounding boxes
     --maxWidth=A         Maximum width of bounding boxes
     --maxHeight=B        Maximum height of bounding boxes
     --quarantinePath=path   Specify quarantine folder (action=QUARANTINE)
     --moveToFolderPath=path Specify move-to-folder path (action=MOVE_TO_FOLDER)
   ```

   Using maven:

   ```sh
   mvn clean package
   java -jar target/VisionGuard-1.0-jar-with-dependencies.jar /images/example.png OUTLINE output.png report.txt
   ```

5. Ensure you configure Tesseract's data path and language in the code:

   ```java
   detector.setupParameters("/usr/local/Cellar/tesseract/5.5.0/share/tessdata", "eng");
   ```

### Supported Actions
- **OUTLINE**: Draw bounding boxes around detected text.
- **MASK**: Mask detected text regions in the image.
- **MOVE\_TO\_FOLDER**: For general output organization, e.g., grouping all processed images into an output folder.
- **QUARANTINE**: To specifically identify and separate suspect or problematic images, such as those containing sensitive data (PHI) or OCR anomalies.

### Input Requirements
- Supported image formats: PNG, JPEG
- Recommended image resolution: 300 DPI for best OCR performance.

## Tools

`parse_response_log.py`

A Python script to help interpret and analyze results from the Ollama vision model. This tool is optional but can aid in debugging and understanding model outputs.


## Contributing

Contributions are welcome! Fork the repo, create a branch, make your changes, and open a pull request.

## License

Licensed under the [MIT License](LICENSE).

<br>
