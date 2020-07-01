package sample;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.io.IOException;

// TODO
// Information & Files cutoff when resizing

public class settings {

    @FXML AnchorPane root;
    @FXML StackPane innerRoot;

    // Information
    @FXML Text version;
    @FXML Text latestVersion;
    @FXML Text youtubeDl;
    @FXML Text ffmpeg;

    // Files
    @FXML Text outputDirectory;
    @FXML BorderPane saveMusicLine;
    @FXML Label outputDirectoryInfo;
    @FXML ComboBox<String> musicFormat;
    @FXML ComboBox<String> saveAlbumArt;

    // Meta-Data Application
    @FXML ToggleSwitch albumArtToggle;
    @FXML ToggleSwitch albumTitleToggle;
    @FXML ToggleSwitch songTitleToggle;
    @FXML ToggleSwitch artistToggle;
    @FXML ToggleSwitch yearToggle;
    @FXML ToggleSwitch trackNumberToggle;

    // Application Configuration
    @FXML ToggleSwitch darkThemeToggle;
    @FXML ToggleSwitch dataSaverToggle;

    @FXML Button saveSettings;
    @FXML Button cancel;

    private JSONObject settings;

    @FXML
    private void initialize() {

        // Prepare settings information from model data
        settings = Model.getInstance().settings.getSettings();

        // Information
        version.setText(Model.getInstance().settings.getVersion() == null ? "Unknown" : Model.getInstance().settings.getVersion());
        new getLatestVersion();
        new verifyExecutable("youtube-dl", youtubeDl);
        new verifyExecutable("ffmpeg", ffmpeg);

        // Files
        outputDirectory.setText(Model.getInstance().settings.getSetting("output_directory").equals("") ? System.getProperty("user.dir") : Model.getInstance().settings.getSetting("output_directory"));
        outputDirectory.wrappingWidthProperty().bind(saveMusicLine.widthProperty().subtract(outputDirectoryInfo.widthProperty()).subtract(30));
        musicFormat.getSelectionModel().select(Integer.parseInt(Model.getInstance().settings.getSetting("music_format")));
        saveAlbumArt.getSelectionModel().select(Integer.parseInt(Model.getInstance().settings.getSetting("save_album_art")));

        // Meta-Data
        albumArtToggle.setSelected(Model.getInstance().settings.getSettingBool("album_art"));
        albumTitleToggle.setSelected(Model.getInstance().settings.getSettingBool("album_title"));
        songTitleToggle.setSelected(Model.getInstance().settings.getSettingBool("song_title"));
        artistToggle.setSelected(Model.getInstance().settings.getSettingBool("artist"));
        yearToggle.setSelected(Model.getInstance().settings.getSettingBool("year"));
        trackNumberToggle.setSelected(Model.getInstance().settings.getSettingBool("track"));

        // Application
        darkThemeToggle.setSelected(Model.getInstance().settings.getSettingBool("theme"));
        dataSaverToggle.setSelected(Model.getInstance().settings.getSettingBool("data_saver"));


        Debug.trace(null, "Initialized settings view.");

    }

