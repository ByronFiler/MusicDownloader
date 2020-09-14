package musicdownloader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;

import java.io.IOException;

/*
TODO
 DOWNLOAD SONG INDIVIDUAL METADATA IS INCORRECT POSITION CORRECT IMMEDIATELY
 Add different language options: https://stackoverflow.com/questions/26325403/how-to-implement-language-support-for-javafx-in-fxml-documents
 Support different search databases: https://en.wikipedia.org/wiki/List_of_online_music_databases
 When the window is closed mid download save the object and continue the download?
 If a song is mono, set the mp3 to play as mono, if it isn't already?
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

        Model.getInstance().setPrimaryStage(primaryStage);

        Debug.trace("Primary Stage Generated");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
