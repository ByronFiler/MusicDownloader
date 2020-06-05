package sample;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

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
