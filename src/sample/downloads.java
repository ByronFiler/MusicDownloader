package sample;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.stream.IntStream;

// TODO: Make graphs and that a separate FXML page for better queries
public class downloads {

    @FXML VBox viewContainer;
    @FXML BorderPane infoContainer;
    @FXML VBox downloadInfo;

    @FXML Text downloadSpeed;
    @FXML Text eta;
    @FXML Text processing;

    @FXML Text eventViewTitle;
    @FXML ComboBox<String> eventViewSelector;
    @FXML ListView<BorderPane> eventsViewTable;

    @FXML
    private void initialize() {

        JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
        JSONArray downloadQueue = Model.getInstance().download.getDownloadQueue();
        JSONObject downloadObject = Model.getInstance().download.getDownloadObject();
        BorderPane[] currentDownloadsView = new BorderPane[0];
        BorderPane[] plannedDownloadsView = new BorderPane[0];
        BorderPane[] downloadHistoriesView = new BorderPane[0];

        // Check what should be displayed
        if (downloadHistory.length() > 0 || downloadQueue.length() > 0 || downloadObject.length() > 0) {

            // Drawing current downloads if they exist
            if (downloadObject.has("metadata")) {

                eventViewSelector.getItems().add("Currently Downloading");
                try {
                    currentDownloadsView = new BorderPane[downloadObject.getJSONArray("songs").length()];
                    for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {

                        // Send (converted) data to the model
                        Model.getInstance().download.setDataItem(generateViewData(downloadObject, i));

                        // Create the result view
                        BorderPane downloadItemLoader = new FXMLLoader(getClass().getResource("app/fxml/download.fxml")).load();
                        downloadItemLoader.minWidthProperty().bind(eventsViewTable.widthProperty().subtract(30));

                        // downloadItemLoader.setId(generateNewId() + "#1");

                        currentDownloadsView[i] = downloadItemLoader;

                        // Update the list view
                        eventsViewTable.getItems().add(downloadItemLoader);

                    }
                } catch (JSONException e) {
                    Debug.error(null, "Error parsing JSON for download object.", e.getCause());
                } catch (IOException e) {
                    Debug.error(null, "FXML Error: download.fxml", e.getCause());
                }
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

                            // Sending (converted) data to model
                            Model.getInstance().download.setDataItem(generateViewData(downloadQueue.getJSONObject(i), j));

                            // Creating the result view
                            BorderPane downloadItemLoader = new FXMLLoader(getClass().getResource("app/fxml/download.fxml")).load();
                            downloadItemLoader.minWidthProperty().bind(eventsViewTable.widthProperty().subtract(30));

                            plannedDownloadsView[k] = downloadItemLoader;

                            // Update the table
                            eventsViewTable.getItems().add(downloadItemLoader);
                            k++;
                        }

                    }
                } catch (JSONException e) {
                    Debug.error(null, "Failed to parse data to draw planned queue items.", e.getCause());
                } catch (IOException e) {
                    Debug.error(null, "FXML Error: download.fxml [planned queue]", e.getCause());
                }
            }

            // Drawing download histories if they exist
            if (downloadHistory.length() > 0) {
                eventViewSelector.getItems().add("Download History");
                downloadHistoriesView = new BorderPane[downloadHistory.length()];
                for (int i = 0; i < downloadHistory.length(); i++) {

                    try {
                        // Update the model
                        Model.getInstance().download.setDataItem(downloadHistory.getJSONObject(i));

                        // Loading the FXML
                        BorderPane resultLoader = new FXMLLoader(getClass().getResource("app/fxml/download.fxml")).load(); // Something in here is taking too much time
                        resultLoader.minWidthProperty().bind(eventsViewTable.widthProperty().subtract(30));

                        downloadHistoriesView[i] = resultLoader;

                        //resultLoader.setId(generateNewId() + "#2");

                        // Update table
                        eventsViewTable.getItems().add(resultLoader);

                    } catch (JSONException e) {
                        // Error for now, later handle it and make it a warning
                        Debug.error(null, "Failed to parse JSON to draw downloads history.", e.getCause());
                    } catch (IOException e) {
                        Debug.error(null, "Failed to create FXML file for result.", e.getCause());
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
                        infoContainer.setVisible(false);
                        infoContainer.setManaged(false);
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
                BorderPane[] finalCurrentDownloadsView = currentDownloadsView;
                BorderPane[] finalPlannedDownloadsView = plannedDownloadsView;
                BorderPane[] finalDownloadHistoriesView = downloadHistoriesView;
                eventViewSelector.setOnAction(e -> {
                    switch (eventViewSelector.getSelectionModel().getSelectedItem()) {

                        case "All":
                            eventsViewTable.getItems().clear();
                            eventsViewTable.getItems().addAll(finalCurrentDownloadsView);
                            eventsViewTable.getItems().addAll(finalPlannedDownloadsView);
                            eventsViewTable.getItems().addAll(finalDownloadHistoriesView);

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
                });

            }

            if (infoContainer.isManaged()) {

                // Load in download data from Model
                try {

                    JSONObject downloadInfo = Model.getInstance().download.getDownloadInfo();

                    downloadSpeed.setText(downloadInfo.getString("downloadSpeed"));
                    eta.setText(downloadInfo.getString("eta"));
                    processing.setText(
                            String.format(
                                    "%s (%s/%s)",
                                    downloadInfo.getString("song"),
                                    downloadInfo.getString("index"),
                                    downloadInfo.getString("songCount")
                            )
                    );


                } catch (JSONException e) {
                    //Debug.error(null, "Error parsing download info.", e.getCause());
                }

            }

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

}
