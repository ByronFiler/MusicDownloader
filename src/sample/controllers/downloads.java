package sample.controllers;

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
import sample.utils.debug;
import sample.model.Model;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ConcurrentModificationException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.IntStream;

public class downloads {

    @FXML AnchorPane root;
    @FXML VBox viewContainer;

    @FXML Label eventViewTitle;
    @FXML ComboBox<String> eventViewSelector;
    @FXML ListView<BorderPane> eventsViewTable;

    @FXML
    private void initialize() {
        JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
        JSONArray downloadQueue = Model.getInstance().download.getDownloadQueue();
        JSONObject downloadObject = Model.getInstance().download.getDownloadObject();

        final BorderPane[][] currentDownloadsView = {new BorderPane[0]};
        BorderPane[] plannedDownloadsView = new BorderPane[0];
        BorderPane[] downloadHistoriesView = new BorderPane[0];

        // Check what should be displayed
        if (downloadHistory.length() > 0 || downloadQueue.length() > 0 || downloadObject.length() > 0) {

            // Drawing current downloads if they exist
            if (downloadObject.has("metadata")) {

                eventViewSelector.getItems().add("Currently Downloading");
                try {
                    currentDownloadsView[0] = new BorderPane[downloadObject.getJSONArray("songs").length()];
                    for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {

                        // Update the table & data
                        eventsViewTable.getItems().add(generateViewResult(generateViewData(downloadObject, i)));
                        currentDownloadsView[0][i] = eventsViewTable.getItems().get(eventsViewTable.getItems().size()-1);

                    }
                } catch (JSONException e) {
                    debug.error(null, "Error parsing JSON for download object.", e);
                }

                // TimerTask to update and redraw if necessary
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {

                        if (Model.getInstance().download.getDownloadObject().toString().equals(new JSONObject().toString())) {
                            debug.trace(Thread.currentThread(), "Detected download completion in view.");
                            Platform.runLater(() -> {
                                eventsViewTable.getItems().clear();
                                eventViewSelector.getItems().clear();
                                initialize();
                            });
                            this.cancel();

                        } else {

                            try {
                                for (int i = 0; i < Model.getInstance().download.getDownloadObject().getJSONArray("songs").length(); i++) {

                                    int workingCounter = 0;

                                    try {
                                        for (BorderPane element : eventsViewTable.getItems()) {

                                            try {
                                                if (element.getId().equals("working")) {
                                                    if (workingCounter == i && Model.getInstance().download.getDownloadObject().getJSONArray("songs").getJSONObject(i).getBoolean("completed"))
                                                        Platform.runLater(() -> {
                                                            try {
                                                                ((HBox) element.getRight()).getChildren().setAll(
                                                                        new ImageView(
                                                                                new Image(
                                                                                        getClass().getResource("../app/img/tick.png").toURI().toString(),
                                                                                        25,
                                                                                        25,
                                                                                        true,
                                                                                        true
                                                                                )
                                                                        )
                                                                );
                                                            } catch (URISyntaxException e) {
                                                                e.printStackTrace();
                                                            }
                                                        });
                                                }
                                            } catch (NullPointerException ignored) {
                                            }
                                            workingCounter++;

                                        }
                                    } catch (ConcurrentModificationException ignored) {
                                    }
                                }
                            } catch (JSONException e) {
                                debug.error(Thread.currentThread(), "Failed to parse JSON to update element result.", e);
                            }
                        }
                    }
                }, 0, 50);

            }

            // Drawing planned downloads if they exist
            if (downloadQueue.length() > 0) {
                eventViewSelector.getItems().add("Download Queue");
                try {
                    plannedDownloadsView = new BorderPane[
                        (int) IntStream.of(
                                IntStream
                                        .range(0, downloadQueue.length())
                                        .toArray()
                        ).mapToLong(i -> {
                            try {
                                return downloadQueue.getJSONObject(i).getJSONArray("songs").length();
                            } catch (JSONException ignored) {}
                                return 0;
                            }
                        ).sum()];
                    int k = 0;
                    for (int i = 0; i < downloadQueue.length(); i++) {
                        for (int j = 0; j < downloadQueue.getJSONObject(i).getJSONArray("songs").length(); j++) {

                            // Update the table & data
                            eventsViewTable.getItems().add(generateViewResult(generateViewData(downloadQueue.getJSONObject(i), j)));
                            plannedDownloadsView[k] = eventsViewTable.getItems().get(eventsViewTable.getItems().size()-1);
                            k++;
                        }

                    }
                } catch (JSONException e) {
                    debug.error(null, "Failed to parse data to draw planned queue items.", e);
                }
            }

