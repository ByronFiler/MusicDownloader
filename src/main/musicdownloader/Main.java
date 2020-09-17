package musicdownloader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.ui.Notification;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/*
TODO
 Support different search databases: https://en.wikipedia.org/wiki/List_of_online_music_databases
 Allow users to select their own language overwriting local locale *heh*
 */

public class Main extends Application {

    static {
        Locale.setDefault(new Locale("cn"));
        try {
            ResourceBundle.getBundle("resources.locale.search");
        } catch (MissingResourceException ignored) {
            Debug.warn("Users current locale " + Locale.getDefault() + " is not supported, defaulting to English.");
            Locale.setDefault(new Locale("en", "GB"));
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                getClass().getClassLoader().getResource("resources/fxml/search.fxml"),
                ResourceBundle.getBundle("resources.locale.search")
        );
        Parent root = loader.load();

        primaryStage.setScene(new Scene(root));
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("resources/img/icon.png"))));
        primaryStage.setTitle(ResourceBundle.getBundle("resources.locale.search").getString("hero"));
        primaryStage.show();
        primaryStage.setOnCloseRequest(this::handleClose);

        Model.getInstance().setPrimaryStage(primaryStage);

        Debug.trace("Primary Stage Generated");

    }

    public static void main(String[] args) {
        launch(args);
    }

    private void handleClose(WindowEvent event) {
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
    }
}
