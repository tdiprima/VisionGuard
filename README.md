# VisionGuard

VisionGuard is a Java-based tool for detecting and protecting sensitive text in images, such as Protected Health Information (PHI). It uses OCR tools like Tesseract and supports extensibility with Java Service Provider Interface (SPI).

## Features

- Detect text regions in images and extract corresponding text.
- Mask sensitive information in images.
- Modular architecture with SPI for adding custom detectors.

## Usage

### Prerequisites
- Java 8 or later
- Maven
- [Tesseract OCR](https://github.com/tesseract-ocr/tessdata)

### Setup and Run

1. Install Tesseract:

   ```sh
   # Install Tesseract
   brew install tesseract
   ```

2. Build the project:

   ```sh
   mvn clean package
   ```

3. Run the JAR:

   ```sh
   java -jar target/VisionGuard-1.0-jar-with-dependencies.jar
   ```

   <!-- cp /usr/local/lib/libtesseract.dylib /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/lib/ -->

3. Ensure you configure Tesseract's data path and language:

   ```java
   detector.setupParameters("/path/to/tessdata", "eng");

   // Example:
   detector.setupParameters("/usr/share/tesseract-ocr/4.00/tessdata", "eng");
   ```

## Contributing

Contributions are welcome. Fork the repo, create a branch, make your changes, and open a pull request.

## License

Licensed under the [MIT License](LICENSE).

<br>
