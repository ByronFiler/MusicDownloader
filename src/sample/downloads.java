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

// TODO
// Theoretically can assign elements ID and perhaps just hide and unmanage IDs instead of reloading assets for efficency
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

        // Check what should be displayed
        if (downloadHistory.length() > 0 || downloadQueue.length() > 0 || downloadObject.length() > 0) {

            // Drawing current downloads if they exist
            if (downloadObject.has("metadata")) {
                eventViewSelector.getItems().add("Currently Downloading");
                drawCurrentDownloads(downloadObject);
            }

            // Drawing planned downloads if they exist
            if (downloadQueue.length() > 0) {
                eventViewSelector.getItems().add("Download Queue");
                drawPlannedQueue(downloadQueue);
            }

            // Drawing download histories if they exist
            if (downloadHistory.length() > 0) {
                eventViewSelector.getItems().add("Download History");
                drawHistory(downloadHistory);
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
                eventViewSelector.setOnAction(e -> {

                    eventsViewTable.getItems().clear();
                    switch (eventViewSelector.getSelectionModel().getSelectedItem()) {

                        case "All":
                            // Redraw self
                            try {
                                Parent searchView = FXMLLoader.load(getClass().getResource("app/fxml/downloads.fxml"));

                                Stage mainWindow = (Stage) ((Node) e.getSource()).getScene().getWindow();
                                mainWindow.setScene(new Scene(searchView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

                            } catch(IOException er) {
                                Debug.error(null, "FXML Error: search.fxml", er.getCause());
                            }

                        case "Currently Downloading":
                            drawCurrentDownloads(downloadObject);
                            break;

                        case "Downloads Queue":
                            drawPlannedQueue(downloadQueue);
                            break;

                        case "Download History":
                            drawHistory(downloadHistory);
                            break;

                        default:
                            Debug.error(null, "Unknown combobox option selected: " + eventViewSelector.getSelectionModel().getSelectedItem(), null);
                    }
                });

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

    private void drawPlannedQueue(JSONArray downloadQueue) {

        try {
            for (int i = 0; i < downloadQueue.length(); i++) {
                for (int j = 0; j < downloadQueue.getJSONObject(i).getJSONArray("songs").length(); j++) {

                    // Sending (converted) data to model
                    Model.getInstance().download.setDataItem(generateViewData(downloadQueue.getJSONObject(i), j));

                    // Creating the result view
                    BorderPane downloadItemLoader = new FXMLLoader(getClass().getResource("app/fxml/history.fxml")).load();
                    downloadItemLoader.minWidthProperty().bind(eventsViewTable.widthProperty().subtract(30));

                    // Update the table
                    eventsViewTable.getItems().add(downloadItemLoader);
                }

            }
        } catch (JSONException e) {
            Debug.error(null, "Failed to parse data to draw planned queue items.", e.getCause());
        } catch (IOException e) {
            Debug.error(null, "FXML Error: history.fxml [planned queue]", e.getCause());
        }

    }

    private void drawCurrentDownloads(JSONObject downloadObject) {
        try {
            for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {

                // Send (converted) data to the model
                Model.getInstance().download.setDataItem(generateViewData(downloadObject, i));

                // Create the result view
                BorderPane downloadItemLoader = new FXMLLoader(getClass().getResource("app/fxml/history.fxml")).load();
                downloadItemLoader.minWidthProperty().bind(eventsViewTable.widthProperty().subtract(30));

                // Update the list view
                eventsViewTable.getItems().add(downloadItemLoader);

            }
        } catch (JSONException e) {
            Debug.error(null, "Error parsing JSON for download object.", e.getCause());
        } catch (IOException e) {
            Debug.error(null, "FXML Error: history.fxml", e.getCause());
        }
    }

    private void drawHistory(JSONArray downloadHistory) {

        for (int i = 0; i < downloadHistory.length(); i++) {

            try {
                // Update the model
                Model.getInstance().download.setDataItem(downloadHistory.getJSONObject(i));

                // Loading the FXML
                BorderPane resultLoader = new FXMLLoader(getClass().getResource("app/fxml/history.fxml")).load(); // Something in here is taking too much time
                resultLoader.minWidthProperty().bind(eventsViewTable.widthProperty().subtract(30));

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

}
