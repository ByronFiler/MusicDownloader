package musicdownloader.controllers;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import musicdownloader.Main;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.fx.Result;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.IntStream;

/*
TODO
 - Songs could have a button to reacquire if missing?
 - Clickable area should be the entire result, the X should have a wide enough area and consume event to prevent both being done
 */

public class Downloads {

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

    Timer uiUpdater = new Timer();

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
                                                    "%scached/%s.jpg",
                                                    Resources.getInstance().getApplicationData(),
                                                    downloadObject[0].getJSONObject("metadata").getString("artId")
                                            )
                                    )
                            )
                    )
                        Debug.warn(
                                String.format(
                                        "Failed to used cached resources for album: \"%s\", containing %s song%s.",
                                        downloadObject[0].getJSONObject("metadata").getString("album"),
                                        downloadObject[0].getJSONArray("songs").length(),
                                        downloadObject[0].getJSONArray("songs").length() == 1 ? "" : "s"
                                )
                        );

                    generateCurrent(downloadObject[0]);

                    if (albumViewSelectorWrapper.isVisible()) eventsViewTable.getItems().addAll(currentDownloadsViewAlbums);
                    else eventsViewTable.getItems().addAll(currentDownloadsView);

                } catch (JSONException e) {
                    Debug.error("Error parsing JSON for download object.", e);
                }

                try {
                    metadataReference = downloadObject[0].getJSONObject("metadata");
                } catch (JSONException e) {
                    Debug.error("Failed to load metadata for rendering albums on the fly.", e);
                }
                // TimerTask to update and redraw if necessary
                uiUpdater.schedule(new TimerTask() {

                    @Override
                    public void run() {

                        // Only handles a single item queued, issue
                        if (Model.getInstance().download.getDownloadObject().toString().equals(new JSONObject().toString())) {

                            Debug.trace("All pending downloads completed.");

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
                                        for (BorderPane element : currentDownloadsView) {
                                            try {
                                                if (workingCounter == i && Model.getInstance().download.getDownloadObject().getJSONArray("songs").getJSONObject(i).getBoolean("completed") && element.getId().equals("working"))
                                                    Platform.runLater(() -> {
                                                        try {
                                                            ((HBox) element.getRight()).getChildren().setAll(
                                                                    new ImageView(
                                                                            new Image(
                                                                                    Main.class.getResource("resources/img/tick.png").toURI().toString(),
                                                                                    25,
                                                                                    25,
                                                                                    true,
                                                                                    true
                                                                            )
                                                                    )
                                                            );
                                                            element.setId("completed");
                                                        } catch (URISyntaxException e) {
                                                            Debug.warn("Failed to load tick for view.");
                                                        }
                                                    });
                                                workingCounter++;
                                            } catch (NullPointerException e) {
                                                Debug.log(Thread.currentThread(), "NullPointerException occurred in view, suppressed.");
                                            }
                                        }
                                    }

                                    double completed = 0;
                                    for (int i = 0; i < Model.getInstance().download.getDownloadObject().getJSONArray("songs").length(); i++)
                                        if (Model.getInstance().download.getDownloadObject().getJSONArray("songs").getJSONObject(i).getBoolean("completed"))
                                            completed++;

                                    // Calculate time remaining and update

                                    double percentComplete = completed / (double) Model.getInstance().download.getDownloadObject().getJSONArray("songs").length();

                                    // Updating array item and view item (if it is use)
                                    if (((ProgressIndicator) ((HBox) currentDownloadsViewAlbums.get(0).getRight()).getChildren().get(0)).getProgress() != percentComplete)
                                        Platform.runLater(() -> ((ProgressIndicator) ((HBox) currentDownloadsViewAlbums.get(0).getRight()).getChildren().get(0)).setProgress(percentComplete));

                                } else {
                                    Debug.trace("Current download completed, switching to next item in queue.");

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
                                Debug.error("Failed to parse JSON to update element result.", e);
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
                    Debug.error("Failed to parse data to draw planned queue items.", e);
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

        }

        // Load style
        if (Model.getInstance().settings.getSettingBool("dark_theme")) {
            root.getStylesheets().add(String.valueOf(Main.class.getResource("resources/css/dark.css")));
            albumViewSelector.setEffect(new ColorAdjust(0, 0, 1, 0));
            songViewSelector.setEffect(new ColorAdjust(0, 0, 1, 0));
        }

        else root.getStylesheets().add(String.valueOf(Main.class.getResource("resources/css/standard.css")));

        if (currentDownloadsView.size() > 0 || downloadHistoriesView.size() > 0) albumsView();
        else defaultView();

        Debug.trace("Initialized downloads view.");
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
                                "%scached/%s.jpg",
                                Resources.getInstance().getApplicationData(),
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
                            "%scached/%s.jpg",
                            Resources.getInstance().getApplicationData(),
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
                        "%scached/%s.jpg",
                        Resources.getInstance().getApplicationData(),
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
                            "%scached/%s.jpg",
                            Resources.getInstance().getApplicationData(),
                            downloadObject.getJSONObject("metadata").getString("artId")
                    ),
                    downloadObject.getJSONObject("metadata").getString("art"),
                    downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"),
                    downloadObject.getJSONObject("metadata").getString("artist")
            );

            if (downloadObject.getJSONArray("songs").getJSONObject(i).getBoolean("completed"))
                currentDownloadViewSongBuilder.setCompleted();


            else currentDownloadViewSongBuilder.setCurrentlyDownloading(-1);

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
                                "%scached/%s.jpg",
                                Resources.getInstance().getApplicationData(),
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
                Debug.error("Failed to parse JSON to draw downloads history.", e);
            }

        }
    }

    private void buildSongHistory(JSONArray downloadHistory, int i) throws JSONException {
        for (int j = 0; j < downloadHistory.getJSONObject(i).getJSONArray("songs").length(); j++) {

            downloadResult downloadHistorySongBuilder = new downloadResult(
                    String.format(
                            "%scached/%s.jpg",
                            Resources.getInstance().getApplicationData(),
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
        if (!Files.exists(Paths.get(String.format(Resources.getInstance().getApplicationData() + "cached/%s.jpg", downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("artId")))))
            Debug.warn(
                    String.format(
                            "Failed to used cached resources for album: \"%s\", containing %s song%s.",
                            downloadQueue.getJSONObject(i).getJSONObject("metadata").getString("album"),
                            downloadQueue.getJSONObject(i).getJSONArray("songs").length(),
                            downloadQueue.getJSONObject(i).getJSONArray("songs").length() == 1 ? "" : "s"
                    )
            );
    }

    private void updateViewSelection() {
        if (eventViewSelector.getItems().size() > 2)
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
                    Debug.error("Unknown combobox option selected: " + eventViewSelector.getSelectionModel().getSelectedItem(), null);
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
                        Main.class.getResourceAsStream("resources/img/icon.png"),
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
            uiUpdater.cancel();
            Parent searchView = FXMLLoader.load(Main.class.getResource("resources/fxml/search.fxml"));
            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(searchView, mainWindow.getWidth() - Resources.getInstance().getWindowResizeWidth(), mainWindow.getHeight() - Resources.getInstance().getWindowResizeHeight()));

        } catch(IOException e) {
            Debug.error("FXML Error: search.fxml", e);
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

        Debug.trace(
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
                    Debug.error("Unknown combobox option selected: " + eventViewSelector.getSelectionModel().getSelectedItem(), null);
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
                        Debug.error("Unknown combobox option selected: " + eventViewSelector.getSelectionModel().getSelectedItem(), null);
                }
            } catch (NullPointerException ignored) {}
        });

        Debug.trace(
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

    public class downloadResult extends Result {

        private historyController historyController = null;

        public downloadResult(
                String localArtResource,
                String remoteArtResource,
                String title,
                String artist
        ) {
            super(localArtResource, remoteArtResource, false, title, artist);
        }

        public void setCurrentlyDownloading(double progress) {
            ProgressIndicator progressView = new ProgressIndicator(progress);

            // Hide the % complete message under it
            if (progress > 0) progressView.getStyleClass().add("progress-indicator-percentage");

            right.getChildren().add(progressView);
            view.setRight(right);
            view.setId("working");
        }

        public void setCompleted() {
            try {
                right.getChildren().setAll(
                        new ImageView(
                                new Image(
                                        Main.class.getResource("resources/img/tick.png").toURI().toString(),
                                        25,
                                        25,
                                        true,
                                        true
                                )
                        )
                );
                view.setRight(right);
            } catch (URISyntaxException e) {
                Debug.error("Failed to load tick for rendering albums.", e);
            }
        }

        public void setScheduledDownload() {

            try {
                ImageView scheduledIcon = new ImageView(
                        new Image(
                                Main.class.getResource("resources/img/scheduled.png").toURI().toString(),
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
                Debug.error("Failed to load path to for scheduled icon.", e);
            }

        }

        public void setHistory(JSONObject downloadObject, int songIndex) throws JSONException {
            this.historyController = new historyController(downloadObject, songIndex);
            view.setRight(historyController.getView());
        }

        protected class historyController {

            private final HBox view = new HBox();

            private final Line crossLine0 = new Line(20, 0, 0, 20);
            private final Line crossLine1 = new Line(20, 20, 0, 0);

            private final downloadResult parent = downloadResult.this;

            private final JSONObject downloadObject;
            private final int songIndex;

            private final ArrayList<File> foundFiles = new ArrayList<>();
            private File viewFile = null;

            public historyController(JSONObject downloadObject, int songIndex) throws JSONException {

                this.downloadObject = downloadObject;
                this.songIndex = songIndex;

                // View Initialisation
                crossLine0.getStyleClass().add("cross-line");
                crossLine1.getStyleClass().add("cross-line");

                view.setAlignment(Pos.CENTER);
                view.setCursor(Cursor.HAND);
                view.setPadding(new Insets(0, 10, 0, 0));
                view.setOnMouseEntered(this::selectCross);
                view.setOnMouseExited(this::unselectCross);

                // Check if: song doesn't exist or all songs in album don't exist
                double foundFiles = 0;
                if ((songIndex != -1 && checkFile(songIndex) == 0)
                        || (songIndex == -1 && (foundFiles = IntStream.range(0, downloadObject.getJSONArray("songs").length()).mapToDouble(this::checkFile).sum()) == 0))
                {
                    parent.title.getStyleClass().add("sub_title1_strikethrough");
                    parent.albumArt.setEffect(new ColorAdjust(0, -1, 0, 0));
                    parent.setSubtext( (songIndex == -1 ? "All Files" : "File") + " Moved or Deleted.");

                } else {

                    double missingFiles;
                    if (songIndex == -1 && foundFiles < downloadObject.getJSONArray("songs").length() ) {

                        parent.setSubtext(
                                String.format(
                                        "%.0f File%s Moved or Deleted.",
                                        (missingFiles = downloadObject.getJSONArray("songs").length() - foundFiles),
                                        missingFiles == 1 ? "" : "s"
                                )
                        );

                    }

                    // Active Files
                    viewFile = this.foundFiles.size() > 1 ? new File(downloadObject.getJSONObject("metadata").getString("directory")) : this.foundFiles.get(0);

                    parent.left.setOnMouseEntered(this::selectTitle);
                    parent.left.setOnMouseExited(this::unselectTitle);

                    parent.view.setOnMouseClicked(this::openFiles);
                    parent.view.setCursor(Cursor.HAND);
                }

                view.setOnMouseClicked(this::clearHistory);
                view.getChildren().setAll(new Group(crossLine0, crossLine1));

            }

            public synchronized HBox getView() {
                return view;
            }

            private synchronized void selectCross(MouseEvent e) {
                crossLine0.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.RED : Color.BLACK);
                crossLine1.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.RED : Color.BLACK);
            }

            private synchronized void unselectCross(MouseEvent e) {
                crossLine0.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.rgb(193, 199, 201) : Color.GRAY);
                crossLine1.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.rgb(193, 199, 201) : Color.GRAY);
            }

            private synchronized void selectTitle(MouseEvent e) {
                parent.title.getStyleClass().add("sub_title1_selected");
            }

            private synchronized void unselectTitle(MouseEvent e) {
                parent.title.getStyleClass().remove("sub_title1_selected");
            }

            private synchronized void openFiles(MouseEvent event) {

                if (event.getButton() == MouseButton.PRIMARY)
                    try {
                        Desktop.getDesktop().open(Objects.requireNonNull(viewFile));
                    } catch (IOException | IllegalArgumentException ignored) {

                        parent.view.setCursor(Cursor.DEFAULT);
                        parent.view.setOnMouseClicked(null);
                        parent.albumArt.setEffect(new ColorAdjust(0, -1, 0, 0));
                        parent.setSubtext("Files moved or deleted.");

                        parent.title.getStyleClass().add("sub_title1_strikethrough");

                        // Rebuild song download histories
                        Downloads.this.renderDownloadHistory(Model.getInstance().download.getDownloadHistory());

                        event.consume();

                    } catch (NullPointerException e) {
                        Debug.error("Attempted to open file which is not yet definitely, should have been completed in initialisation.", e);
                    }

            }

            private synchronized void clearHistory(MouseEvent event) {

                Downloads.this.eventsViewTable.getItems().remove(parent.view);

                try {
                    for (int i = 0; i < (songIndex == -1 ? downloadObject.getJSONArray("songs").length() : 1); i++) {
                        Model.getInstance().download.deleteHistory(downloadObject.getJSONArray("songs").getJSONObject(songIndex == -1 ? i : songIndex));
                    }
                } catch (JSONException e) {
                    Debug.error("Failed to parse JSON to remove download history from the model.", e);
                }

                Downloads.this.renderDownloadHistory(Model.getInstance().download.getDownloadHistory());

                if (eventsViewTable.getItems().size() == 0) Platform.runLater(Downloads.this::defaultView);
                event.consume();

            }

            private synchronized int checkFile(int songIndex) {

                try {
                    String filePath;
                    for (String extension : Resources.songReferences) {
                        filePath = String.format(
                                "%s/%s.%s",
                                downloadObject.getJSONObject("metadata").getString("directory"),
                                downloadObject.getJSONArray("songs").getJSONObject(songIndex).getString("title").replaceAll("[\u0000-\u001f<>:\"/\\\\|?*\u007f]+", "_"),
                                extension
                        );
                        if (Files.exists(Paths.get(filePath))) {
                            foundFiles.add(new File(filePath));
                            return 1;
                        }


                    }
                } catch (JSONException e) {
                    Debug.error("Failed check file based on JSON history.", e);
                }

                return 0;

            }

        }
    }
}
