package musicdownloader.controllers;

import com.google.common.base.CaseFormat;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import musicdownloader.Main;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.io.Install;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.ToggleSwitch;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;

import static musicdownloader.utils.app.Resources.albumArtOptions;
import static musicdownloader.utils.app.Resources.songReferences;

// TODO
// Information & Files cutoff when resizing

public class Settings {

    @FXML
    BorderPane root;

    // Information
    @FXML Label version;
    @FXML Label latestVersion;
    @FXML Label youtubeDl;
    @FXML Label ffmpeg;

    @FXML HBox versionContainer;
    @FXML HBox latestVersionContainer;
    @FXML HBox youtubeDlContainer;
    @FXML HBox ffmpegContainer;

    // Files
    @FXML Label outputDirectory;
    @FXML BorderPane saveMusicLine;
    @FXML Label outputDirectoryInfo;
    @FXML HBox outputDirectoryContainer;
    @FXML ComboBox<String> musicFormat;
    @FXML ComboBox<String> saveAlbumArt;

    // Audio
    @FXML ToggleSwitch advancedValidationToggle;
    @FXML ToggleSwitch volumeCorrectionToggle;

    // Meta-Data application
    @FXML ToggleSwitch albumArtToggle;
    @FXML ToggleSwitch albumTitleToggle;
    @FXML ToggleSwitch songTitleToggle;
    @FXML ToggleSwitch artistToggle;
    @FXML ToggleSwitch yearToggle;
    @FXML ToggleSwitch trackNumberToggle;

    // application Configuration
    @FXML ToggleSwitch darkThemeToggle;
    @FXML ToggleSwitch dataSaverToggle;

    // Confirm / Cancel
    @FXML Button cancel;

    @FXML
    protected void initialize() {

        // Prepare settings information from model data

        // Information
        if (Model.getInstance().settings.getVersion() == null) {
            version.setText("Unknown");
            versionContainer.getChildren().add(
                    new ImageView(
                            new Image(
                                    Main.class.getResourceAsStream("resources/img/warning.png"),
                                    20,
                                    20,
                                    true,
                                    true
                            )
                    )
            );
        } else {
            version.setText(Model.getInstance().settings.getVersion());
            versionContainer.getChildren().add(
                    new ImageView(
                            new Image(
                                    Main.class.getResourceAsStream("resources/img/tick.png"),
                                    20,
                                    20,
                                    true,
                                    true
                            )
                    )
            );
        }

        version.setText(Model.getInstance().settings.getVersion() == null ? "Unknown" : Model.getInstance().settings.getVersion());
        new getLatestVersion(false);
        new verifyExecutable(Resources.getInstance().getYoutubeDlExecutable(), youtubeDl, youtubeDlContainer);
        new verifyExecutable(Resources.getInstance().getFfmpegExecutable(), ffmpeg, ffmpegContainer);


        // Files
        String outputDirectoryRaw = Model.getInstance().settings.getSetting("output_directory").equals("") ?
                System.getProperty("user.dir") :
                Model.getInstance().settings.getSetting("output_directory");
        outputDirectory.setText(outputDirectoryRaw);
        musicFormat.getSelectionModel().select(Integer.parseInt(Model.getInstance().settings.getSetting("music_format")));
        saveAlbumArt.getSelectionModel().select(Integer.parseInt(Model.getInstance().settings.getSetting("save_album_art")));
        new validateDirectory(outputDirectoryRaw);

        // Audio
        advancedValidationToggle.setSelected(Model.getInstance().settings.getSettingBool("advanced_validation"));
        volumeCorrectionToggle.setSelected(Model.getInstance().settings.getSettingBool("volume_correction"));

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

        // Load theme
        root.getStylesheets().setAll(
                Main.class.getResource(
                        "resources/css/" + (Model.getInstance().settings.getSettingBool("dark_theme") ? "dark" : "standard") + ".css"
                ).toString()
        );

        Debug.trace("Initialized settings view.");

    }

