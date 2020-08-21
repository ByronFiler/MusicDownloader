package MusicDownloader.controllers;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import MusicDownloader.Main;
import MusicDownloader.model.Model;
import MusicDownloader.utils.app.debug;
import MusicDownloader.utils.app.resources;
import MusicDownloader.utils.fx.result;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/*
TODO
 - In future also write actual tests for this
 - Songs could have a button to reacquire if missing?
 */

public class downloads {

    @FXML AnchorPane root;
    @FXML VBox viewContainer;

    @FXML Label eventViewTitle;
    @FXML ComboBox<String> eventViewSelector;
    @FXML ListView<BorderPane> eventsViewTable;

    @FXML ImageView albumViewSelector;
    @FXML ImageView songViewSelector;

    @FXML BorderPane albumViewSelectorWrapper;
    @FXML BorderPane songViewSelectorWrapper;

    ArrayList<BorderPane> downloadHistoriesView = new ArrayList<>();
    ArrayList<BorderPane> downloadHistoriesViewAlbums = new ArrayList<>();

    ArrayList<BorderPane> currentDownloadsView = new ArrayList<>();
    ArrayList<BorderPane> currentDownloadsViewAlbums = new ArrayList<>();

    ArrayList<BorderPane> plannedDownloadsView = new ArrayList<>();
    ArrayList<BorderPane> plannedDownloadsViewAlbums = new ArrayList<>();

    JSONObject metadataReference = new JSONObject();

