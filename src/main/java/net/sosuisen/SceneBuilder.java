package net.sosuisen;

import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Builder for constructing a JavaFX Scene from specified FXML and CSS files.
 * <p>
 * Example:
 * 
 * <pre>
 * var scene = SceneBuilder.fromFxml("/com/example/main.fxml")
 *         .css("/com/example/common.css")
 *         .css("/com/example/dashboard.css")
 *         .resources("com.example.i18n.Messages", Locale.getDefault())
 *         .build();
 * </pre>
 */
public class SceneBuilder {
    private final URL fxmlURL;
    private List<String> cssURLs = new ArrayList<>();
    private int width = -1;
    private int height = -1;
    private Object[] ctrlConstructorParams;
    private ResourceBundle resources;

    /**
     * Creates a new SceneBuilder instance from the specified FXML resource name.
     * 
     * @param resourceName the path to the FXML resource
     * @return a new SceneBuilder instance
     * @throws IllegalArgumentException if the resource is not found
     */
    public static SceneBuilder fromFxml(String resourceName) {
        var url = SceneBuilder.class.getResource(resourceName);
        if (url == null) {
            throw new IllegalArgumentException("FXML resource not found: " + resourceName);
        }
        return new SceneBuilder(url);
    }

    /**
     * Creates a new SceneBuilder instance from the specified FXML URL.
     * 
     * @param fxmlURL the URL of the FXML file
     * @return a new SceneBuilder instance
     */
    public static SceneBuilder fromFxml(URL fxmlURL) {
        return new SceneBuilder(Objects.requireNonNull(fxmlURL, "Url must not be null."));
    }

    private SceneBuilder(URL fxmlURL) {
        this.fxmlURL = Objects.requireNonNull(fxmlURL, "fxmlURL must not be null.");
    }

    /**
     * Specifies the CSS resource name.
     * If multiple CSS files are needed, call this method multiple times.
     * 
     * @param resourceName the path to the CSS resource
     * @return this builder
     * @throws IllegalArgumentException if the resource is not found
     */
    public SceneBuilder css(String resourceName) {
        var cssURL = SceneBuilder.class.getResource(resourceName);
        if (cssURL == null) {
            throw new IllegalArgumentException("CSS resource not found: " + resourceName);
        }
        cssURLs.add(cssURL.toExternalForm());
        return this;
    }

    /**
     * Specifies the CSS URL.
     * If multiple CSS files are needed, call this method multiple times.
     * 
     * @param url the URL of the CSS file
     * @return this builder
     */
    public SceneBuilder css(URL url) {
        cssURLs.add(Objects.requireNonNull(url, "Url must not be null.").toExternalForm());
        return this;
    }

    /**
     * Specifies the constructor arguments for the controller.
     * 
     * @param constructorArgs the constructor arguments for the controller
     * @return this builder
     */
    public SceneBuilder newController(Object... constructorArgs) {
        ctrlConstructorParams = constructorArgs;
        return this;
    }

    /**
     * Specifies the size of the Scene.
     * If not set, the Scene will use the size of the root container.
     * 
     * @param width  the width
     * @param height the height
     * @return this builder
     */
    public SceneBuilder size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Specifies the ResourceBundle for internationalization.
     * 
     * @param ResourceBundle the ResourceBundle to use
     * @return this builder
     */
    public SceneBuilder resources(ResourceBundle resources) {
        this.resources = resources;
        return this;
    }

    /**
     * Specifies the ResourceBundle for internationalization.
     * 
     * @param baseName the base name of the resource bundle
     * @param locale   the locale for the resource bundle
     * @return this builder
     */
    public SceneBuilder resources(String baseName, Locale locale) {
        resources = ResourceBundle.getBundle(baseName, locale);
        return this;
    }

    /**
     * Builds the Scene.
     * 
     * @return the constructed Scene
     * @throws IOException if loading the FXML fails
     */
    public Scene build() throws IOException {
        var loader = resources != null
                ? new FXMLLoader(fxmlURL, resources)
                : new FXMLLoader(fxmlURL);

        if (ctrlConstructorParams != null && ctrlConstructorParams.length > 0) {
            loader.setControllerFactory(controllerClass -> {
                // This lambda is a factory that instantiates the controller class when the root
                // container node in main.fxml includes an fx:controller attribute.
                // controllerClass refers to the class specified by the fx:controller attribute.
                var paramTypes = new Class<?>[ctrlConstructorParams.length];
                for (int i = 0; i < ctrlConstructorParams.length; i++) {
                    paramTypes[i] = ctrlConstructorParams[i] != null
                            ? ctrlConstructorParams[i].getClass()
                            : Object.class; // If null, use Object.class to avoid NullPointerException.
                }
                // Retrieve a constructor that accepts paramTypes as parameters,
                // and then use it to create an instance.
                try {
                    return controllerClass.getDeclaredConstructor(paramTypes)
                            .newInstance(ctrlConstructorParams);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create controller instance for "
                            + controllerClass.getName(), e);
                }
            });
        }

        Scene scene;
        try {
            scene = (width < 0 || height < 0)
                    ? new Scene(loader.load())
                    : new Scene(loader.load(), width, height);
        } catch (javafx.fxml.LoadException e) {
            // Set more informative message
            throw new javafx.fxml.LoadException("Failed to load FXML from " + fxmlURL, e);
        }

        if (!cssURLs.isEmpty()) {
            scene.getStylesheets().addAll(cssURLs);
        }
        return scene;
    }
}
