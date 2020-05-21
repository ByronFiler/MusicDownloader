package sample;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import static javafx.scene.input.KeyCode.ENTER;


public class Controller {

    public View view;
    public Main main;

    public Controller()
    {
        Debug.trace("Controller::<constructor>");
    }

    public void userKeyInteraction (KeyEvent event) throws Exception {
        Debug.trace("User key interaction" + event.getCode());

        switch (event.getCode()) {

            case ENTER:
                view.setSearchData(view.handleSearch());
                break;

        }

    }

}
