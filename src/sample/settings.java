package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import org.controlsfx.control.ToggleSwitch;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;

public class settings {

    @FXML AnchorPane root;

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

    @FXML
    private void initialize() {
        JSONObject savedSettings = SettingsFunc.getSettings();
        String versionData = SettingsFunc.getVersion();

        // Update toggle switches based on settings
        try {

            // Information
            if (versionData == null) {
                version.setText("Unknown");
                Debug.warn(null, "Failed to find the user version.");
            } else {
                version.setText(versionData);
            }
            new getLatestVersion();
            new verifyExecutable("youtube-dl", youtubeDl);
            new verifyExecutable("ffmpeg", ffmpeg);

            // Files
            outputDirectory.setText(savedSettings.get("output_directory").equals("") ? System.getProperty("user.dir") : savedSettings.get("output_directory").toString());
            outputDirectory.wrappingWidthProperty().bind(saveMusicLine.widthProperty().subtract(outputDirectoryInfo.widthProperty()).subtract(30));
            musicFormat.getSelectionModel().select(savedSettings.getInt("music_format"));
            saveAlbumArt.getSelectionModel().select(savedSettings.getInt("save_album_art"));

            // Meta-Data
            albumArtToggle.setSelected(savedSettings.getInt("album_art") != 0);
            albumTitleToggle.setSelected(savedSettings.getInt("album_title") != 0);
            songTitleToggle.setSelected(savedSettings.getInt("song_title") != 0);
            artistToggle.setSelected(savedSettings.getInt("artist") != 0);
            yearToggle.setSelected(savedSettings.getInt("year") != 0);
            trackNumberToggle.setSelected(savedSettings.getInt("track") != 0);

            // Application
            darkThemeToggle.setSelected(savedSettings.getInt("theme") != 0);
            dataSaverToggle.setSelected(savedSettings.getInt("data_saver") != 0);


        } catch (JSONException e) {
            Debug.warn(null, "Failed to load settings, generating new settings and retrying.");
            SettingsFunc.resetSettings();
            initialize();

        }
        // Give content to the context boxes

        Debug.trace(null, "Initialized settings view.");

    }

    @FXML
    private void searchView() {

        try {

            AnchorPane searchView = FXMLLoader.load(new File("resources\\fxml\\search.fxml").toURI().toURL());
            root.getChildren().setAll(searchView.getChildren().get(0));

        } catch (IOException e) {
            Debug.error(null, "Missing FXML File: Search.fxml", e.getStackTrace());
        }

    }

    @FXML private void selectNewFolder() {
        Debug.trace(null, "Should select new folder to save");
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

        private Thread thread;
        private String executable;
        private Text element;

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
                Platform.runLater(() -> element.setText("Working"));

            } catch (IOException ignored) {

                Debug.warn(thread, "Failed to verify executable: " + executable);
                Platform.runLater(() -> element.setText("Not Setup"));

            }

        }

    }
}



