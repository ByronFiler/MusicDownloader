package sample;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;

public class View implements EventHandler<KeyEvent>
{
    public Controller controller;
    public Utils utils;

    public int width;
    public int height;

    public Pane pane;
    public Canvas canvas;

    public TextField searchRequest;

    public View(int w, int h)
    {
        Debug.trace("View::<constructor>");
        width = w;
        height = h;
    }

    public void start(Stage window)
    {
        pane = new Pane();
        pane.setId("initial");

        canvas = new Canvas(width, height);
        pane.getChildren().add(canvas);

        Label title = new Label("Music Downloader");
        title.setTextFill(Color.BLACK);
        title.setStyle("-fx-font: 32 arial;");
        title.setTranslateY(300);
        title.setTranslateX(170);

        searchRequest = new TextField();
        searchRequest.setTranslateX(220);
        searchRequest.setTranslateY(340);

        pane.getChildren().add(title);
        pane.getChildren().add(searchRequest);

        Scene scene = new Scene(pane);
        scene.setOnKeyPressed(this);

        window.setScene(scene);
        window.setTitle("Music Downloader");

        window.show();

    }

    public void handle(KeyEvent event) {
        try {
            controller.userKeyInteraction(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void handleSearch() throws IOException {

        // Load the search data
        ArrayList<ArrayList<String>> searchResults =  utils.allmusicQuery(searchRequest.getText());

        if (searchResults.size() > 0) {
            // Transition to table
        } else {
            // Inform user invalid search
        }


        // Transition to table


    }
}