    @FXML
    protected void searchView(Event event) {

        try {
            (
                    ((Node) event.getSource())
                            .getScene()
                            .getWindow()
            )
                    .getScene()
                    .setRoot(
                            FXMLLoader.load(
                                    Main.class.getResource("resources/fxml/search.fxml")
                            )
                    );
        } catch (IOException e) {
            Debug.error("Missing FXML File: Search.fxml", e);
        }

    }

    @FXML
    protected void selectNewFolder(javafx.scene.input.MouseEvent e) {

        JFileChooser newFolder = new JFileChooser();
        newFolder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        newFolder.showSaveDialog(null);

        outputDirectory.setText(newFolder.getSelectedFile().getPath());
        new validateDirectory(newFolder.getSelectedFile().getPath());
        saveSettings(e);

    }

    @FXML
    @SuppressWarnings("unchecked")
    protected void saveSettings(Event e) {

        String settingsFormatString = "Settings Changed %s: %s -> %s";

        // TODO: Consider for other settings previous states
        if ( (e.getSource()).getClass().equals(ToggleSwitch.class) ) {

            ToggleSwitch modifiedSetting = (ToggleSwitch) e.getSource();

            Debug.trace(
                    String.format(
                            settingsFormatString,
                            StringUtils.capitalize(
                                    StringUtils.join(
                                            StringUtils.splitByCharacterTypeCamelCase(
                                                    modifiedSetting.getId().substring(
                                                            0,
                                                            modifiedSetting.getId().length() - 6)
                                            ),
                                            StringUtils.SPACE
                                    )
                            ),
                            convertSwitchValue(!modifiedSetting.isSelected()),
                            convertSwitchValue(modifiedSetting.isSelected())
                    )
            );

        } else if ((e.getSource()).getClass().equals(ComboBox.class)) {

            ComboBox<String> modifiedSetting = (ComboBox<String>) e.getSource();

            Debug.trace(
                    String.format(
                            settingsFormatString,
                            StringUtils.capitalize(
                                    StringUtils.join(
                                            StringUtils.splitByCharacterTypeCamelCase(modifiedSetting.getId()),
                                            StringUtils.SPACE
                                    )
                            ),
                            (modifiedSetting.getId().equals("musicFormat") ? songReferences : albumArtOptions).get(
                                    Model.getInstance().settings.getSettingInt(
                                            CaseFormat.UPPER_CAMEL.to(
                                                    CaseFormat.LOWER_UNDERSCORE,
                                                    modifiedSetting.getId()
                                            )
                                    )
                            ),
                            modifiedSetting.getSelectionModel().getSelectedItem()
                    )
            );

        } else if ((e.getSource()).getClass().equals(Label.class)) {

            Label modifiedSetting = (Label) e.getSource();

            // Not correcting '' to the default dir is intentional as this is more representative of the real settings values vs effective
            Debug.trace(
                    String.format(
                            settingsFormatString,
                            StringUtils.capitalize(
                                    StringUtils.join(
                                            StringUtils.splitByCharacterTypeCamelCase(modifiedSetting.getId()),
                                            StringUtils.SPACE
                                    )
                            ),
                            Model.getInstance().settings.getSetting("output_directory"),
                            modifiedSetting.getText()
                    )
            );

        } else {

            Debug.warn("Unknown setting modification source: " + e.getSource());

        }

        JSONObject newSettings = getNewSettings();
        try {
            root.getStylesheets().setAll(
                    String.valueOf(
                            Main.class.getResource(
                                    "resources/css/"
                                            + (newSettings.getBoolean("dark_theme") ? "dark" : "standard")
                                            + ".css"
                            )
                    )
            );
        } catch (JSONException er) {
            Debug.error("Failed to find dark theme setting.", er);
        }

        Model.getInstance().settings.saveSettings(newSettings);
    }

    protected JSONObject getNewSettings() {

        JSONObject settings = new JSONObject();
        try {
            // Files
            settings.put("output_directory", outputDirectory.getText().equals(System.getProperty("user.dir")) ? "" : outputDirectory.getText());
            settings.put("music_format", musicFormat.getSelectionModel().getSelectedIndex());
            settings.put("save_album_art", saveAlbumArt.getSelectionModel().getSelectedIndex());

            // Audio
            settings.put("advanced_validation", advancedValidationToggle.isSelected());
            settings.put("volume_correction", volumeCorrectionToggle.isSelected());

            // Meta-Data
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
            Debug.error("Failed to generate new settings.", e);
        }

        return settings;


    }

