package sample;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;


public class Controller {

    public View view;

    public Controller() {
        Debug.trace(null, "<constructor>");
    }

    public void userKeyInteraction (KeyEvent event) {
        //Debug.trace(null, "User key interaction" + event.getCode());

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
            if (event.getCode() == KeyCode.ESCAPE) {
                view.searchMode();
            }
        }

        switch (event.getCode()) {

            case ENTER:

                if (view.searchRequest.isVisible()) {
                    view.searchResults = view.handleSearch();
                    break;
                }

            case ESCAPE:
                view.searchRequest.setText("The Dark Side of the Moon");
                break;


        }

    }

}
