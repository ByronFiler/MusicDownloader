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

        if (
                view.settingsTitle.isVisible() &&
                view.OutputDirectorySettingNew.equals(view.outputDirectorySetting) &&
                view.musicFormatSetting == view.songDownloadFormatResult
                        .getSelectionModel()
                        .selectedIndexProperty()
                        .getValue() &&
                view.saveAlbumArtSetting == view.saveAlbumArtResult
                        .getSelectionModel()
                        .selectedIndexProperty()
                        .getValue()
        ) {
            switch (event.getCode()) {

                case ESCAPE:
                    view.searchMode();
                    break;


            }
        }

        switch (event.getCode()) {

            case ENTER:
                view.searchResults = view.handleSearch();
                break;

            case X:
                view.searchRequest.setText("The Dark Side of the Moon");
                view.searchResults = view.handleSearch();


        }

    }

}
