package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sample.utils.debug;

import java.io.IOException;

/*
TODO
 Add different language options
 Support different search clients, make allmusic work into a class
 Resources should contain a link to the application folder, save constant function calls and make os compatibility simpler
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
