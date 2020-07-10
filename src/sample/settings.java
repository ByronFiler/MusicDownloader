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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

// TODO
// Information & Files cutoff when resizing

public class settings {
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
    @FXML ToggleSwitch advancedValidationToggle;

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

    // Confirm / Cancel
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
        advancedValidationToggle.setSelected(Model.getInstance().settings.getSettingBool("advanced_validation"));

        // Meta-Data
        albumArtToggle.setSelected(Model.getInstance().settings.getSettingBool("album_art"));
        albumTitleToggle.setSelected(Model.getInstance().settings.getSettingBool("album_title"));
        songTitleToggle.setSelected(Model.getInstance().settings.getSettingBool("song_title"));
        artistToggle.setSelected(Model.getInstance().settings.getSettingBool("artist"));
        yearToggle.setSelected(Model.getInstance().settings.getSettingBool("year"));
        trackNumberToggle.setSelected(Model.getInstance().settings.getSettingBool("track"));

        // Application
        darkThemeToggle.setSelected(Model.getInstance().settings.getSettingBool("dark_theme"));
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
            Debug.error(null, "Missing FXML File: Search.fxml", e.getCause());
        }

    }

    @FXML
    private void selectNewFolder() {

        try {

            JFileChooser newFolder = new JFileChooser();
            newFolder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            newFolder.showSaveDialog(null);

            outputDirectory.setText(newFolder.getSelectedFile().getPath());
            validateConfirm();

        } catch (NullPointerException ignored) {}

    }

    @FXML
    private void validateConfirm() {
        // Check if settings have been adjusted from default

        System.out.println(settings.toString());
        System.out.println(getNewSettings().toString());

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

    @FXML
    private void saveSettings() {
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
            // Files
            settings.put("output_directory", outputDirectory.getText());
            settings.put("music_format", musicFormat.getSelectionModel().getSelectedIndex());
            settings.put("save_album_art", saveAlbumArt.getSelectionModel().getSelectedIndex());
            settings.put("advanced_validation", advancedValidationToggle.isSelected());

            // Meta-Data Application
            settings.put("album_art", albumArtToggle.isSelected());
            settings.put("album_title", albumTitleToggle.isSelected());
            settings.put("song_title", songTitleToggle.isSelected());
            settings.put("artist", artistToggle.isSelected());
            settings.put("year", yearToggle.isSelected());
            settings.put("track", trackNumberToggle.isSelected());

            // Application Configuration
            settings.put("dark_theme", darkThemeToggle.isSelected());
            settings.put("data_saver", dataSaverToggle.isSelected());

        } catch (JSONException e) {
            Debug.error(null, "Failed to generate new settings.", e.getCause());
        }

        return settings;


    }

    // Sends a web-request to the github to check the latest version available
    class getLatestVersion implements Runnable {

        Thread t;

        getLatestVersion (){
            t = new Thread(this, "get-latest-version");
            t.setDaemon(true);
            t.start();
        }

        public void run() {

            try {
                Document githubRequestLatestVersion = Jsoup.connect("https://raw.githubusercontent.com/ByronFiler/MusicDownloader/master/src/sample/app/meta.json").get();
                JSONObject jsonData = new JSONObject(githubRequestLatestVersion.text());
                Platform.runLater(() -> {
                    try {
                        latestVersion.setText(jsonData.get("version").toString());
                    } catch (JSONException ignored) {
                        Debug.warn(t, "Found data syntactically incorrect.");
                    }
                });

            } catch (IOException | JSONException e) {
                Debug.warn(t, "Failed to get latest version, connection issue.");
                new awaitReconnection();
                Platform.runLater(() -> latestVersion.setText("Unknown"));
            }
        }

    }

    // Will attempt to check the version in the event of failure will update
    class awaitReconnection implements Runnable {

        public awaitReconnection() {
            Thread thread = new Thread(this, "reconnection-get-version");
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {

            // Sending repeated connection attempts is fine as we only need one to succeed, rest go nowhere
            Timer connectionAttempt = new Timer();
            connectionAttempt.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (InetAddress.getByName("github.com").isReachable(1000)) {
                            new getLatestVersion();
                            connectionAttempt.cancel();
                        }
                    } catch (IOException ignored) {}

                }
            }, 0, 1000);

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
            thread.setDaemon(true);
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



