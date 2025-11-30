package net.sosuisen;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import javafx.concurrent.Task;
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
    private TextField pdfTitleLabel;

    @FXML
    private Button selectFolderButton;

    @FXML
    private Label folderNameLabel;

    @FXML
    private Button createPdfButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label outputHintLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressMessageLabel;

    private Model model;
    private Task<Void> currentTask;

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
        cancelButton.setOnAction(e -> onCancelPdf());

        createPdfButton.disableProperty().bind(
                Bindings.isEmpty(model.pdfTitleTextProperty())
                        .or(Bindings.isEmpty(model.folderNameTextProperty())));
        outputHintLabel.visibleProperty().bind(
                (Bindings.isEmpty(model.pdfTitleTextProperty())
                        .or(Bindings.isEmpty(model.folderNameTextProperty()))).not());

        model.outputHintTextProperty().bind(
                Bindings.concat("作成されるPDFファイル：\n",
                        model.folderNameTextProperty(),
                        "\\",
                        model.pdfTitleTextProperty(),
                        ".pdf"));
        outputHintLabel.textProperty().bind(model.outputHintTextProperty());

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
        cancelButton.setVisible(true);

        currentTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                File folder = new File(folderPath);
                List<File> imageFiles = getImageFiles(folder);

                if (imageFiles.isEmpty()) {
                    System.out.println("No image files found in the selected folder.");
                    throw new IllegalArgumentException("選択したフォルダに画像ファイルが見つかりません。");
                }

                PDDocument document = new PDDocument();

                // Set PDF document properties
                PDDocumentInformation info = document.getDocumentInformation();
                info.setTitle(pdfTitle);

                int totalImages = imageFiles.size();
                for (int i = 0; i < totalImages; i++) {
                    // Check for cancellation
                    if (isCancelled()) {
                        document.close();
                        return null;
                    }

                    File imageFile = imageFiles.get(i);

                    // Update progress
                    updateMessage("Processing image " + (i + 1) + " of " + totalImages + ": " + imageFile.getName());
                    updateProgress(i, totalImages);

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
                updateMessage("Saving PDF...");
                updateProgress(totalImages, totalImages);

                String pdfFileName = pdfTitle.endsWith(".pdf") ? pdfTitle : pdfTitle + ".pdf";
                File pdfFile = new File(folder, pdfFileName);
                document.save(pdfFile);
                document.close();

                System.out.println("PDF created successfully: " + pdfFile.getAbsolutePath());
                return null;
            }

            @Override
            protected void succeeded() {
                progressBar.setVisible(false);
                progressMessageLabel.setVisible(false);
                cancelButton.setVisible(false);
                currentTask = null;
                System.out.println("PDF creation completed successfully");

                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("PDF作成完了");
                alert.setHeaderText(null);
                alert.setContentText("できました！");
                alert.showAndWait();

                // Re-bind the disable property
                createPdfButton.disableProperty().bind(
                        Bindings.isEmpty(model.pdfTitleTextProperty())
                                .or(Bindings.isEmpty(model.folderNameTextProperty())));
            }

            @Override
            protected void failed() {
                progressBar.setVisible(false);
                progressMessageLabel.setVisible(false);
                cancelButton.setVisible(false);
                currentTask = null;
                Throwable exception = getException();
                System.err.println("Error creating PDF: " + exception.getMessage());
                exception.printStackTrace();

                // Show error alert
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("PDF作成エラー");
                alert.setHeaderText(null);

                if (exception instanceof IllegalArgumentException &&
                        exception.getMessage().contains("画像ファイルが見つかりません")) {
                    alert.setAlertType(Alert.AlertType.WARNING);
                    alert.setContentText(
                            "選択したフォルダに画像ファイル（.jpg, .jpeg, .png, .bmp, .gif）が見つかりません。\n画像ファイルが含まれているフォルダを選択してください。");
                } else {
                    alert.setAlertType(Alert.AlertType.ERROR);
                    alert.setContentText("PDFファイルの作成中にエラーが発生しました：\n" + exception.getMessage());
                }

                alert.showAndWait();

                // Re-bind the disable property
                createPdfButton.disableProperty().bind(
                        Bindings.isEmpty(model.pdfTitleTextProperty())
                                .or(Bindings.isEmpty(model.folderNameTextProperty())));
            }

            @Override
            protected void cancelled() {
                progressBar.setVisible(false);
                progressMessageLabel.setVisible(false);
                cancelButton.setVisible(false);
                currentTask = null;
                // Re-bind the disable property
                createPdfButton.disableProperty().bind(
                        Bindings.isEmpty(model.pdfTitleTextProperty())
                                .or(Bindings.isEmpty(model.folderNameTextProperty())));
            }
        };

        // Bind progress bar and message to the task
        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressMessageLabel.textProperty().bind(currentTask.messageProperty());

        // Show progress components
        progressBar.setVisible(true);
        progressMessageLabel.setVisible(true);

        Thread taskThread = new Thread(currentTask);
        taskThread.setDaemon(true);
        taskThread.start();
    }

    @FXML
    private void onCancelPdf() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
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
