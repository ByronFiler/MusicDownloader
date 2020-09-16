package musicdownloader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.ui.Notification;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;

/*
TODO
 Add different language options: https://stackoverflow.com/questions/26325403/how-to-implement-language-support-for-javafx-in-fxml-documents
 Support different search databases: https://en.wikipedia.org/wiki/List_of_online_music_databases
 When the window is closed mid download save the object and continue the download?
 */

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("resources/fxml/search.fxml"));
        Parent root = loader.load();

        primaryStage.setScene(new Scene(root));
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("resources/img/icon.png")));
        primaryStage.setTitle("Music Downloader");
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            Model.getInstance().setStageClosed(true);
            Debug.trace("Primary stage closed.");
            if (!Model.getInstance().download.getDownloadObject().toString().equals(new JSONObject().toString())) {

                try {

                    int incompleteSongs = 0;
                    for (int i = 0; i < Model.getInstance().download.getDownloadObject().getJSONArray("songs").length(); i++) {
                        if (!Model.getInstance().download.getDownloadObject().getJSONArray("songs").getJSONObject(i).getBoolean("completed")) incompleteSongs++;
                    }

                    new Notification(
                            String.format("Downloading \"%s\" in Background...", Model.getInstance().download.getDownloadObject().getJSONObject("metadata").getString("album")),
                            String.format("%s song%s remaining.", incompleteSongs, incompleteSongs == 1 ? "" : "s"),
                            null,
                            TrayIcon.MessageType.INFO
                    );
                } catch (JSONException er) {
                    Debug.warn("Failed to send downloads notification on window close, review download object.");
                }

            }
        });

        Model.getInstance().setPrimaryStage(primaryStage);

        Debug.trace("Primary Stage Generated");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
