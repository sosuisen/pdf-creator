package net.sosuisen;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * JavaFX MVC(Model-View-Controller) application
 */
public class App extends Application {

    /**
     * Called when the application is started.
     * 
     * @param stage the primary stage for this application
     */
    @Override
    public void start(Stage stage) {
        // Model
        var model = new Model();

        // View
        showMainWindow(stage, model);
    }

    /**
     * Shows the main window of the application.
     * 
     * @param stage the primary stage
     * @param model the application model
     */
    private void showMainWindow(Stage stage, Model model) {
        try {
            // A controller class must be specified in fx:controller of main.fxml.
            var scene = SceneBuilder.fromFxml("main.fxml")
                    .css("style.css")
                    // The parameters of newController must match
                    // the constructor parameters of the controller class.
                    .newController(model)
                    .build();
            stage.setScene(scene);
            stage.setTitle("PDF Creator");
            stage.show();
        } catch (Exception e) {
            showStartupErrorAndExit(e);
        }
    }

    /**
     * Displays an error dialog and exits the application if startup fails.
     * 
     * @param e the exception that occurred during startup
     */
    private void showStartupErrorAndExit(Exception e) {
        e.printStackTrace();

        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Startup Error");
        alert.setHeaderText("An error occurred during startup");
        alert.getDialogPane().setExpandableContent(new Label(e.getMessage()));
        alert.getDialogPane().setExpanded(true);
        alert.setOnHidden(event -> Platform.exit());
        alert.show();
    }
}
