package net.sosuisen;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Model {
    private StringProperty pdfTitleText = new SimpleStringProperty();
    private StringProperty outputHintText = new SimpleStringProperty();
    private StringProperty folderNameText = new SimpleStringProperty();

    public StringProperty pdfTitleTextProperty() {
        return pdfTitleText;
    }

    public StringProperty outputHintTextProperty() {
        return outputHintText;
    }

    public StringProperty folderNameTextProperty() {
        return folderNameText;
    }
}
