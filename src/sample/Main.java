package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sample.utils.app.debug;

import java.io.IOException;

/*
TODO
 Add different language options
 Support different search databases: https://en.wikipedia.org/wiki/List_of_online_music_databases
 Support different video sources: Dailymotion, Vimeo, Flickr, Metacafe
 */

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("app/fxml/search.fxml"));
        Parent root = loader.load();

        primaryStage.setScene(new Scene(root));
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("app/img/icon.png")));
        primaryStage.setTitle("Music Downloader");
        primaryStage.show();

        debug.trace(null, "Primary Stage Generated");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
