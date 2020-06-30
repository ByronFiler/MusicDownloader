package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

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
                // FXML File Error
                System.exit(-1);
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
