package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        View view = new View(600, 800);
        Controller controller = new Controller();
        Utils utils = new Utils();

        view.controller = controller;
        controller.view = view;

        utils.view = view;


        view.start(primaryStage);

    }


    public static void main(String[] args) {
        launch(args);
    }
}
