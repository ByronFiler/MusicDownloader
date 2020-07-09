package sample;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.IntStream;

public class history {

    @FXML BorderPane root;
    @FXML ImageView albumArt;
    @FXML Text title;
    @FXML Text artist;
    @FXML HBox iconContainer;

    private JSONObject data;


    // 2 ~ 4ms to execute
    @FXML
    void initialize() throws JSONException {
        // Load in relevant data from the Model
        data = Model.getInstance().download.getDataItem();
        title.setText(data.getString("title"));
        artist.setText(data.getString("artist"));

        if (Files.exists(Paths.get(String.format("resources\\cached\\%s.jpg", data.getString("artId"))))) {

            // Cached album art exists, use that
            albumArt.setImage(
                    new Image(
                            new File(String.format("resources\\cached\\%s.jpg", data.getString("artId"))).toURI().toString(),
                            85,
                            85,
                            true,
                            true
                    )
            );

        } else {

            // Sending the request causes lag, hence use as thread
            Thread loadAlbumArt = new Thread(() -> {
                try {
                    if (InetAddress.getByName("allmusic.com").isReachable(1000)) {
                        albumArt.setImage(
                                new Image(
                                        data.getString("artUrl"),
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
        }

        if (!Files.exists(Paths.get(data.getString("directory")))) {
            // Greyscale the album art
            ColorAdjust desaturate = new ColorAdjust();
            desaturate.setSaturation(-1);
            albumArt.setEffect(desaturate);

            // Use default cursor as directory can't be opened
            root.setCursor(Cursor.DEFAULT);
        }

        // Determine if this is a completed download
        if (data.has("completed")) {

            // This is a scheduled or completed download
            if (data.get("completed") == JSONObject.NULL) {

                // Queued in the future, not current in progress for a download
                // Download Icon




            } else {
                if (data.getBoolean("completed")) {

                    // In queue and downloaded
                    // Green Tick

                } else {

                    // In queue, not downloaded
                    // ProgressIndicator (Indeterminate)

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

                Model.getInstance().download.deleteHistory(data);

                try {
                    Parent settingsView = FXMLLoader.load(getClass().getResource("app/fxml/downloads.fxml"));
                    Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    mainWindow.setScene(new Scene(settingsView, mainWindow.getWidth() - 16, mainWindow.getHeight() - 39));
                } catch (IOException er) {
                    Debug.error(null, "FXML Error: Downloads.fxml", er.getCause());
                }

            });
            root.setOnMouseClicked(event -> {
                try {
                    Desktop.getDesktop().open(new File(data.getString("directory")));
                } catch (IOException | JSONException ignored) {
                    root.setCursor(Cursor.DEFAULT);
                    root.setOnMouseClicked(null);
                }
            });
            iconContainer.getChildren().add(crossBox);

        }
    }

    @FXML
    private void hoverSelect() {
        root.setStyle("-fx-background-color: WHITE");
    }

    @FXML
    private void hoverUnselect() {
        root.setStyle("-fx-background-color: transparent;");
    }

}
