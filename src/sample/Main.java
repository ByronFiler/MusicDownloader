package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sample.utils.debug;

import java.io.IOException;

// TODO
// Auto-zip setting
// Create temporary directory

// Structure files & check about running
// tmp: Temporary files: youtube-dl raw downloads, temporary wav files, bat files

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {

        //sample.utils.install.getYoutubeDl();
        sample.utils.install.getFFMPEG();

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
