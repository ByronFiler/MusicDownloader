package musicdownloader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import musicdownloader.utils.app.Debug;

import java.io.IOException;

/*
TODO
 Look at AsyncHttpClient to speed up high volume of web requests
 Add different language options
 Support different search databases: https://en.wikipedia.org/wiki/List_of_online_music_databases
 Support different video sources: Dailymotion, Vimeo, Flickr, Metacafe
 Normalise audio on songs as a post processing effect
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

        Debug.trace("Primary Stage Generated");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
