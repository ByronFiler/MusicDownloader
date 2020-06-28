package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

// TODO: Begin transitioning program into FXML files

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        boolean useBeta = true;

        if (useBeta) {

            try {
                FXMLLoader loader = new FXMLLoader(new File("resources/fxml/search.fxml").toURI().toURL());
                Parent root = loader.load();

                primaryStage.setScene(new Scene(root));
                primaryStage.show();

                Debug.trace(null, "Primary Stage Generated");

            } catch (IOException e) {
                // Missing FXML file
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
