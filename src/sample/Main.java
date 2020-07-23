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

// Once CSS files are finished merge into a single style sheet

// Auto-zip setting
// Handle the no youtube results strange error warning
// Create temporary directory

// Structure files & check about running
// usr: All user created application data: config, download history, cache
// tmp: Temporary files: youtube-dl raw downloads, temporary wav files, bat files
// app: Core app files: css, fxml, images

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("app/fxml/search.fxml"));
        Parent root = loader.load();

        primaryStage.setScene(new Scene(root));
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("app/img/icon.png")));
        primaryStage.setTitle("Music Downloader");
        primaryStage.show();

        debug.trace(null, "Primary Stage Generated");
    }


    public static void main(String[] args) {
        launch(args);
    }
}
