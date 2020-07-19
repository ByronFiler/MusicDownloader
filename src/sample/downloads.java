package sample;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.*;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ConcurrentModificationException;
import java.util.OptionalDouble;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.IntStream;

// TODO: Fix lag when loading in
// TODO: TimeTask to auto update the completion icon

public class downloads {

    @FXML VBox viewContainer;

    @FXML Text eventViewTitle;
    @FXML ComboBox<String> eventViewSelector;
    @FXML ListView<BorderPane> eventsViewTable;

    @FXML VBox textInfoContainer;
    @FXML Text processing;
    @FXML Text downloadSpeed;
    @FXML Text eta;

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
                    Debug.error(null, "Error parsing JSON for download object.", e.getCause());
                }

                // TimerTask to update and redraw if necessary
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {

                        if (Model.getInstance().download.getDownloadObject().toString().equals(new JSONObject().toString())) {
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
                                                                                        getClass().getResource("app/img/tick.png").toURI().toString(),
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
                                Debug.error(Thread.currentThread(), "Failed to parse JSON to update element result.", e.getCause());
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
                    Debug.error(null, "Failed to parse data to draw planned queue items.", e.getCause());
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
                        Debug.error(null, "Failed to parse JSON to draw downloads history.", e.getCause());
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
                                Debug.error(null, "Unknown combobox option selected: " + eventViewSelector.getSelectionModel().getSelectedItem(), null);
                        }
                    } catch (NullPointerException ignored) {}
                });

            }

            // Handling view to show download speed, eta, etc.
            if (!Model.getInstance().download.getDownloadInfo().toString().equals(new JSONObject().toString()) && currentDownloadsView[0].length > 0) {

                final JSONObject[] workingData = {new JSONObject()};
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {

                        // New data received, redrawing
                        if (!Model.getInstance().download.getDownloadInfo().toString().equals(workingData[0].toString())) {

                            // Begin drawing with loaded data
                            try {
                                workingData[0] = Model.getInstance().download.getDownloadInfo();

                                // UI Text
                                downloadSpeed.setText(workingData[0].getString("downloadSpeed"));

                                eta.setText(workingData[0].getString("eta"));

                                processing.setText(workingData[0].getString("song"));

                                // processing.setWrappingWidth(INTERNAL CONTAINER WIDTH - (INFO MESSAGE + SPACING));
                                textInfoContainer.prefWidthProperty().bind( ((BorderPane) viewContainer.getChildren().get(0)).prefHeightProperty().divide(2).subtract(20));

                                // Preparing the data
                                JSONArray chartData = workingData[0].getJSONArray("seriesData");

                                // Calculating max point
                                double minCalculator = 0;
                                OptionalDouble minCalculatorOpt = IntStream.range(0, chartData.length()).mapToDouble(i -> {
                                    try {
                                        return chartData.getJSONObject(i).getInt("speed");
                                    } catch (JSONException e) {
                                        Debug.error(null, "Missing data in working data.", e.getCause());
                                    }
                                    return 0;
                                }).min();

                                if (minCalculatorOpt.isPresent())
                                    minCalculator = minCalculatorOpt.getAsDouble();
                                else
                                    Debug.error(null, "Failed to get maximum value from given data.", null);

                                int conversion;
                                // Surely there is a better way to do this?
                                if (minCalculator > 1024 * 1024) {
                                    // Using units MiB/s
                                    conversion = 2;
                                } else if (minCalculator > 1024) {
                                    // Using units KiB/s
                                    conversion = 1;
                                } else {
                                    // Using units Bytes/s
                                    conversion = 0;
                                }

                                NumberAxis xAxis = new NumberAxis();
                                NumberAxis yAxis = new NumberAxis();

                                yAxis.setLabel(new String[]{"Bytes/s", "KiB/s", "MiB/s"}[conversion]);
                                xAxis.setLabel("Playtime Downloaded");

                                LineChart<Number, Number> chart = new LineChart<>(xAxis,yAxis);
                                XYChart.Series<Number, Number> series = new XYChart.Series<>();

                                // Due to size constraints we ideally just want to map a few data points if there are too many
                                if (chartData.length() > 10) {

                                    // Group data into 10 clusters calculate average of each
                                    for (int i = 0; i < 9; i++) {

                                        OptionalDouble clusterAverageTimeOpt = IntStream.range(
                                                (int) Math.round((double) chartData.length() / 10) * i,
                                                (int) Math.round((double) chartData.length() / 10) * i+1
                                        ).mapToDouble(j -> {
                                            try {
                                                return chartData.getJSONObject(j).getInt("time");
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            return 0;
                                        }).average();

                                        OptionalDouble clusterAverageSpeedOpt = IntStream.range(
                                                (int) Math.round((double) chartData.length() / 10) * i,
                                                (int) Math.round((double) chartData.length() / 10) * i+1
                                        ).mapToDouble(j -> {
                                            try {
                                                return chartData.getJSONObject(j).getInt("speed");
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            return 0;
                                        }).average();

                                        if (clusterAverageTimeOpt.isPresent() && clusterAverageSpeedOpt.isPresent()) {

                                            series.getData().add(
                                                    new XYChart.Data<>(
                                                            clusterAverageTimeOpt.getAsDouble(),
                                                            clusterAverageSpeedOpt.getAsDouble()
                                                    )
                                            );

                                        } else {
                                            Debug.error(null, "Failed to calculate average of given cluster.", null);
                                        }

                                    }

                                } else {

                                    for (int i = 0; i < chartData.length(); i++) {
                                        series.getData().add(
                                                new XYChart.Data<>(
                                                        chartData.getJSONObject(i).getInt("time"),
                                                        chartData.getJSONObject(i).getInt("speed") / Math.pow(1024, conversion)
                                                )
                                        );

                                    }
                                }

                                chart.getData().add(series);
                                chart.prefWidthProperty().bind(((BorderPane) viewContainer.getChildren().get(0)).prefWidthProperty().subtract(textInfoContainer.widthProperty()));

                                Platform.runLater(() -> ((BorderPane) viewContainer.getChildren().get(0)).setRight(chart));
                            } catch (JSONException e) {
                                Debug.warn(null, "Unknown key");
                            }

                        }

                        // Downloads are completed, show now only histories
                        if (Model.getInstance().download.getDownloadObject().toString().equals(new JSONObject().toString())) {
                            Platform.runLater(() -> {
                                eventsViewTable.getItems().clear();
                                eventViewSelector.getItems().clear();
                                initialize();
                            });
                            this.cancel();
                        }

                    }
                }, 0, 20);

            } else {

                viewContainer.getChildren().remove(0);

            }

            /*
            BorderPane[] finalCurrentDownloadsView1 = currentDownloadsView[0];
            BorderPane[] finalPlannedDownloadsView1 = plannedDownloadsView;
            BorderPane[] finalDownloadHistoriesView1 = downloadHistoriesView;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run(){

                    try {
                        // Await model changes to redraw current downloads
                        JSONObject currentDownloads = Model.getInstance().download.getDownloadObject();
                        if (currentDownloads.toString().equals(new JSONObject().toString())) {

                            this.cancel();

                        } else {

                            BorderPane[] newCurrentDownloads = new BorderPane[currentDownloads.getJSONArray("songs").length()];
                            for (int i = 0; i < currentDownloads.getJSONArray("songs").length(); i++)
                                newCurrentDownloads[i] = eventsViewTable.getItems().get(eventsViewTable.getItems().size() - 1);

                            // Update if data has changed
                            if (!Arrays.equals(newCurrentDownloads, finalCurrentDownloadsView1)) {

                                Platform.runLater(() -> {
                                    currentDownloadsView[0] = newCurrentDownloads;

                                    eventsViewTable.getItems().clear();
                                    eventsViewTable.getItems().addAll(currentDownloadsView[0]);
                                    eventsViewTable.getItems().addAll(finalPlannedDownloadsView1);
                                    eventsViewTable.getItems().addAll(finalDownloadHistoriesView1);
                                });

                            }
                        }

                    } catch (JSONException e) {
                        Debug.error(Thread.currentThread(), "Failed to parse JSON to update current downloads view.", e.getCause());
                    }

                }
            }, 0, 50);
             */

        } else {
            Debug.warn(null, "Downloads was accessed without any downloads history, downloads in progress or any download queue items, this should not have happened.");
        }

        Debug.trace(null, "Initialized downloads view.");

    }

    @FXML
    public void searchView(Event event) {

        // Go to search page
        try {
            Parent searchView = FXMLLoader.load(getClass().getResource("app/fxml/search.fxml"));
            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(searchView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

        } catch(IOException e) {
            Debug.error(null, "FXML Error: search.fxml", e.getCause());
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

            Debug.warn(Thread.currentThread(), "Failed to use cached resource.");

            // Sending the request causes lag, hence use as thread, only needs to be called once, in future can add network error handling but that seems excessive as of the moment
            try {
                Thread loadAlbumArt = new Thread(() -> {

                    // Setting as default until it loads (or doesn't)
                    try {
                        albumArt.setImage(
                                new Image(
                                        getClass().getResource("app/img/song_default.png").toURI().toString(),
                                        85,
                                        85,
                                        true,
                                        true
                                )
                        );
                    } catch (URISyntaxException er) {
                        Debug.error(null, "Failed to set default album art.", er.getCause());
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
                        Debug.warn(null, "Failed to connect to allmusic to get album art, using default.");
                    } catch (JSONException e) {
                        Debug.error(null, "Failed to get art for loading resource.", e.getCause());
                    }
                }, "load-art");
                loadAlbumArt.setDaemon(true);
                loadAlbumArt.start();
            } catch (IndexOutOfBoundsException e) {
                Debug.warn(null, "Internal error loading album art.");
            }
        }

        // Greyscale if downloaded & files don't exist
        if (!Files.exists(Paths.get(viewData.getString("directory"))) && !viewData.has("completed")) {
            // Greyscale the album art
            ColorAdjust desaturate = new ColorAdjust();
            desaturate.setSaturation(-1);
            albumArt.setEffect(desaturate);

            // Use default cursor as directory can't be opened
            result.setCursor(Cursor.DEFAULT);
        }

        BorderPane resultInformationContainer = new BorderPane();

        Text title = new Text(viewData.getString("title"));
        title.setStyle("-fx-font-weight: bold; -fx-font-family: arial; -fx-font-size: 22px;");

        Text artist = new Text(viewData.getString("artist"));
        artist.setStyle("-fx-font-family: arial; -fx-font-size: 16px; -fx-font-style: italic;");

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
                                            getClass().getResource("app/img/icon.png").toURI().toString(),
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
                                                getClass().getResource("app/img/tick.png").toURI().toString(),
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
            Group crossBox = new Group(
                    new Line(20, 0, 0, 20),
                    new Line(20, 20, 0, 0)
            );
            IntStream.range(0, 2).forEach(i -> {((Line) crossBox.getChildren().get(i)).setStroke(Color.GRAY); ((Line) crossBox.getChildren().get(i)).setStrokeWidth(2);} );
            crossBox.setOnMouseEntered(e ->
                    IntStream
                            .range(0, 2)
                            .forEach(
                                    i -> ((Line) crossBox.getChildren().get(i)).setStroke(Color.BLACK)
                            )
            );
            crossBox.setOnMouseExited(e ->
                    IntStream
                            .range(0, 2)
                            .forEach(
                                    i -> ((Line) crossBox.getChildren().get(i)).setStroke(Color.GRAY)
                            )
            );
            crossBox.setOnMouseClicked(event -> {

                Model.getInstance().download.deleteHistory(viewData);

                try {
                    Parent settingsView = FXMLLoader.load(getClass().getResource("app/fxml/downloads.fxml"));
                    Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    mainWindow.setScene(new Scene(settingsView, mainWindow.getWidth() - 16, mainWindow.getHeight() - 39));
                } catch (IOException er) {
                    Debug.error(null, "FXML Error: Downloads.fxml", er.getCause());
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

        return result;

    }

}
