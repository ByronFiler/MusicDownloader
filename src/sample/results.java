package sample;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class results {

    @FXML public AnchorPane root;

    @FXML public TableView<Model.resultsSet> results;
    @FXML public TableColumn<String, Model.resultsSet> artColumn;
    @FXML public TableColumn<String, Model.resultsSet> titleColumn;
    @FXML public TableColumn<String, Model.resultsSet> artistColumn;
    @FXML public TableColumn<String, Model.resultsSet> genreColumn;
    @FXML public TableColumn<String, Model.resultsSet> yearColumn;
    @FXML public TableColumn<String, Model.resultsSet> typeColumn;

    @FXML public ProgressIndicator queueAdditionProgress;
    @FXML public Button download;
    @FXML public Button cancel;

    @FXML
    private void initialize() {

        // Declare table properties
        titleColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.2).add(-15));
        artistColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.2).add(-15));
        yearColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.2).add(-15));
        genreColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.2).add(-15));
        typeColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.2).add(-15));

        // Set the table data
        results.getItems().setAll(Model.getInstance().getSearchResults());

        Debug.trace(null, "Initialized results view");

    }

    /*

    @FXML
    public void makeQueueItem() {



    }

    @FXML
    public void processValue() {

    }

     */
    @FXML
    public void searchView(Event event) {

        try {

            Parent searchView = FXMLLoader.load(getClass().getResource("app/fxml/search.fxml"));
            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();

            mainWindow.setScene(new Scene(searchView));

        } catch (IOException e) {
            Debug.error(null, "FXML Error with search.fxml", e.getStackTrace());
        }

    }

}