            // Drawing download histories if they exist
            if (downloadHistory.length() > 0) {
                eventViewSelector.getItems().add("Download History");
                downloadHistoriesView = new BorderPane[downloadHistory.length()];
                for (int i = 0; i < downloadHistory.length(); i++) {

                    try {
                        // Update table & data
                        eventsViewTable.getItems().add(generateViewResult(downloadHistory.getJSONObject(i)));
                        downloadHistoriesView[i] = eventsViewTable.getItems().get(eventsViewTable.getItems().size()-1);

                    } catch (JSONException e) {
                        // Error for now, later handle it and make it a warning
                        debug.error(null, "Failed to parse JSON to draw downloads history.", e);
                    }

                }
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

                // Handle changes
                BorderPane[] finalCurrentDownloadsView = currentDownloadsView[0];
                BorderPane[] finalPlannedDownloadsView = plannedDownloadsView;
                BorderPane[] finalDownloadHistoriesView = downloadHistoriesView;
                eventViewSelector.setOnAction(e -> {
                    try {
                        eventViewTitle.setText(eventViewSelector.getSelectionModel().getSelectedItem());
                        switch (eventViewSelector.getSelectionModel().getSelectedItem()) {

                            case "All":
                                eventsViewTable.getItems().clear();
                                eventsViewTable.getItems().addAll(finalCurrentDownloadsView);
                                eventsViewTable.getItems().addAll(finalPlannedDownloadsView);
                                eventsViewTable.getItems().addAll(finalDownloadHistoriesView);
                                break;

                            case "Currently Downloading":
                                eventsViewTable.getItems().setAll(finalCurrentDownloadsView);
                                break;

                            case "Downloads Queue":
                                eventsViewTable.getItems().setAll(finalPlannedDownloadsView);
                                break;

                            case "Download History":
                                eventsViewTable.getItems().setAll(finalDownloadHistoriesView);
                                break;

                            default:
                                debug.error(null, "Unknown combobox option selected: " + eventViewSelector.getSelectionModel().getSelectedItem(), null);
                        }
                    } catch (NullPointerException ignored) {}
                });

            }

        } else {
            debug.warn(null, "Downloads was accessed without any downloads history, downloads in progress or any download queue items, this should not have h../appened.");
        }

        // Load style
        if (Model.getInstance().settings.getSettingBool("dark_theme"))
            root.getStylesheets().add(String.valueOf(getClass().getResource("../app/css/dark.css")));

        else
            root.getStylesheets().add(String.valueOf(getClass().getResource("../app/css/standard.css")));


        debug.trace(null, "Initialized downloads view.");

    }

    @FXML
    public void searchView(Event event) {

        // Go to search page
        try {
            Parent searchView = FXMLLoader.load(getClass().getResource("../app/fxml/search.fxml"));
            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(searchView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

        } catch(IOException e) {
            debug.error(null, "FXML Error: search.fxml", e);
        }

    }

    private JSONObject generateViewData(JSONObject source, int index) throws JSONException {

        JSONObject viewData = new JSONObject();
        viewData.put("artId", source.getJSONObject("metadata").getString("artId"));
        viewData.put("artist", source.getJSONObject("metadata").getString("artist"));
        viewData.put("id", source.getJSONArray("songs").getJSONObject(index).getString("id"));
        viewData.put("title", source.getJSONArray("songs").getJSONObject(index).getString("title"));
        viewData.put("directory", source.getJSONObject("metadata").getString("directory"));
        viewData.put("artUrl", source.getJSONObject("metadata").getString("art")); // error
        viewData.put("completed", source.getJSONArray("songs").getJSONObject(index).getBoolean("completed"));

        return viewData;

    }

    private BorderPane generateViewResult(JSONObject viewData) throws JSONException{

        BorderPane result = new BorderPane();
        result.setCursor(Cursor.HAND);

        // Left: Album Art, Song Title, Artist, Status & Padding
        HBox left = new HBox();

        // Preparing album art image: Cached Resource > Online Resource > Default
        ImageView albumArt = new ImageView();

        if (Files.exists(Paths.get(String.format("usr\\cached\\%s.jpg", viewData.getString("artId"))))) {

            // Cached album art exists, use that
            albumArt.setImage(
                    new Image(
                            new File(String.format("usr\\cached\\%s.jpg", viewData.getString("artId"))).toURI().toString(),
                            85,
                            85,
                            true,
                            true
                    )
            );

        } else {

            debug.warn(Thread.currentThread(), "Failed to use cached resource.");

            // Sending the request causes lag, hence use as thread, only needs to be called once, in future can add network error handling but that seems excessive as of the moment
            try {
                Thread loadAlbumArt = new Thread(() -> {

                    // Setting as default until it loads (or doesn't)
                    try {
                        albumArt.setImage(
                                new Image(
                                        getClass().getResource("../app/img/song_default.png").toURI().toString(),
                                        85,
                                        85,
                                        true,
                                        true
                                )
                        );
                    } catch (URISyntaxException er) {
                        debug.error(null, "Failed to set default album art.", er);
                    }

                    try {
                        if (InetAddress.getByName("allmusic.com").isReachable(1000)) {
                            albumArt.setImage(
                                    new Image(
                                            viewData.getString("artUrl"),
                                            85,
                                            85,
                                            true,
                                            true
                                    )
                            );
                        }
                    } catch (IOException e) {
                        debug.warn(null, "Failed to connect to allmusic to get album art, using default.");
                    } catch (JSONException e) {
                        debug.error(null, "Failed to get art for loading resource.", e);
                    }
                }, "load-art");
                loadAlbumArt.setDaemon(true);
                loadAlbumArt.start();

            } catch (IndexOutOfBoundsException e) {
                debug.warn(null, "Internal error loading album art.");
            }
        }

        // Greyscale if downloaded & files don't exist
        if (!Files.exists(Paths.get(viewData.getString("directory"))) && !viewData.has("completed")) {
            // Greyscale the album art
            albumArt.setEffect(new ColorAdjust(0, -1, 0, 0));

            // Use default cursor as directory can't be opened
            result.setCursor(Cursor.DEFAULT);
        }

        BorderPane resultInformationContainer = new BorderPane();

        Label title = new Label(viewData.getString("title"));
        title.getStyleClass().add("sub_title1");

        Label artist = new Label(viewData.getString("artist"));
        artist.getStyleClass().add("sub_title2");

        VBox songArtistContainer = new VBox(title, artist);
        songArtistContainer.setAlignment(Pos.TOP_LEFT);

        resultInformationContainer.setTop(songArtistContainer);
        resultInformationContainer.setPadding(new Insets(0, 0, 0, 5));

        left.getChildren().addAll(albumArt, resultInformationContainer);

        // Right: Icon
        HBox right = new HBox();
        // Determine if this is a completed download
        if (viewData.has("completed")) {

            // This is a scheduled or completed download
            if (viewData.get("completed") == JSONObject.NULL) {

                // Queued in the future, not current in progress for a download
                try {
                    right.getChildren().add(
                            new ImageView(
                                    new Image(
                                            getClass().getResource("../app/img/icon.png").toURI().toString(),
                                            25,
                                            25,
                                            true,
                                            true
                                    )
                            )
                    );
                } catch (URISyntaxException ignored) {}



            } else {

                result.setId("working");

                if (viewData.getBoolean("completed")) {

                    // In queue and downloaded (Green Tick)
                    try {
                        right.getChildren().add(
                                new ImageView(
                                        new Image(
                                                getClass().getResource("../app/img/tick.png").toURI().toString(),
                                                25,
                                                25,
                                                true,
                                                true
                                        )
                                )
                        );
                    } catch (URISyntaxException ignored) {}

                } else {

                    // In queue, not downloaded (ProgressIndicator (Indeterminate))
                    right.getChildren().add(new ProgressIndicator());

                }
            }

        } else {

            // This is a history result, should have a box to delete the history item

            Line crossLine0 = new Line(20, 0, 0, 20);
            crossLine0.getStyleClass().add("cross-line");

            Line crossLine1 = new Line(20, 20, 0, 0);
            crossLine1.getStyleClass().add("cross-line");

            Group crossBox = new Group(crossLine0, crossLine1);

            crossBox.setOnMouseEntered(e -> {
                crossLine0.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.RED : Color.BLACK);
                crossLine1.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.RED : Color.BLACK);
            });

            crossBox.setOnMouseExited(e -> {
                crossLine0.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.rgb(193, 199, 201) : Color.GRAY);
                crossLine1.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.rgb(193, 199, 201) : Color.GRAY);
            });

            crossBox.setOnMouseClicked(event -> {

                Model.getInstance().download.deleteHistory(viewData);

                try {
                    Parent settingsView = FXMLLoader.load(getClass().getResource("../app/fxml/downloads.fxml"));
                    Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    mainWindow.setScene(new Scene(settingsView, mainWindow.getWidth() - 16, mainWindow.getHeight() - 39));
                } catch (IOException er) {
                    debug.error(null, "FXML Error: Downloads.fxml", er);
                }

            });
            result.setOnMouseClicked(event -> {
                try {
                    Desktop.getDesktop().open(new File(viewData.getString("directory")));
                } catch (IOException | JSONException | IllegalArgumentException ignored) {
                    result.setCursor(Cursor.DEFAULT);
                    result.setOnMouseClicked(null);
                }
            });
            right.getChildren().add(crossBox);

        }
        right.setPadding(new Insets(0, 10, 0, 0));
        right.setAlignment(Pos.CENTER);
        right.setMaxWidth(40);

        result.setLeft(left);
        result.setRight(right);
        result.getStyleClass().add("result");

        return result;

    }

}
