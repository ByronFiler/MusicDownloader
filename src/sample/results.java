package sample;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.io.IOException;

public class results {

    @FXML public AnchorPane root;

    @FXML public TableView results;
    @FXML public TableColumn artColumn;
    @FXML public TableColumn titleColumn;
    @FXML public TableColumn artistColumn;
    @FXML public TableColumn genreColumn;
    @FXML public TableColumn yearColumn;
    @FXML public TableColumn typeColumn;

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

        System.out.println("Initialized search view");

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
    public void searchView() {

        try {

            AnchorPane searchView = FXMLLoader.load(new File("resources\\fxml\\search.fxml").toURI().toURL());
            root.getChildren().setAll(searchView);

        } catch (IOException e) {
            Debug.error(null, "FXML Error with search.fxml", e.getStackTrace());
        }

    }

}
