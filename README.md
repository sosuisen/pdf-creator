# PDF Creator

A JavaFX application that combines image files from a specified folder into a single PDF file, arranged in filename order.

## Features

- Image sizes are preserved.
- The title is embedded as a PDF property.

## Development Requirements

- Java 21 or later
- Maven 3.6 or later

## Building and Running

### Run the application

```bash
mvn javafx:run
```

### Package as executable

```bash
mvn clean package
```

The packaged application will be available in `target/jpackage/`.

## Usage

1. Launch the application
2. Enter a title for your PDF in the text field
3. Click "Select Folder" to choose a directory containing images
4. Click "Create PDF" to generate the PDF file
5. The PDF will be saved in the selected folder with the specified title
