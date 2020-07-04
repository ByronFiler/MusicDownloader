package sample;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class downloads {

    @FXML VBox downloadInfo;

    @FXML
    private void initialize(Event event) {

        // Check what should be displayed
        if (Model.getInstance().download.getDownloadHistory().length() > 0 || Model.getInstance().download.getDownloadQueue().length() > 0 || Model.getInstance().download.getDownloadObject().length() > 0) {

            if (Model.getInstance().download.getDownloadObject().length() > 0) {

                // Draw the current download information data


            } else {

                // No current download only view the history
                downloadInfo.setVisible(false);

            }

            // Prepare combobox relative to the data available [All (Default), Planned Download Queue, Download Object, Download History]


            // Prepare list view: default, all

        } else {
            Debug.warn(null, "Downloads was accessed without any downloads history, downloads in progress or any download queue items, this should not have happened.");
            searchView(event);
        }

    }

    // Consider moving to a different file for switching views
    @FXML
    private void searchView(Event event) {

        // Go to search page
        try {
            Parent searchView = FXMLLoader.load(getClass().getResource("app/fxml/search.fxml"));

            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(searchView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

        } catch(IOException e) {
            Debug.error(null, "FXML Error: search.fxml", e.getCause());
        }

    }

}