    @FXML
    private void searchView(Event event) {

        try {

            AnchorPane searchView = FXMLLoader.load(getClass().getResource("app/fxml/search.fxml"));
            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();

            mainWindow.setScene(new Scene(searchView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

        } catch (IOException e) {
            Debug.error(null, "Missing FXML File: Search.fxml", e.getStackTrace());
        }

    }

    @FXML private void selectNewFolder() {

        try {

            JFileChooser newFolder = new JFileChooser();
            newFolder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            newFolder.showSaveDialog(null);

            outputDirectory.setText(newFolder.getSelectedFile().getPath());
            validateConfirm();

        } catch (NullPointerException ignored) {}

    }

    @FXML private void validateConfirm() {
        // Check if settings have been adjusted from default

        if (settings.toString().equals(getNewSettings().toString())) {
            // Settings have not been modified, hence return to default
            Platform.runLater(() -> {
                saveSettings.setDisable(true);
                cancel.setText("Back");
            });

        } else {
            // Settings have been modified
            Platform.runLater(() -> {
                saveSettings.setDisable(false);
                cancel.setText("Cancel");
            });

        }

    }

    @FXML private void saveSettings() {
        Model.getInstance().settings.saveSettings(getNewSettings());
        settings = Model.getInstance().settings.getSettings();

        Platform.runLater(() -> {
            saveSettings.setDisable(true);
            cancel.setText("Back");
        });

        Debug.trace(null, "Updated settings file");
    }

    private JSONObject getNewSettings() {

        JSONObject settings = new JSONObject();

        try {
            //
            settings.put("output_directory", outputDirectory.getText());
            settings.put("music_format", musicFormat.getSelectionModel().getSelectedIndex());
            settings.put("save_album_art", saveAlbumArt.getSelectionModel().getSelectedIndex());

            // Meta-Data Application
            settings.put("album_art", albumArtToggle.isSelected() ? 1 : 0);
            settings.put("album_title", albumTitleToggle.isSelected() ? 1 : 0);
            settings.put("song_title", songTitleToggle.isSelected() ? 1 : 0);
            settings.put("artist", artistToggle.isSelected() ? 1 : 0);
            settings.put("year", yearToggle.isSelected() ? 1 : 0);
            settings.put("track", trackNumberToggle.isSelected() ? 1 : 0);

            // Application Configuration
            settings.put("theme", darkThemeToggle.isSelected() ? 1 : 0);
            settings.put("data_saver", dataSaverToggle.isSelected() ? 1 : 0);

        } catch (JSONException e) {
            Debug.error(null, "Failed to generate new settings.", e.getStackTrace());
        }

        return settings;


    }

    // Sends a web-request to the github to check the latest version available
    class getLatestVersion implements Runnable {

        Thread t;

        getLatestVersion (){
            t = new Thread(this, "get-latest-version");
            t.start();
        }

        public void run() {

            try {

                Document githubRequestLatestVersion = Jsoup.connect("https://raw.githubusercontent.com/ByronFiler/MusicDownloader/master/resources/json/meta.json").get();
                JSONObject jsonData = new JSONObject(githubRequestLatestVersion.text());
                Platform.runLater(() -> {
                    try {
                        latestVersion.setText(jsonData.get("version").toString());
                    } catch (JSONException e) {
                        Debug.warn(t, "Failed to get latest version.");
                    }
                });

            } catch (IOException | JSONException e) {
                Debug.warn(t, "Failed to get latest version.");
                Platform.runLater(() -> latestVersion.setText("Unknown"));
            }

            /*
            double originalWidth = latestVersionResult.getWidth();
            String latestVersion = SettingsFunc.getLatestVersion();

            Platform.runLater(() -> latestVersionResult.setText(latestVersion == null ? "Unknown" : latestVersion));
            while (latestVersionResult.getWidth() == originalWidth) { try {Thread.sleep(10);} catch (InterruptedException ignored) {} }
            Platform.runLater(() -> latestVersionResultContainer.setPadding(new Insets(70, 0, 0, -latestVersionResult.getWidth())));

             */
        }

    }

    // Attempts to test an execution
    static class verifyExecutable implements Runnable {

        private final Thread thread;
        private final String executable;
        private final Text element;

        verifyExecutable(String executable, Text element) {
            this.executable = executable;
            this.element = element;

            thread = new Thread(this, "executable-execution");
            thread.start();
        }

        public void run() {

            try {

                // Will throw an error if not setup
                Runtime.getRuntime().exec(new String[]{executable});
                Platform.runLater(() -> element.setText("Configured"));

            } catch (IOException ignored) {

                Debug.warn(thread, "Failed to verify executable: " + executable);
                Platform.runLater(() -> element.setText("Not Configured"));

            }

        }

    }
}