    @FXML
    private void initialize() {
        final JSONArray[] downloadHistory = {Model.getInstance().download.getDownloadHistory()};
        final JSONArray[] downloadQueue = {Model.getInstance().download.getDownloadQueue()};
        final JSONObject[] downloadObject = {Model.getInstance().download.getDownloadObject()};

        // Check what should be displayed
        if (downloadHistory[0].length() > 0 || downloadQueue[0].length() > 0 || downloadObject[0].length() > 0) {

            // Drawing current downloads if they exist
            if (downloadObject[0].has("metadata")) {

                eventViewSelector.getItems().add("Currently Downloading");
                try {
                    if (
                            !Files.exists(
                                    Paths.get(
                                            String.format(
                                                    "%scached\\%s.jpg",
                                                    resources.applicationData,
                                                    downloadObject[0].getJSONObject("metadata").getString("artId")
                                            )
                                    )
                            )
                    )
                        debug.warn(
                                String.format(
                                        "Failed to used cached resources for album: \"%s\", containing %s song%s.",
                                        downloadObject[0].getJSONObject("metadata").getString("album"),
                                        downloadObject[0].getJSONArray("songs").length(),
                                        downloadObject[0].getJSONArray("songs").length() == 1 ? "" : "s"
                                )
                        );

                    generateCurrent(downloadObject[0]);

                    if (albumViewSelectorWrapper.isVisible())
                        eventsViewTable.getItems().addAll(currentDownloadsViewAlbums);
                    else
                        eventsViewTable.getItems().addAll(currentDownloadsView);

                } catch (JSONException e) {
                    debug.error("Error parsing JSON for download object.", e);
                }

                try {
                    metadataReference = downloadObject[0].getJSONObject("metadata");
                } catch (JSONException e) {
                    debug.error("Failed to load metadata for rendering albums on the fly.", e);
                }
                // TimerTask to update and redraw if necessary
                new Timer().schedule(new TimerTask() {

                    @Override
                    public void run() {

                        // Only handles a single item queued, issue
                        if (Model.getInstance().download.getDownloadObject().toString().equals(new JSONObject().toString())) {
                            debug.trace("All pending downloads completed.");

                            Platform.runLater(() -> {
                                eventsViewTable.getItems().clear();

                                eventViewSelector.getItems().clear();
                                eventViewSelector.setVisible(false);

                                eventViewTitle.setText("Download History");

                                // Only histories should exist logically, hence draw histories only
                                currentDownloadsView.clear();
                                currentDownloadsViewAlbums.clear();

                                downloadHistoriesView.clear();
                                downloadHistoriesViewAlbums.clear();

                                renderDownloadHistory(Model.getInstance().download.getDownloadHistory());
                                if (albumViewSelectorWrapper.getStyleClass().size() > 0) albumsView();
                                else songsView();

                            });

                            this.cancel();

                        } else {

                            try {
                                if (
                                        metadataReference.toString().equals(
                                                Model
                                                        .getInstance()
                                                        .download
                                                        .getDownloadObject()
                                                        .getJSONObject("metadata")
                                                        .toString()
                                        )
                                ) {
                                    for (int i = 0; i < Model.getInstance().download.getDownloadObject().getJSONArray("songs").length(); i++) {

                                        int workingCounter = 0;
                                        try {
                                            for (BorderPane element : currentDownloadsView) {
                                                try {
                                                    if (workingCounter == i && Model.getInstance().download.getDownloadObject().getJSONArray("songs").getJSONObject(i).getBoolean("completed"))
                                                        Platform.runLater(() -> {
                                                            try {
                                                                ((HBox) element.getRight()).getChildren().setAll(
                                                                        new ImageView(
                                                                                new Image(
                                                                                        Main.class.getResource("app/img/tick.png").toURI().toString(),
                                                                                        25,
                                                                                        25,
                                                                                        true,
                                                                                        true
                                                                                )
                                                                        )
                                                                );
                                                            } catch (URISyntaxException ignored) {
                                                            }
                                                        });
                                                } catch (NullPointerException ignored) {
                                                }
                                                workingCounter++;
                                            }
                                        } catch (ConcurrentModificationException ignored) {
                                        }
                                    }

                                    double completed = 0;
                                    for (int i = 0; i < Model.getInstance().download.getDownloadObject().getJSONArray("songs").length(); i++) {
                                        if (Model.getInstance().download.getDownloadObject().getJSONArray("songs").getJSONObject(i).getBoolean("completed"))
                                            completed++;
                                    }

                                    double percentComplete = completed / (double) Model.getInstance().download.getDownloadObject().getJSONArray("songs").length();

                                    // Updating array item and view item (if it is use)
                                    if (((ProgressIndicator) ((HBox) currentDownloadsViewAlbums.get(0).getRight()).getChildren().get(0)).getProgress() != percentComplete)
                                        Platform.runLater(() -> ((ProgressIndicator) ((HBox) currentDownloadsViewAlbums.get(0).getRight()).getChildren().get(0)).setProgress(percentComplete));

                                } else {

                                    debug.trace("Current download completed, switching to next item in queue.");

                                    // Download Object has changed, update the model accordingly
                                    metadataReference = Model.getInstance().download.getDownloadObject().getJSONObject("metadata");

                                    // Clear all ArrayLists (think view should be done automatically, test and see anyway)
                                    downloadObject[0] = Model.getInstance().download.getDownloadObject();
                                    downloadQueue[0] = Model.getInstance().download.getDownloadQueue();
                                    downloadHistory[0] = Model.getInstance().download.getDownloadHistory();

                                    // Build the new current
                                    generateCurrent(downloadObject[0]);
                                    buildDownloadQueueView(downloadQueue[0]);
                                    renderDownloadHistory(downloadHistory[0]);

                                    // Prevent users being left on a blank screen if we run out of queued elements
                                    Platform.runLater(() -> {
                                        if (eventViewSelector.getSelectionModel().getSelectedItem().equals("Download Queue") && plannedDownloadsViewAlbums.size() == 0)
                                            eventViewSelector.getSelectionModel().select(1);

                                        eventViewSelector.getItems().setAll("All", "Currently Downloading", "Download History");
                                        if (plannedDownloadsViewAlbums.size() > 0)
                                            eventViewSelector.getItems().add("Download Queue");

                                        eventsViewTable.getItems().clear();
                                        if (albumViewSelectorWrapper.getStyleClass().size() > 0) albumsView();
                                        else songsView();
                                    });
                                }

                            } catch (JSONException e) {
                                debug.error("Failed to parse JSON to update element result.", e);
                            }

                        }
                    }
                }, 0, 50);

            }

            // Drawing planned downloads if they exist
            if (downloadQueue[0].length() > 0) {

                eventViewSelector.getItems().add("Download Queue");
                try {
                    buildDownloadQueueView(downloadQueue[0]);
                } catch (JSONException e) {
                    debug.error("Failed to parse data to draw planned queue items.", e);
                }
            }

            // Drawing download histories if they exist
            if (downloadHistory[0].length() > 0) {
                eventViewSelector.getItems().add("Download History");
                renderDownloadHistory(downloadHistory[0]);
            }

            // Evaluate whether to decide to hide the current download info-box and the combobox
            if (eventViewSelector.getItems().size() < 2) {

                // Only one thing to show
                eventViewSelector.setVisible(false);

                // Only has one relevant item, hence hide it's self and set the title as the only relevant item
                switch (eventViewSelector.getItems().get(0)) {

                    case "Download History":
                        eventViewTitle.setText("Download History");
                        break;

                    case "Currently Downloading":
                        eventViewTitle.setText("Currently Downloading");
                        break;
                }

            } else {

                eventViewTitle.setText("All");
                eventViewSelector.getItems().add(0, "All");
                eventViewSelector.getSelectionModel().select(0);

            }

        } else {
            debug.warn("Downloads was accessed without any downloads history, downloads in progress or any download queue items, this should not have happened.");
        }

        // Load style
        if (Model.getInstance().settings.getSettingBool("dark_theme")) {
            root.getStylesheets().add(String.valueOf(Main.class.getResource("app/css/dark.css")));
            albumViewSelector.setEffect(new ColorAdjust(0, 0, 1, 0));
            songViewSelector.setEffect(new ColorAdjust(0, 0, 1, 0));
        }

        else root.getStylesheets().add(String.valueOf(Main.class.getResource("app/css/standard.css")));

        if (currentDownloadsView.size() > 0 || downloadHistoriesView.size() > 0) albumsView();
        else defaultView();
        debug.trace("Initialized downloads view.");
    }