    private String convertSwitchValue(boolean enabled) {
        return enabled ? "ENABLED" : "DISABLED";
    }

    // Sends a web-request to the github to check the latest version available
    class getLatestVersion implements Runnable {

        private final boolean suppressWarning;

        getLatestVersion(boolean suppressWarning) {
            this.suppressWarning = suppressWarning;

            Thread thread = new Thread(this, "get-latest-version");
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {

            try {
                Document githubRequestLatestVersion = Jsoup.connect(Resources.remoteVersionUrl).get();
                JSONObject jsonData = new JSONObject(githubRequestLatestVersion.text());
                Platform.runLater(() -> {
                    try {
                        latestVersion.setText(jsonData.getString("version"));
                        latestVersionContainer.getChildren().add(
                                new ImageView(
                                    new Image(
                                            Main.class.getResourceAsStream("resources/img/tick.png"),
                                            20,
                                            20,
                                            true,
                                            true
                                    )
                                )
                        );

                    } catch (JSONException ignored) {
                        Debug.warn("Found data syntactically incorrect.");
                    }
                });

            } catch (IOException | JSONException e) {
                if (!suppressWarning) {
                    Debug.warn("Failed to get latest version, connection issue.");
                    Platform.runLater(() -> {
                        latestVersion.setText("Unknown");
                        latestVersionContainer.getChildren().clear();
                        latestVersionContainer.getChildren().addAll(
                                latestVersion,
                                new ImageView(
                                        new Image(
                                                Main.class.getResourceAsStream("resources/img/warning.png"),
                                                20,
                                                20,
                                                true,
                                                true
                                        )
                                )
                        );
                    });
                }
                new awaitReconnection();
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
                            new getLatestVersion(true);
                            connectionAttempt.cancel();
                        }
                    } catch (IOException ignored) {}

                }
            }, 0, 1000);

        }

    }

    // Validate directory to confirm
    class validateDirectory implements Runnable {

        String directory;

        validateDirectory(String directory) {
            this.directory = directory;

            Thread thread = new Thread(this, "directory-validation");
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {

            // Check the actual directory, exists
            if (!Files.exists(Paths.get(directory))) {

                ImageView warningImage = new ImageView(new Image(
                        Main.class.getResourceAsStream("resources/img/warning.png"),
                        25,
                        25,
                        true,
                        true
                ));
                Tooltip.install(warningImage, new Tooltip("Failed to find the request directory, check it exists or select a new one."));

                // Warn user
                Platform.runLater(() ->
                        outputDirectoryContainer
                                .getChildren()
                                .add(warningImage)
                );

                Debug.trace("Output directory was not found.");


            } else {

                // Check read/write perms
                File checkerTempFile;

                do {
                    checkerTempFile = new File(directory + "/" + Math.random());
                } while (checkerTempFile.exists());

                if (!checkerTempFile.mkdir() || !checkerTempFile.delete()) {

                    ImageView warningImage = new ImageView(new Image(
                            Main.class.getResourceAsStream("resources/img/warning.png"),
                            25,
                            25,
                            true,
                            true
                    ));

                    Tooltip.install(warningImage,
                            new Tooltip("The program does not have permissions to write to this directory, please restart with elevated permissions or select a different directory.")
                    );

                    // Warn user
                    Platform.runLater(() ->
                        outputDirectoryContainer
                                .getChildren()
                                .add(warningImage)
                    );

                    Debug.trace("Output directory was found, but lacking write permissions.");

                } else {

                    Platform.runLater(() -> {
                        if (outputDirectoryContainer.getChildren().size() > 1)
                            outputDirectoryContainer.getChildren().setAll(outputDirectoryContainer.getChildren().get(0));
                    });

                }



            }


        }


    }

    // Attempts to test an execution
    static class verifyExecutable implements Runnable {

        private final String executablePath;
        private final Label element;
        private final HBox elementContainer;

        verifyExecutable(String executablePath, Label element, HBox elementContainer) {
            this.executablePath = executablePath;
            this.element = element;
            this.elementContainer = elementContainer;

            Thread thread = new Thread(this, "executable-execution");
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {

            try {
                // Will throw an error if not setup
                new ProcessBuilder(executablePath, "--version").start();

                Platform.runLater(() -> {
                    element.setText("Configured");
                    elementContainer.getChildren().add(
                            new ImageView(
                                    new Image(
                                            Main.class.getResourceAsStream("resources/img/tick.png"),
                                            20,
                                            20,
                                            true,
                                            true
                                    )
                            )
                    );
                });

            } catch (IOException ignored) {
                Debug.warn(String.format("Failed to verify executable: \"%s\"", executablePath));

                Platform.runLater(() -> {
                    element.setText("Not Configured");
                    elementContainer.getChildren().add(
                            new ImageView(
                                new Image(
                                        Main.class.getResourceAsStream("resources/img/warning.png"),
                                        20,
                                        20,
                                        true,
                                        true
                                )
                        )
                    );

                    // Installation requires admin permissions, hence verify the user is a admin, or inform them
                    if (System.getProperty("os.name").startsWith("Windows")) {
                        if (new File(System.getenv("ProgramFiles(X86)") + "/test/").mkdir() && new File(System.getenv("ProgramFiles(X86)") + "/test/").delete()) {
                            elementContainer.setCursor(Cursor.HAND);
                            Tooltip.install(elementContainer, new Tooltip("Click to configure"));
                            elementContainer.setOnMouseClicked(e ->
                                    new manageInstall(
                                        FilenameUtils.removeExtension(Paths.get(executablePath).getFileName().toString()),
                                        element,
                                        elementContainer
                                    )
                            );
                        } else Tooltip.install(elementContainer, new Tooltip("Easy installation requires elevated permissions, restart the program and try again."));
                    }
                });
            }

        }

        private static class manageInstall implements Runnable {

            final String executable;
            final Label element;
            final HBox elementContainer;

            manageInstall(String executable, Label element, HBox elementContainer) {

                System.out.println(executable);

                this.executable = executable;
                this.element = element;
                this.elementContainer = elementContainer;

                new Thread(this, "manage-install").start();
            }

            @Override
            public void run() {

                ProgressIndicator x = new ProgressIndicator();
                x.setMaxSize(20, 20);

                Platform.runLater(() -> {
                    element.setText("Configuring...");
                    elementContainer.setOnMouseClicked(null);
                    elementContainer.setCursor(Cursor.DEFAULT);
                    elementContainer.getChildren().set(1, x);
                });

                try {

                    if (executable.equals("youtube-dl") ? Install.getYoutubeDl() : Install.getFFMPEG())
                        Platform.runLater(() -> {
                            element.setText("Configured");
                            elementContainer.getChildren().set(
                                    1,
                                    new ImageView(
                                            new Image(
                                                    Main.class.getResourceAsStream("resources/img/tick.png"),
                                                    20,
                                                    20,
                                                    true,
                                                    true
                                            )
                                    )
                            );
                        });

                    else
                        Platform.runLater(() -> {
                            element.setText("Configure Manually");
                            elementContainer.getChildren().set(
                                    1,
                                    new ImageView(
                                            new Image(
                                                    Main.class.getResourceAsStream("resources/img/warning.png"),
                                                    20,
                                                    20,
                                                    true,
                                                    true
                                            )
                                    )
                            );}
                        );

                } catch (IOException e) {
                    Debug.warn("Failed to configure due to likely permission issues, despite that it should be blocked.");
                    Platform.runLater(() -> {element.setText("Configure Manually");
                        elementContainer.getChildren().set(
                                1,
                                new ImageView(
                                        new Image(
                                                Main.class.getResourceAsStream("resources/img/warning.png"),
                                                20,
                                                20,
                                                true,
                                                true
                                        )
                                )
                        );
                    });
                }
            }
        }
    }

}