package musicdownloader.controllers;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;

import java.io.IOException;
import java.util.Arrays;
import java.util.ResourceBundle;

public class Container {

    @FXML
    private StackPane controllerContainer;

    @FXML
    private BorderPane viewContainer;

    @FXML
    private ImageView searchIcon;

    @FXML
    private ImageView converterIcon;

    @FXML
    private ImageView downloaderIcon;

    @FXML
    private ImageView settingsIcon;

    @FXML
    private Label searchText;

    @FXML
    private Label converterText;

    @FXML
    private Label downloaderText;

    @FXML
    private Label settingsText;

    private ImageView[] icons;
    private Label[] links;

    private int selectedIndex = 0;

    private final ColorAdjust defaultIconAdjustment = new ColorAdjust(
            0,
            0,
            Model.getInstance().settings.getSettingBool("dark_theme") ? 0.75 : 0.5,
            0
    );
    private final ColorAdjust colourReset = new ColorAdjust(
            -1,
            -1,
            Model.getInstance().settings.getSettingBool("dark_theme") ? 1 : -1,
            -1
    );

    @FXML
    private void initialize() {

        icons = new ImageView[]{searchIcon, converterIcon, downloaderIcon, settingsIcon};
        links = new Label[]{searchText, converterText, downloaderText, settingsText};

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getClassLoader().getResource("fxml/search2.fxml"),
                    ResourceBundle.getBundle("locale.search")
            );

            controllerContainer.getChildren().add(loader.load());

            viewContainer.getStylesheets().add(
                    String.valueOf(
                            getClass()
                                    .getClassLoader()
                                    .getResource("css/" + (
                                            Model
                                                    .getInstance()
                                                    .settings
                                                    .getSettingBool("dark_theme")
                                                    ? "dark" : "standard"
                                    ) + ".css")
                    )
            );

            // Color Correct Icons
            Arrays.asList(icons).forEach(icon -> icon.setEffect(defaultIconAdjustment));

            // Selecting search
            searchIcon.setEffect(colourReset);
            searchText.getStyleClass().remove("sub_title1_unselected");
            searchText.getStyleClass().add("sub_title1");

            selectedIndex = 0;

        } catch (IOException e) {
            Debug.error("FXML Error", e);
        }

    }

    // Clicks
    @FXML
    private void searchView(Event e) {

        selectedIndex = 0;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getClassLoader().getResource("fxml/search2.fxml"),
                    ResourceBundle.getBundle("locale.search")
            );

            controllerContainer.getChildren().clear();
            controllerContainer.getChildren().add(loader.load());

        } catch (IOException er) {
            Debug.error("FXML Error", er);
        }

        updateUi();
        Debug.trace("Search View");

    }

    @FXML
    private void converterView(Event e) {

        selectedIndex = 1;

        updateUi();
        Debug.trace("Converter view");

    }

    @FXML
    private void downloadsView(Event e) {

        selectedIndex = 2;

        updateUi();
        Debug.trace("Downloads view");

    }

    @FXML
    private void settingsView(Event e) {

        selectedIndex = 3;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getClassLoader().getResource("fxml/settings2.fxml"),
                    ResourceBundle.getBundle("locale.settings")
            );

            controllerContainer.getChildren().clear();
            controllerContainer.getChildren().add(loader.load());
        } catch (IOException er) {
            er.printStackTrace();
        }

        updateUi();
        Debug.trace("Settings view");

    }

    // Hovers
    @FXML
    private synchronized void selectSearchView(Event e) {
        updateUi(0);
        Debug.trace("Selected search view");
    }

    @FXML
    private synchronized void selectConverterView(Event e) {
        updateUi(1);
        Debug.trace("Selected convert view");
    }

    @FXML
    private synchronized void selectDownloadsView(Event e) {
        updateUi(2);
        Debug.trace("Selected downloads view");
    }

    @FXML
    private synchronized void selectSettingsView(Event e) {
        updateUi(3);
        Debug.trace("Selected search view");
    }

    // Un-hover
    @FXML
    private void unSelectSearchView(Event e) {

        updateUi();
        Debug.trace("Unselected search view");
    }

    @FXML
    private void unSelectConverterView(Event e) {

        updateUi();
        Debug.trace("Unselected convert view");
    }

    @FXML
    private void unSelectDownloadsView(Event e) {

        updateUi();
        Debug.trace("Unselected downloads view");
    }

    @FXML
    private void unSelectSettingsView(Event e) {

        updateUi();
        Debug.trace("Unselected search view");
    }

    private synchronized void updateUi(int hoverIndex) {

        Arrays.asList(icons).forEach(icon ->
            icon.setEffect(
                    (Arrays.asList(icons).indexOf(icon) == selectedIndex ||  Arrays.asList(icons).indexOf(icon) == hoverIndex)
                            ? colourReset : defaultIconAdjustment
            )
        );

        for (int i = 0; i < links.length; i++) {
            links[i].getStyleClass().set(1, (i == selectedIndex || i == hoverIndex) ? "sub_title1" : "sub_title1_unselected");
        }

    }

    private void updateUi() {
        updateUi(selectedIndex);
    }

}