    private void buildDownloadQueueView(JSONArray downloadQueue) throws JSONException {

        plannedDownloadsView.clear();
        plannedDownloadsViewAlbums.clear();

        for (int i = 0; i < downloadQueue.length(); i++) {
            checkForCache(downloadQueue, i);

            for (int j = 0; j < downloadQueue.getJSONObject(i).getJSONArray("songs").length(); j++) {

                // Update the table & data
                downloadResult plannedDownloadSongBuilder = new downloadResult(
                        String.format(
                                "%scached\\%s.jpg",
                                resources.applicationData,
                                downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("artId")
                        ),
                        downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("art"),
                        downloadQueue.getJSONObject(i).getJSONArray("songs").getJSONObject(j).getString("title"),
                        downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("artist")
                );
                plannedDownloadSongBuilder.setScheduledDownload();

                plannedDownloadsView.add(plannedDownloadSongBuilder.getView());
            }

            downloadResult plannedDownloadAlbumBuilder = new downloadResult(
                    String.format(
                            "%scached\\%s.jpg",
                            resources.applicationData,
                            downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("artId")
                    ),
                    downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("art"),
                    downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("album"),
                    downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("artist")
            );
            plannedDownloadAlbumBuilder.setScheduledDownload();
            plannedDownloadsViewAlbums.add(plannedDownloadAlbumBuilder.getView());

        }
    }

    private void generateCurrent(JSONObject downloadObject) throws JSONException {
        currentDownloadsView.clear();
        currentDownloadsViewAlbums.clear();

        downloadResult currentDownloadViewAlbumBuilder = new downloadResult(
                String.format(
                        "%scached\\%s.jpg",
                        resources.applicationData,
                        downloadObject.getJSONObject("metadata").getString("artId")
                ),
                downloadObject.getJSONObject("metadata").getString("art"),
                downloadObject.getJSONObject("metadata").getString("album"),
                downloadObject.getJSONObject("metadata").getString("artist")
        );


        double completed = 0;
        for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++)
            if (downloadObject.getJSONArray("songs").getJSONObject(i).getBoolean("completed"))
                completed++;

        currentDownloadViewAlbumBuilder.setCurrentlyDownloading(
                completed / downloadObject.getJSONArray("songs").length()
        );

        currentDownloadsViewAlbums.add(currentDownloadViewAlbumBuilder.getView());

