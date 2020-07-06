package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

// TODO

// Auto-zip setting
// Handle the no youtube results strange error warning
// Create temporary directory

// Structure files & check about running
// data: All user created application data: config, download history, cache
// tmp: Temporary files: youtube-dl raw downloads, temporary wav files, bat files
// app: Core app files: css, fxml, images

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {

        boolean useBeta = true;

        if (useBeta) {

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("app/fxml/search.fxml"));
                Parent root = loader.load();

                primaryStage.setScene(new Scene(root));
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("app/img/icon.png")));
                primaryStage.setTitle("Music Downloader");
                primaryStage.show();

                Debug.trace(null, "Primary Stage Generated");

            } catch (IOException e) {

                throw e;

                // FXML File Error
                //Debug.error(null, "FXML Error: search.fxml", e.getCause());
                //System.exit(-1);
            }

        } else {

            View view = new View(600, 800);
            Controller controller = new Controller();
            Utils utils = new Utils();

            view.controller = controller;
            controller.view = view;

            utils.view = view;

            view.start(primaryStage);
            Debug.trace(null, "Primary Stage Generated");
        }

    }


    public static void main(String[] args) {
        launch(args);
    }
}
