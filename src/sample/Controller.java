package sample;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;


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
                    view.handleSearch();
                    break;
                }

            case ESCAPE:
                view.searchRequest.setText("The Dark Side of the Moon");
                break;

            case F1:
                Debug.trace(null, ((BorderPane) view.downloadEventsView.getItems().get(0)).getId() );
                break;

        }

    }

}
