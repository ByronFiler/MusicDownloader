package sample;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

public class history {

    @FXML BorderPane root;
    @FXML ImageView albumArt;
    @FXML Text title;
    @FXML Text artist;

    @FXML Line crossLine0;
    @FXML Line crossLine1;

    JSONObject historyData;

    @FXML
    void initialize() throws JSONException {

        // Load in relevant data from the Model
        historyData = Model.getInstance().download.getHistoryItem();
        title.setText(historyData.getString("title"));
        artist.setText(historyData.getString("artist"));

        if (Files.exists(Paths.get(String.format("resources\\cached\\%s.jpg", historyData.getString("artId"))))) {
            // Cached album art exists, use that
            albumArt.setImage(
                    new Image(
                            new File(String.format("resources\\cached\\%s.jpg", historyData.getString("artId"))).toURI().toString(),
                            85,
                            85,
                            true,
                            true
                    )
            );

        } else {

            // Attempt to use online album art
            try {
                if (InetAddress.getByName("allmusic.com").isReachable(1000)) {
                    albumArt.setImage(
                            new Image(
                                    historyData.getString("artUrl"),
                                    85,
                                    85,
                                    true,
                                    true
                            )
                    );
                }
            } catch (IOException e) {
                Debug.warn(null, "Failed to connect to allmusic to get album art, using default.");
            }

        }

        if (!Files.exists(Paths.get(historyData.getString("directory")))) {
            // Greyscale the album art
            ColorAdjust desaturate = new ColorAdjust();
            desaturate.setSaturation(-1);
            albumArt.setEffect(desaturate);
        }


    }

    @FXML
    private void blackenCross() {
        crossLine0.setStroke(Color.BLACK);
        crossLine1.setStroke(Color.BLACK);
    }

    @FXML
    private void lightenCross() {
        crossLine0.setStroke(Color.GRAY);
        crossLine1.setStroke(Color.GRAY);
    }

    @FXML
    private void hoverSelect() {
        root.setStyle("-fx-background-color: WHITE");
    }

    @FXML
    private void hoverUnselect() {
        root.setStyle("-fx-background-color: transparent;");
    }

    @FXML
    private void deleteHistory(Event event) throws IOException {

        // Delete this element from it's parent, then redraw downloads view
        Model.getInstance().download.deleteHistory(historyData);

        Parent settingsView = FXMLLoader.load(getClass().getResource("app/fxml/downloads.fxml"));
        Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
        mainWindow.setScene(new Scene(settingsView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));
    }

}