        for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {

            downloadResult currentDownloadViewSongBuilder = new downloadResult(
                    String.format(
                            "%scached\\%s.jpg",
                            resources.applicationData,
                            downloadObject.getJSONObject("metadata").getString("artId")
                    ),
                    downloadObject.getJSONObject("metadata").getString("art"),
                    downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"),
                    downloadObject.getJSONObject("metadata").getString("artist")
            );

            if (downloadObject.getJSONArray("songs").getJSONObject(i).getBoolean("completed"))
                currentDownloadViewSongBuilder.setCompleted();

            else
                currentDownloadViewSongBuilder.setCurrentlyDownloading(-1);

            currentDownloadsView.add(currentDownloadViewSongBuilder.getView());

        }
    }

    private void renderDownloadHistory(JSONArray downloadHistory) {
        downloadHistoriesView.clear();
        downloadHistoriesViewAlbums.clear();

        for (int i = 0; i < downloadHistory.length(); i++) {

            try {
                checkForCache(downloadHistory, i);

                // Generating songs
                buildSongHistory(downloadHistory, i);

                // Generating albums
                downloadResult downloadHistoryAlbumBuilder = new downloadResult(
                        String.format(
                                "%scached\\%s.jpg",
                                resources.applicationData,
                                downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("artId")
                        ),
                        downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("art"),
                        downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("album"),
                        downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("artist")
                );
                downloadHistoryAlbumBuilder.setHistory(
                        downloadHistory.getJSONObject(i),
                        -1
                );
                downloadHistoriesViewAlbums.add(downloadHistoryAlbumBuilder.getView());

            } catch (JSONException e) {
                // Error for now, later handle it and make it a warning
                debug.error("Failed to parse JSON to draw downloads history.", e);
            }

        }
    }

    private void buildSongHistory(JSONArray downloadHistory, int i) throws JSONException {
        for (int j = 0; j < downloadHistory.getJSONObject(i).getJSONArray("songs").length(); j++) {

            downloadResult downloadHistorySongBuilder = new downloadResult(
                    String.format(
                            "%scached\\%s.jpg",
                            resources.applicationData,
                            downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("artId")
                    ),
                    downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("art"),
                    downloadHistory.getJSONObject(i).getJSONArray("songs").getJSONObject(j).getString("title"),
                    downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("artist")
            );
            downloadHistorySongBuilder.setHistory(
                    downloadHistory.getJSONObject(i),
                    j
            );

            downloadHistoriesView.add(downloadHistorySongBuilder.getView());
        }
    }

    private void checkForCache(JSONArray downloadQueue, int i) throws JSONException {
        if (!Files.exists(Paths.get(String.format(resources.applicationData + "cached\\%s.jpg", downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("artId")))))
            debug.warn(
                    String.format(
                            "Failed to used cached resources for album: \"%s\", containing %s song%s.",
                            downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("album"),
                            downloadQueue.getJSONObject(i).getJSONArray("songs").length(),
                            downloadQueue.getJSONObject(i).getJSONArray("songs").length() == 1 ? "" : "s"
                    )
            );
    }

    private void updateViewSelection() {
        switch (eventViewSelector.getSelectionModel().getSelectedItem()) {

            case "All":
                eventsViewTable.getItems().clear();
                eventsViewTable.getItems().addAll(currentDownloadsViewAlbums);
                eventsViewTable.getItems().addAll(plannedDownloadsViewAlbums);
                eventsViewTable.getItems().addAll(downloadHistoriesViewAlbums);
                break;

            case "Currently Downloading":
                eventsViewTable.getItems().setAll(currentDownloadsViewAlbums);
                break;

            case "Download Queue":
                eventsViewTable.getItems().setAll(plannedDownloadsViewAlbums);
                break;

            case "Download History":
                eventsViewTable.getItems().setAll(downloadHistoriesViewAlbums);
                break;

            default:
                debug.error("Unknown combobox option selected: " + eventViewSelector.getSelectionModel().getSelectedItem(), null);
        }
    }

    private void defaultView() {

        albumViewSelectorWrapper.setVisible(false);
        songViewSelectorWrapper.setVisible(false);

        viewContainer.getChildren().clear();
        viewContainer.setAlignment(Pos.CENTER);

        Label defaultMessage = new Label("Files you download appear here");
        defaultMessage.getStyleClass().add("sub_title1");

        ImageView iconImage = new ImageView(
                new Image(
                        Main.class.getResourceAsStream("app/img/icon.png"),
                        50,
                        50,
                        true,
                        true
                )
        );


        if (Model.getInstance().settings.getSettingBool("dark_theme"))
            iconImage.setEffect(new ColorAdjust(0, 0, 1, 0));

        VBox defaultInfoContainer = new VBox(defaultMessage, iconImage);
        defaultInfoContainer.setAlignment(Pos.CENTER);
        defaultInfoContainer.setPadding(new javafx.geometry.Insets(0, 0, 40, 0));

        viewContainer.getChildren().setAll(defaultInfoContainer);

    }

    @FXML
    public void searchView(Event event) {

        // Go to search page
        try {
            Parent searchView = FXMLLoader.load(Main.class.getResource("app/fxml/search.fxml"));
            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(searchView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

        } catch(IOException e) {
            debug.error("FXML Error: search.fxml", e);
        }

    }

    @FXML
    public void albumsView() {
        albumViewSelectorWrapper.getStyleClass().setAll("underline2");
        songViewSelectorWrapper.getStyleClass().setAll();

        if (eventViewSelector.isVisible()) {

            updateViewSelection();

        } else {
            eventsViewTable.getItems().clear();
            eventsViewTable.getItems().addAll(currentDownloadsViewAlbums);
            eventsViewTable.getItems().addAll(downloadHistoriesViewAlbums);
        }

        eventViewSelector.setOnAction(e -> {
            eventViewTitle.setText(eventViewSelector.getSelectionModel().getSelectedItem());
            updateViewSelection();
        });

        debug.trace(
                String.format(
                        "Switched to albums view, displaying %s element%s out of %s (%.0f%%), using \"%s\" view.",
                        eventsViewTable.getItems().size(),
                        eventsViewTable.getItems().size() == 1 ? "" : "s",
                        (currentDownloadsViewAlbums.size() + plannedDownloadsViewAlbums.size() + downloadHistoriesViewAlbums.size()),
                        ((double) eventsViewTable.getItems().size() / (double) (currentDownloadsViewAlbums.size() + plannedDownloadsViewAlbums.size() + downloadHistoriesViewAlbums.size())) * 100,
                        eventViewSelector.isVisible() ? eventViewSelector.getSelectionModel().getSelectedItem() : "All"
                )
        );
    }

    @FXML
    public void songsView() {

        albumViewSelectorWrapper.getStyleClass().setAll();
        songViewSelectorWrapper.getStyleClass().setAll("underline2");

        double usedElements = 0;

        if (eventViewSelector.isVisible()) {
            switch (eventViewSelector.getSelectionModel().getSelectedItem()) {

                case "All":
                    eventsViewTable.getItems().clear();
                    eventsViewTable.getItems().addAll(currentDownloadsView);
                    eventsViewTable.getItems().addAll(plannedDownloadsView);
                    eventsViewTable.getItems().addAll(downloadHistoriesView);
                    usedElements = (currentDownloadsView.size() + plannedDownloadsView.size() + plannedDownloadsView.size());
                    break;

                case "Currently Downloading":
                    eventsViewTable.getItems().setAll(currentDownloadsView);
                    usedElements = currentDownloadsView.size();
                    break;

                case "Download Queue":
                    eventsViewTable.getItems().setAll(plannedDownloadsView);
                    usedElements = plannedDownloadsView.size();
                    break;

                case "Download History":
                    eventsViewTable.getItems().setAll(downloadHistoriesView);
                    usedElements = downloadHistoriesView.size();
                    break;

                default:
                    debug.error("Unknown combobox option selected: " + eventViewSelector.getSelectionModel().getSelectedItem(), null);
            }

        } else {
            eventsViewTable.getItems().clear();
            eventsViewTable.getItems().addAll(currentDownloadsView);
            eventsViewTable.getItems().addAll(plannedDownloadsView);
            eventsViewTable.getItems().addAll(downloadHistoriesView);
            usedElements = currentDownloadsView.size() + plannedDownloadsView.size() + downloadHistoriesView.size();
        }

        eventViewSelector.setOnAction(e -> {
            try {
                eventViewTitle.setText(eventViewSelector.getSelectionModel().getSelectedItem());
                switch (eventViewSelector.getSelectionModel().getSelectedItem()) {

                    case "All":
                        eventsViewTable.getItems().clear();
                        eventsViewTable.getItems().addAll(currentDownloadsView);
                        eventsViewTable.getItems().addAll(plannedDownloadsView);
                        eventsViewTable.getItems().addAll(downloadHistoriesView);
                        break;

                    case "Currently Downloading":
                        eventsViewTable.getItems().setAll(currentDownloadsView);
                        break;

                    case "Downloads Queue":
                        eventsViewTable.getItems().setAll(plannedDownloadsView);
                        break;

                    case "Download History":
                        eventsViewTable.getItems().setAll(downloadHistoriesView);
                        break;

                    default:
                        debug.error("Unknown combobox option selected: " + eventViewSelector.getSelectionModel().getSelectedItem(), null);
                }
            } catch (NullPointerException ignored) {}
        });

        debug.trace(
                String.format(
                        "Switched to songs view, displaying %.0f element%s out of %.0f (%.0f%%), using \"%s\" view.",
                        usedElements,
                        usedElements == 0 ? "" : "s",
                        (double) currentDownloadsView.size() + plannedDownloadsView.size() + downloadHistoriesView.size(),
                        (usedElements / ((double) currentDownloadsView.size() + plannedDownloadsView.size() + downloadHistoriesView.size())) * 100,
                        eventViewSelector.isVisible() ? eventViewSelector.getSelectionModel().getSelectedItem() : "All"

                )
        );

    }

    public class downloadResult extends result {

        public downloadResult(
                String localArtResource,
                String remoteArtResource,
                String title,
                String artist
        ) {
            super(localArtResource, remoteArtResource, false, title, artist);
        }

        public void setCurrentlyDownloading(double progress) {
            right.getChildren().add(new ProgressIndicator(progress));
            view.setRight(right);
        }

        public void setCompleted() {
            try {
                right.getChildren().setAll(
                        new ImageView(
                                new Image(
                                        Main.class.getResource("app/img/tick.png").toURI().toString(),
                                        25,
                                        25,
                                        true,
                                        true
                                )
                        )
                );
                view.setRight(right);
            } catch (URISyntaxException e) {
                debug.error("Failed to load tick for rendering albums.", e);
            }
        }

        public void setScheduledDownload() {

            try {
                ImageView scheduledIcon = new ImageView(
                        new Image(
                                Main.class.getResource("app/img/scheduled.png").toURI().toString(),
                                25,
                                25,
                                true,
                                true
                        )
                );
                scheduledIcon.setEffect(
                        new ColorAdjust(
                                0,
                                0,
                                Model.getInstance().settings.getSettingBool("dark_theme") ? 1 : -1,
                                0
                        )
                );
                right.getChildren().setAll(scheduledIcon);
                view.setRight(right);

            } catch (URISyntaxException e) {
                debug.error("Failed to load path to for scheduled icon.", e);
            }

        }

        public void setHistory(JSONObject downloadObject, int songIndex) throws JSONException {

            // Building the cross
            Line crossLine0 = new Line(20, 0, 0, 20);
            crossLine0.getStyleClass().add("cross-line");

            Line crossLine1 = new Line(20, 20, 0, 0);
            crossLine1.getStyleClass().add("cross-line");

            right.setOnMouseEntered(e -> {
                crossLine0.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.RED : Color.BLACK);
                crossLine1.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.RED : Color.BLACK);
            });
            right.setOnMouseExited(e -> {
                crossLine0.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.rgb(193, 199, 201) : Color.GRAY);
                crossLine1.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.rgb(193, 199, 201) : Color.GRAY);
            });
            right.setCursor(Cursor.HAND);

            // Processing an album history
            if (songIndex == -1) {

                int foundFiles = 0;
                for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {
                    for (String extension : resources.songReferences) {

                        if (
                                Files.exists(
                                        Paths.get(
                                                String.format(
                                                        "%s\\%s.%s",
                                                        downloadObject.getJSONObject("metadata").getString("directory"),
                                                        downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"),
                                                        extension
                                                )
                                        )
                                )
                        )
                            foundFiles++;

                    }
                }

                if (foundFiles == 0) {
                    setSubtext("All Files moved or deleted.");
                    albumArt.setEffect(new ColorAdjust(0, -1, 0, 0));
                    view.setLeft(new HBox(albumArt, leftTextContainer));

                } else {

                    if (foundFiles != downloadObject.getJSONArray("songs").length())
                        setSubtext((downloadObject.getJSONArray("songs").length() - foundFiles) + " Files moved or deleted.");

                    left.setCursor(Cursor.HAND);
                    left.setOnMouseClicked(event -> {
                        try {
                            Desktop.getDesktop().open(new File(downloadObject.getJSONObject("metadata").getString("directory")));
                        } catch (IOException | IllegalArgumentException | JSONException ignored) {
                            left.setCursor(Cursor.DEFAULT);
                            left.setOnMouseClicked(null);

                            albumArt.setEffect(new ColorAdjust(0, -1, 0, 0));
                            setSubtext("All Files moved or deleted.");

                            renderDownloadHistory(Model.getInstance().download.getDownloadHistory());
                        }
                    });

                }

                right.setOnMouseClicked(e -> {
                    downloads.this.eventsViewTable.getItems().remove(view);
                    downloads.this.downloadHistoriesViewAlbums.remove(view);

                    try {
                        for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++)
                            Model.getInstance().download.deleteHistory(downloadObject.getJSONArray("songs").getJSONObject(i));
                    } catch (JSONException er) {
                        debug.error("Failed to parse songs to delete history album.", er);
                    }

                    try {
                        JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
                        downloadHistoriesView.clear();

                        for (int i = 0; i < downloadHistory.length(); i++) buildSongHistory(downloadHistory, i);
                    } catch (JSONException er) {
                        debug.error("Failed to parse JSON to render new download history for songs.", er);
                    }

                    if (eventsViewTable.getItems().size() == 0) Platform.runLater(downloads.this::defaultView);

                });

            } else {

                File foundFile = null;
                for (String extension: resources.songReferences) {
                    File potentialFile = new File(
                            String.format(
                                    "%s\\%s.%s",
                                    downloadObject.getJSONObject("metadata").getString("directory"),
                                    downloadObject.getJSONArray("songs").getJSONObject(songIndex).getString("title"),
                                    extension
                            )
                    );
                    if (potentialFile.exists()) {
                        foundFile = potentialFile;
                        break;
                    }
                }

                if (foundFile == null) {

                    setSubtext("File moved or deleted.");
                    albumArt.setEffect(new ColorAdjust(0, -1, 0, 0));

                    view.setLeft(new HBox(albumArt, leftTextContainer));

                } else {
                    left.setCursor(Cursor.HAND);
                    File finalFoundFile = foundFile;
                    left.setOnMouseClicked(event -> {
                        try {
                            Desktop.getDesktop().open(finalFoundFile);
                        } catch (IOException | IllegalArgumentException ignored) {
                            left.setCursor(Cursor.DEFAULT);
                            left.setOnMouseClicked(null);
                            albumArt.setEffect(new ColorAdjust(0, -1, 0, 0));
                            setSubtext("Files moved or deleted.");

                            // Rebuild song download histories
                            renderDownloadHistory(Model.getInstance().download.getDownloadHistory());

                        }
                    });
                }

                right.setOnMouseClicked(e -> {
                    downloads.this.eventsViewTable.getItems().remove(view);
                    downloads.this.downloadHistoriesViewAlbums.remove(view);

                    try {
                        Model.getInstance().download.deleteHistory(downloadObject.getJSONArray("songs").getJSONObject(songIndex));
                    } catch (JSONException er) {
                        debug.error("Failed to parse songs to delete history album.", er);
                    }


                    try {
                        downloadHistoriesViewAlbums.clear();
                        JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();

                        for (int i = 0; i < downloadHistory.length(); i++) {
                            buildSongHistory(downloadHistory, i);
                        }

                    } catch (JSONException er) {
                        debug.error("Failed to parse JSON to redraw albums.", er);
                    }

                    if (eventsViewTable.getItems().size() == 0) Platform.runLater(downloads.this::defaultView);

                });

            }

            right.getChildren().setAll(new Group(crossLine0, crossLine1));
            view.setRight(right);

        }
    }
}