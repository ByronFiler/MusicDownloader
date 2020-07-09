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

// Not working with FXML file
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
        JSONObject downloadObject = Model.getInstance().download.getDownloadObject();

        // Check what should be displayed
        if (downloadHistory.length() > 0 || Model.getInstance().download.getDownloadQueue().length() > 0 || downloadObject.length() > 0) {

            // Drawing current downloads if they exist
            if (downloadObject.has("metadata")) {

                try {
                    for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {

                        // Preparing relevant data to be used in view
                        JSONObject downloadingItemData = new JSONObject();
                        downloadingItemData.put("artId", downloadObject.getJSONObject("metadata").getString("artId"));
                        downloadingItemData.put("artist", downloadObject.getJSONObject("metadata").getString("artist"));
                        downloadingItemData.put("id", downloadObject.getJSONArray("songs").getJSONObject(i).getString("id"));
                        downloadingItemData.put("title", downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"));
                        downloadingItemData.put("directory", downloadObject.getJSONObject("metadata").getString("directory"));
                        downloadingItemData.put("artUrl", downloadObject.getJSONObject("metadata").getString("artUrl"));
                        downloadingItemData.put("completed", downloadObject.getJSONArray("songs").getJSONObject(i).getBoolean("completed"));

                        // Send data to the model
                        Model.getInstance().download.setDataItem(downloadingItemData);

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

            // Drawing planned downloads if they exist


            // Drawing download histories if they exist
            if (downloadHistory.length() > 0) {
                eventViewSelector.getItems().add("Downloads");
                
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

            // Evaluate whether to decide to hide the current download info-box and the combobox
            if (eventViewSelector.getItems().size() < 2) {

                // Only one thing to show
                eventViewSelector.setVisible(false);

                // Only has one relevant item, hence hide it's self and set the title as the only relevant item
                switch (eventViewSelector.getItems().get(0)) {

                    case "Downloads":
                        eventViewTitle.setText("Download History");
                        infoContainer.setVisible(false);
                        infoContainer.setManaged(false);
                        break;


                    case "Downloading...":
                        eventViewTitle.setText("Downloading...");
                        break;
                }

            } else {

                eventViewTitle.setText("All");
                eventViewSelector.getItems().add(0, "All");

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

}
