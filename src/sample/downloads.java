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

import java.io.IOException;

// Not working with FXML file
public class downloads {

    @FXML VBox viewContainer;
    @FXML BorderPane infoContainer;
    @FXML VBox downloadInfo;

    @FXML Text eventViewTitle;
    @FXML ComboBox<String> eventViewSelector;

    @FXML ListView<BorderPane> eventsViewTable;

    @FXML
    private void initialize() {

        JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();

        // Check what should be displayed
        if (downloadHistory.length() > 0 || Model.getInstance().download.getDownloadQueue().length() > 0 || Model.getInstance().download.getDownloadObject().length() > 0) {

            if (downloadHistory.length() > 0 && !Model.getInstance().download.getDownloadObject().has("metadata")) {
                // Drawing purely download history
                eventViewTitle.setText("Download History");
                eventViewSelector.setVisible(false);
                infoContainer.setVisible(false);
                infoContainer.setManaged(false);

                // Drawing the download history
                for (int i = 0; i < downloadHistory.length(); i++) {

                    try {

                        // Pass the BorderPane element too

                        // Initialize a back and fourth between the model
                        Model.getInstance().download.setHistoryItem(downloadHistory.getJSONObject(i));
                        BorderPane resultLoader = new FXMLLoader(getClass().getResource("app/fxml/history.fxml")).load();
                        resultLoader.minWidthProperty().bind(eventsViewTable.widthProperty().subtract(30));

                        eventsViewTable.getItems().add(resultLoader);

                    } catch (JSONException e) {
                        // Error for now, later handle it and make it a warning
                        Debug.error(null, "Failed to parse JSON to draw downloads history.", e.getCause());
                    } catch (IOException e) {
                        Debug.error(null, "Failed to create FXML file for result.", e.getCause());
                    }

                }

            } else if (Model.getInstance().download.getDownloadHistory().length() == 0 && Model.getInstance().download.getDownloadObject().has("metadata")){
                // Drawing purely downloads in progress and download queue if necessary

            } else {
                // Has: history, downloads in progress and possibly more items in download queue
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
