package sample;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.FileWriter;
import java.io.IOException;


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

            case F1:
                System.out.println(view.dumpThreadData());
                break;

            case F2:
                try {
                    FileWriter dumpFile = new FileWriter("dump.txt");
                    dumpFile.write(view.dumpThreadData());
                    dumpFile.close();
                } catch (IOException ignored) {}
                Debug.trace(null, "Dumped threads data to: dump.exe");



            case ESCAPE:
                view.searchRequest.setText("The Dark Side of the Moon");
                break;


        }

    }

}
