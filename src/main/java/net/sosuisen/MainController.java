package net.sosuisen;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Controller for the main.fxml
 * 
 * This class must be specified as the fx:controller in main.fxml.
 */
public class MainController {
    @FXML
    private Label processingLabel;

    @FXML
    private TextField pdfTitleLabel;

    @FXML
    private Button selectFolderButton;

    @FXML
    private Label folderNameLabel;

    @FXML
    private Button createPdfButton;

    @FXML
    private Label outputHintLabel;

    private Model model;

    private final Map<String, String> processingMessages = new HashMap<>() {
        {
            put("PROCESSING", "処理中..");
            put("COMPLETED", "できました!");
            put("ERROR", "失敗しました..");
            put("EMPTY", "");
        }
    };

    public MainController(Model model) {
        // Notice that @FXML-annotated fields (e.g., messageLabel) have not been loaded
        // yet.
        this.model = model;
    }

    @FXML
    private void initialize() {
        pdfTitleLabel.textProperty().bindBidirectional(model.pdfTitleTextProperty());
        folderNameLabel.textProperty().bind(model.folderNameTextProperty());

        selectFolderButton.setOnAction(e -> onSelectFolder());

        createPdfButton.setOnAction(e -> onCreatePdf());

        createPdfButton.disableProperty().bind(
                Bindings.isEmpty(model.pdfTitleTextProperty())
                        .or(Bindings.isEmpty(model.folderNameTextProperty())));
        outputHintLabel.visibleProperty().bind(
                (Bindings.isEmpty(model.pdfTitleTextProperty())
                        .or(Bindings.isEmpty(model.folderNameTextProperty()))).not());

        model.outputHintTextProperty().bind(
                Bindings.concat("作成されるPDFファイル：",
                        model.folderNameTextProperty(),
                        "\\",
                        model.pdfTitleTextProperty(),
                        ".pdf"));
        outputHintLabel.textProperty().bind(model.outputHintTextProperty());

        processingLabel.setText(processingMessages.get("EMPTY"));

        // Clear processing message when title or folder changes
        model.pdfTitleTextProperty().addListener((obs, oldVal, newVal) -> {
            processingLabel.setText(processingMessages.get("EMPTY"));
        });

        model.folderNameTextProperty().addListener((obs, oldVal, newVal) -> {
            processingLabel.setText(processingMessages.get("EMPTY"));
        });

    }

    @FXML
    private void onSelectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");

        Stage stage = (Stage) selectFolderButton.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            model.folderNameTextProperty().set(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void onCreatePdf() {
        String pdfTitle = model.pdfTitleTextProperty().get();
        String folderPath = model.folderNameTextProperty().get();

        // Unbind and disable button during processing, show processing label
        createPdfButton.disableProperty().unbind();
        createPdfButton.setDisable(true);
        processingLabel.setText(processingMessages.get("PROCESSING"));

        CompletableFuture.runAsync(() -> {
            try {
                createPdfFromImages(folderPath, pdfTitle);
                Platform.runLater(() -> {
                    processingLabel.setText(processingMessages.get("COMPLETED"));
                    System.out.println("PDF creation completed successfully");
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    processingLabel.setText(processingMessages.get("ERROR"));
                    System.err.println("Error creating PDF: " + e.getMessage());
                    e.printStackTrace();
                });
            } finally {
                Platform.runLater(() -> {
                    // Re-bind the disable property
                    createPdfButton.disableProperty().bind(
                            Bindings.isEmpty(model.pdfTitleTextProperty())
                                    .or(Bindings.isEmpty(model.folderNameTextProperty())));

                });
            }
        });
    }

    private void createPdfFromImages(String folderPath, String pdfTitle) throws IOException {
        File folder = new File(folderPath);
        List<File> imageFiles = getImageFiles(folder);

        if (imageFiles.isEmpty()) {
            System.out.println("No image files found in the selected folder.");
            return;
        }

        PDDocument document = new PDDocument();

        // Set PDF document properties
        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle(pdfTitle);
        info.setCreator("PDF Creator Application");

        for (File imageFile : imageFiles) {
            PDImageXObject image = PDImageXObject.createFromFile(imageFile.getAbsolutePath(), document);

            // Create a page with the same size as the image
            PDRectangle pageSize = new PDRectangle(image.getWidth(), image.getHeight());
            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            // Add the image to the page
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.drawImage(image, 0, 0);
            contentStream.close();
        }

        // Save the PDF
        String pdfFileName = pdfTitle.endsWith(".pdf") ? pdfTitle : pdfTitle + ".pdf";
        File pdfFile = new File(folder, pdfFileName);
        document.save(pdfFile);
        document.close();

        System.out.println("PDF created successfully: " + pdfFile.getAbsolutePath());
    }

    private List<File> getImageFiles(File folder) {
        String[] imageExtensions = { ".jpg", ".jpeg", ".png", ".bmp", ".gif" };

        return Arrays.stream(folder.listFiles())
                .filter(File::isFile)
                .filter(file -> {
                    String fileName = file.getName().toLowerCase();
                    return Arrays.stream(imageExtensions)
                            .anyMatch(fileName::endsWith);
                })
                .sorted(this::naturalCompare)
                .collect(Collectors.toList());
    }

    private int naturalCompare(File f1, File f2) {
        String name1 = f1.getName();
        String name2 = f2.getName();

        Pattern pattern = Pattern.compile("(\\d+)|(\\D+)");
        Matcher matcher1 = pattern.matcher(name1);
        Matcher matcher2 = pattern.matcher(name2);

        while (matcher1.find() && matcher2.find()) {
            String part1 = matcher1.group();
            String part2 = matcher2.group();

            // If both parts are numbers, compare numerically
            if (part1.matches("\\d+") && part2.matches("\\d+")) {
                int num1 = Integer.parseInt(part1);
                int num2 = Integer.parseInt(part2);
                int result = Integer.compare(num1, num2);
                if (result != 0)
                    return result;
            } else {
                // Otherwise, compare lexicographically (case-insensitive)
                int result = part1.compareToIgnoreCase(part2);
                if (result != 0)
                    return result;
            }
        }

        // If one string is longer, the shorter one comes first
        return Integer.compare(name1.length(), name2.length());
    }
}
