package sample;

import com.mpatric.mp3agic.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.controlsfx.control.ToggleSwitch;
import javafx.stage.Stage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/*
Optimisations
TODO: Redesign with FXGL pages and a main controller instead of everything in one file
TODO: Switch to CountDownLatch instead of while (x); to wait for threads [https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CountDownLatch.html]
TODO: Settings view should just use smoother yes no buttons instead of ComboBoxes
TODO: Windows file explorer would preferable to JavaFX one
TODO: Added to table is a spam thread based way isn't particularly efficient, I'd switch back to the old method and allow for easier killing
TODO: Don't have white space on the search page before a download is started, have it taken up by the table and then repositioned later
TODO: Break bulky threads into functions internally
TODO: Remove utils into their respective threads
TODO: Where files cannot be access handle and regnerate respectively
TODO: Switch to using warnings where relevant

Known Bugs
TODO: Adding to table thread appears to cause lots of internal errors at times, no actual debugging information given, perhaps calling too fast?
TODO: Interruption in downloading such as internet or file-io should be accounted for
TODO: Switching to night theme and then exiting and reentering settings won't colour the text properly, also throws a lot of errors when night theme is set
TODO: Cancel should kill youtube-dl listener threads
TODO: Don't add elements with no name to search results table on either
TODO: Table adder will mess with the exiting, try to deal with that
TODO: Downloading twice appears to cause issues
TODO: Error message can be hidden under autocomplete results, also invalid search seems to not let me search again

Features
TODO: Duplicate album arts could be checked for and replaced to use the same ID if the art is identical
TODO: Escape key should cancel search in case of lag or anything
TODO: Recalculate the estimated time based off of youtube-dl's estimates
TODO: Add button to install and configure youtube-dl & ffmpeg
TODO: Have a download progress view a bit similar to steam, downloaded songs and such like
TODO: Once that page is implemented add a download queue system for multiple albums and such

Future, for when the application is effectively done
    Git
    TODO: Add a README

    Optimisations
    TODO: Rewrite Main.css and redesign general look of the application
    TODO: Fix warnings
    TODO: Change a lot of start content of a java-fx page view
    TODO: Go through each function and optimise

    Known Bugs
    TODO: Get reu to try and break this

    Features
    TODO: Look into macOS and Linux compatibility

    Misc
    TODO: Add testing
    TODO: Use debugging and error classes properly
    TODO: Increase use of debugging and logging of errors
*/

public class View implements EventHandler<KeyEvent>
{
    public Controller controller;

    public int width;
    public int height;

    public Scene scene;
    public Pane pane;
    public Canvas canvas;
    public Stage mainWindow;

    // Settings
    public String outputDirectorySetting;
    public String OutputDirectorySettingNew;
    public int musicFormatSetting;
    public int saveAlbumArtSetting;

    public boolean applyAlbumArt;
    public boolean applyAlbumTitle;
    public boolean applySongTitle;
    public boolean applyArtist;
    public boolean applyYear;
    public boolean applyTrack;

    public boolean darkTheme;
    public boolean dataSaver;

    // Search & Download
    public String programVersion;
    public TextField searchRequest;
    public ImageView loadingIcon;
    public Label searchErrorMessage;
    public TableView resultsTable;
    public Label searchResultsTitle;
    public Label title;
    public Button downloadButton;
    public Button cancelButton;
    public ProgressBar loading;
    public Label searchesProgressText;
    public Line footerMarker;
    public Label settingsLink;
    public Label downloadsLink;
    public TableView autocompleteResultsTable;
    public Label downloadSpeedLabel;
    public Label timeRemainingLabel;
    public Label backgroundDownload;
    public Label downloadsViewLink;
    public ProgressIndicator queueAdditionProgress;

    // Settings
    VBox versionResultContainer;
    VBox latestVersionResultContainer;
    VBox youtubeDlVerificationResultContainer;
    VBox ffmpegVerificationResultContainer;

    VBox outputDirectoryContainer;
    VBox outputDirectoryResultContainer;
    VBox songDownloadFormatResultContainer;
    VBox songDownloadFormatContainer;
    VBox saveAlbumArtResultContainer;
    VBox saveAlbumArtContainer;

    VBox metaDataInfoContainer;
    VBox albumArtSettingResultContainer;
    VBox albumTitleSettingResultContainer;
    VBox songTitleSettingResultContainer;
    VBox artistSettingResultContainer;
    VBox yearSettingResultContainer;
    VBox trackNumberSettingResultContainer;

    VBox darkModeSettingResultContainer;
    VBox dataSaverSettingResultContainer;

    public ScrollPane settingsContainer;
    public BorderPane settingsContainerBase;
    public Label settingsTitle;

    // Program
    public Label programSettingsTitle;
    public Label version;
    public Label versionResult;
    public Label latestVersion;
    public Label latestVersionResult;
    public Label youtubeDlVerification;
    public Label youtubeDlVerificationResult;
    public Label ffmpegVerification;
    public Label ffmpegVerificationResult;

    // File
    public Label fileSettingsTitle;
    public Label outputDirectory;
    public Label outputDirectoryResult;
    public Label songDownloadFormat;
    public ComboBox<String> songDownloadFormatResult;
    public Label saveAlbumArt;
    public ComboBox<String> saveAlbumArtResult;

    // Metadata
    public Label metaDataTitle;

    public BorderPane albumArtLine;
    public Label albumArtSetting;
    public ToggleSwitch albumArtSettingResult;
    public Label albumTitleSetting;
    public ToggleSwitch albumTitleSettingResult;
    public Label songTitleSetting;
    public ToggleSwitch songTitleSettingResult;
    public Label artistSetting;
    public ToggleSwitch artistSettingResult;
    public Label yearSetting;
    public ToggleSwitch yearSettingResult;
    public Label trackNumberSetting;
    public ToggleSwitch trackNumberSettingResult;

    // Application
    public Label applicationSettingTitle;
    public Label darkModeSetting;
    public ComboBox<String> darkModeSettingResult;
    public Label dataSaverSetting;
    public ComboBox<String> dataSaverSettingResult;

    // Buttons
    public Button confirmChanges;
    public Button cancelBackButton;

    // Settings Lines
    public Line settingTitleLine;
    public Line programSettingsTitleLine;
    public Line fileSettingsTitleLine;
    public Line metaDataTitleLine;
    public Line applicationSettingTitleLine;

    // Downloads View
    public Label downloadsTitle;
    public ScrollPane downloadsInfoScrollPane;
    public VBox downloadsInfo;
    public Label downloadTotal;
    public LineChart downloadGraph;
    public ListView downloadEventsView;
    public Button downloadBack;

    // Data
    public ArrayList<ArrayList<String>> searchResults;
    public Utils.resultsSet[] searchResultFullData;
    public ArrayList<Utils.resultsSet> resultsData;
    public Timer timerRotate;
    public ArrayList<Long> lastClicked = new ArrayList<>();
    public JSONArray downloadQueue = new JSONArray();

    public ArrayList<String> formatReferences = new ArrayList<>(Arrays.asList("mp3", "wav", "ogg", "aac"));

    List<Object> threadManagement = new ArrayList<>();

    generateAutocomplete autocompleteGenerator;
    downloadsListener downloader;
    addToQueue queueHandler;
    allMusicQuery searchQuery;
    outputDirectoryVerification directoryVerify;
    outputDirectoryListener directoryListener;

    public View(int w, int h) {
        Debug.trace(null, "View::<constructor>");
        width = w;
        height = h;
    }

    public void start(Stage window) {

        // Continually check every 5 minutes to optimise the cache
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(optimiseCache::new, 0, 300, TimeUnit.SECONDS);

        /* Checking Resources Exist, Will Delay Main Thread but this is the intentional, as can't continue without these resources */
        ArrayList<String> reacquire = new ArrayList<>();
        String[] targetCSSFiles = new String[]{"main.css", "night_theme.css", "normal_theme.css"};
        String[] targetIMGFiles = new String[]{"album_default.png", "loading.png", "song_default.png"};

        // CSS can be whatever, they just need to exist
        for (String file: targetCSSFiles) {
            if (!Files.exists(Paths.get("resources\\css\\" + file)))
                reacquire.add("css/" + file);
        }
        for (String file: targetIMGFiles) {
            try {
                if (!Files.exists(Paths.get("resources\\" + file)) || ImageIO.read(new File("resources/" + file)) == null)
                    reacquire.add(file);
            } catch (IOException ignored) {
                ignored.printStackTrace();
                System.out.println("IO: " + file);
                reacquire.add(file);
            }
        }

        for (String downloadFile: reacquire) {

            try {

                FileUtils.copyURLToFile(
                        new URL("https://raw.githubusercontent.com/ByronFiler/MusicDownloader/master/resources/" + downloadFile),
                        new File("resources/" + downloadFile)
                );

                Debug.trace(null, "Reaquired resource: " + downloadFile);

            } catch (IOException e) {
                Debug.error(null, "Failed to reacquire resource: " + downloadFile, e.getStackTrace());
                System.exit(-1);
            }

        }

        /* JAVA-FX INITIALISATION */
        pane = new Pane();
        pane.setId("initial");

        canvas = new Canvas(width, height);
        pane.getChildren().add(canvas);

        /* LOADING DATA */
        threadManagement.add(new ArrayList<>(Arrays.asList("smartQuitDownload", new smartQuitDownload())));

        mainWindow = window;
        JSONObject config = SettingsFunc.getSettings();

        try {
            outputDirectorySetting = config.get("output_directory").toString();
            OutputDirectorySettingNew = outputDirectorySetting;
            musicFormatSetting = (int) config.get("music_format");
            saveAlbumArtSetting = (int) config.get("save_album_art");

            threadManagement.add(new ArrayList<>(Arrays.asList("outputDirectoryVerification", new outputDirectoryVerification())));

            applyAlbumArt = (int) config.get("album_art") != 0;
            applyAlbumTitle = (int) config.get("album_title") != 0;
            applySongTitle = (int) config.get("song_title") != 0;
            applyArtist = (int) config.get("artist") != 0;
            applyYear = (int) config.get("year") != 0;
            applyTrack = (int) config.get("track") != 0;

            darkTheme = (int) config.get("theme") != 0;
            dataSaver = (int) config.get("data_saver") != 0;

        } catch (JSONException e) {

            // Should really regenerate all files, although this shouldn't happen
            Debug.error(null, "Failed to find valid settings to load, even after files should have been checked.", e.getStackTrace());
            System.exit(-1);

        }

        programVersion = SettingsFunc.getVersion();

        downloader = new downloadsListener();

        /* JAVA-FX DESIGN */

        /* Search Page */
        title = new Label("Music Downloader");
        title.setId("title");

        searchRequest = new TextField();
        searchRequest.setId("search");
        searchRequest.setPrefSize(400, 20);
        searchRequest.textProperty().addListener((observableValue, s, t1) -> generateQueryAutocomplete(t1));

        loadingIcon = new ImageView(new Image((new File("resources/loading.png").toURI().toString())));
        loadingIcon.setVisible(false);

        searchErrorMessage = new Label();
        searchErrorMessage.setVisible(false);
        searchErrorMessage.setId("searchError");

        footerMarker = new Line(0, 0, 0, 0);
        footerMarker.setId("line");

        settingsLink = new Label("Settings");
        settingsLink.setTranslateX(10);
        settingsLink.setId("subTitle2");
        settingsLink.setCursor(Cursor.HAND);
        settingsLink.setOnMouseClicked(e -> settingsMode());

        downloadsLink = new Label("Downloads");
        downloadsLink.setId("subTitle2");
        downloadsLink.setCursor(Cursor.HAND);
        downloadsLink.setOnMouseClicked(e -> downloadMode());

        /* Search Results Page */
        searchResultsTitle = new Label("Search Results");
        searchResultsTitle.setId("subTitle");
        searchResultsTitle.setVisible(false);
        searchResultsTitle.setTranslateX(50);
        searchResultsTitle.setTranslateY(10);

        resultsTable = new TableView<PropertyValueFactory<TableColumn<String, Utils.resultsSet>, Utils.resultsSet>>();
        resultsTable.setId("table");
        resultsTable.getSelectionModel().selectedIndexProperty().addListener((obs, oldSelection, newSelection) -> downloadButton.setDisable(newSelection == null));
        resultsTable.setTranslateX(50);
        resultsTable.setTranslateY(50);
        resultsTable.setVisible(false);

        TableColumn<String, Utils.resultsSet> albumArtColumn = new TableColumn<>("Album Art");
        albumArtColumn.setCellValueFactory(new PropertyValueFactory<>("albumArt"));

        TableColumn<String, Utils.resultsSet> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.prefWidthProperty().bind(resultsTable.widthProperty().multiply(0.2).add(-15));

        TableColumn<String, Utils.resultsSet> artistColumn = new TableColumn<>("Artist");
        artistColumn.setCellValueFactory(new PropertyValueFactory<>("artist"));
        artistColumn.prefWidthProperty().bind(resultsTable.widthProperty().multiply(0.2).add(-15));

        TableColumn<String, Utils.resultsSet> yearColumn = new TableColumn<>("Year");
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));
        yearColumn.prefWidthProperty().bind(resultsTable.widthProperty().multiply(0.2).add(-15));

        TableColumn<String, Utils.resultsSet> genreColumn = new TableColumn<>("Genre");
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        genreColumn.prefWidthProperty().bind(resultsTable.widthProperty().multiply(0.2).add(-15));

        TableColumn<String, Utils.resultsSet> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.prefWidthProperty().bind(resultsTable.widthProperty().multiply(0.2).add(-15));

        resultsTable.getColumns().addAll(
                albumArtColumn,
                titleColumn,
                artistColumn,
                yearColumn,
                genreColumn,
                typeColumn
        );

        autocompleteResultsTable = new TableView<PropertyValueFactory<TableColumn<String, Utils.autocompleteResultsSet>, Utils.autocompleteResultsSet>>();
        autocompleteResultsTable.setVisible(false);
        autocompleteResultsTable.getStyleClass().add("noheader");
        autocompleteResultsTable.setId("autocompleteTable");
        autocompleteResultsTable.prefWidthProperty().bind(searchRequest.widthProperty());
        autocompleteResultsTable
                .getSelectionModel()
                .selectedIndexProperty()
                .addListener(
                        (obs,
                         oldSelection,
                         newSelection
                        ) -> {
                            try {
                                searchRequest.setText(
                                        (
                                                (Utils.autocompleteResultsSet) autocompleteResultsTable
                                                        .getItems()
                                                        .get((Integer) newSelection)
                                        ).getName()
                                );
                            } catch (IndexOutOfBoundsException ignored) {}
                        }
                );

        TableColumn<String, Utils.resultsSet> iconColumn = new TableColumn<>();
        iconColumn.setCellValueFactory(new PropertyValueFactory<>("icon"));
        iconColumn.prefWidthProperty().bind(autocompleteResultsTable.widthProperty().multiply(0).add(32));

        TableColumn<String, Utils.resultsSet> nameColumn = new TableColumn<>();
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.prefWidthProperty().bind(autocompleteResultsTable.widthProperty().add(-25));

        autocompleteResultsTable.getColumns().addAll(
                iconColumn,
                nameColumn
        );

        downloadButton = new Button("Download");
        downloadButton.setPrefSize(120, 40);
        downloadButton.setTranslateX(50);
        downloadButton.setOnAction(
                e -> {
                    downloadButton.setText("Adding to queue...");
                    queueAdditionProgress.setVisible(true);
                    downloadButton.setDisable(true);
                    cancelButton.setText("Cancel");

                    queueHandler = new addToQueue(
                            searchResults.get(
                                    resultsData.indexOf(
                                            resultsTable
                                                    .getItems()
                                                    .get(
                                                            resultsTable
                                                                    .getSelectionModel()
                                                                    .getSelectedIndex()
                                                    )
                                    )
                            )
                    );
                    cancelButton.setOnMouseClicked(f -> queueHandler.kill());

                    // Once the thread has finished, should restore to previous states
                    // If the cancel button is clicked should kill the thread and restore to previous states
                }
        );
        downloadButton.setVisible(false);

        cancelButton = new Button("Back");
        cancelButton.setPrefSize(120, 40);
        cancelButton.setOnMouseClicked(e -> cancel(true));
        cancelButton.setVisible(false);

        /* Search Results Page: Downloads */
        searchesProgressText = new Label("Locating Songs: 0%");
        searchesProgressText.setTranslateX(50);
        searchesProgressText.setVisible(false);

        timeRemainingLabel = new Label();
        timeRemainingLabel.setVisible(false);

        downloadSpeedLabel = new Label();
        downloadSpeedLabel.setVisible(false);

        loading = new ProgressBar();
        loading.setProgress(0);
        loading.setTranslateX(50);
        loading.setPrefHeight(20);
        loading.setVisible(false);

        downloadsViewLink = new Label("View Downloads");
        downloadsViewLink.setUnderline(true);
        downloadsViewLink.setOnMouseClicked(e -> downloadMode());
        downloadsViewLink.setCursor(Cursor.HAND);
        downloadsViewLink.setTranslateX(50);
        downloadsViewLink.setVisible(false);

        backgroundDownload = new Label("Minimize Download");
        backgroundDownload.setOnMouseClicked(e -> cancel(false));
        backgroundDownload.setCursor(Cursor.HAND);
        backgroundDownload.setUnderline(true);
        backgroundDownload.setVisible(false);

        queueAdditionProgress = new ProgressIndicator(0);
        queueAdditionProgress.setMinSize(50, 50);
        queueAdditionProgress.setId("progressIndicator");
        queueAdditionProgress.setVisible(false);

        /* Settings Page */
        settingsTitle = new Label("Settings");
        settingsTitle.setId("subTitle");

        programSettingsTitle = new Label("Information");
        programSettingsTitle.setId("settingsHeader");

        version = new Label("Version: ");
        version.setId("settingInfo");

        versionResult = new Label(programVersion == null ? "Unknown" : programVersion);
        versionResult.setId("settingInfo");

        latestVersion = new Label("Latest Version: ");
        latestVersion.setId("settingInfo");

        latestVersionResult = new Label("Locating...");
        latestVersionResult.setId("settingInfo");
        if (dataSaver)
            threadManagement.add(new ArrayList<>(Arrays.asList("getLatestVersion",new getLatestVersion())));

        youtubeDlVerification = new Label("YouTube-DL Status: ");
        youtubeDlVerification.setId("settingInfo");

        youtubeDlVerificationResult = new Label("Checking...");
        youtubeDlVerificationResult.setId("settingInfo");

        ffmpegVerification = new Label("FFMPEG Status: ");
        ffmpegVerification.setId("settingInfo");

        ffmpegVerificationResult = new Label("Checking...");
        ffmpegVerificationResult.setId("settingInfo");

        fileSettingsTitle = new Label("Files");
        fileSettingsTitle.setId("settingsHeader");

        outputDirectory = new Label("Save music to: ");
        outputDirectory.setId("settingInfo");

        outputDirectoryResult = new Label(outputDirectorySetting.equals("") ? System.getProperty("user.dir") : outputDirectorySetting);
        outputDirectoryResult.setOnMouseClicked(
                e -> threadManagement.add(
                        new ArrayList<>(
                                Arrays.asList(
                                        "selectFolder",
                                        new selectFolder()
                                )
                        )
                )
        );
        outputDirectoryResult.setCursor(Cursor.HAND);
        outputDirectoryResult.setId("settingInfo");

        songDownloadFormat = new Label("Music format: ");
        songDownloadFormat.setId("settingInfo");

        songDownloadFormatResult = new ComboBox<>(FXCollections.observableArrayList("mp3", "wav", "ogg", "aac"));
        songDownloadFormatResult.setOnAction(e -> {setMetaDataVisibility(); evaluateSettingsChanges();} );
        songDownloadFormatResult.setId("combobox");

        saveAlbumArt = new Label("Save Album Art: ");
        saveAlbumArt.setId("settingInfo");

        saveAlbumArtResult = new ComboBox<>(FXCollections.observableArrayList("No", "Albums Only", "Songs Only", "All"));
        saveAlbumArtResult.setOnAction(e -> evaluateSettingsChanges());
        saveAlbumArtResult.setId("combobox");

        metaDataTitle = new Label("Meta-Data Application");
        metaDataTitle.setId("settingsHeader");

        // Album art design
        albumArtSetting = new Label("Album Art: ");
        albumArtSetting.setId("settingInfo");

        albumArtSettingResult = new ToggleSwitch();
        albumArtSettingResult.setOnMouseClicked(e -> evaluateSettingsChanges());

        albumArtLine = new BorderPane();
        albumArtLine.setLeft(albumArtSetting);
        albumArtLine.setRight(albumArtSettingResult);

        albumTitleSetting = new Label("Album Title: ");
        albumTitleSetting.setId("settingInfo");

        albumTitleSettingResult = new ToggleSwitch();
        albumTitleSettingResult.setOnMouseClicked(e -> evaluateSettingsChanges());

        songTitleSetting = new Label("Song Title: ");
        songTitleSetting.setId("settingInfo");

        songTitleSettingResult = new ToggleSwitch();
        songTitleSettingResult.setOnMouseClicked(e -> evaluateSettingsChanges());

        artistSetting = new Label("Artist: ");
        artistSetting.setId("settingInfo");

        artistSettingResult = new ToggleSwitch();
        artistSettingResult.setOnMouseClicked(e -> evaluateSettingsChanges());

        yearSetting = new Label("Year: ");
        yearSetting.setId("settingInfo");

        yearSettingResult = new ToggleSwitch();
        yearSettingResult.setOnMouseClicked(e -> evaluateSettingsChanges());

        trackNumberSetting = new Label("Track Number: ");
        trackNumberSetting.setId("settingInfo");

        trackNumberSettingResult = new ToggleSwitch();
        trackNumberSettingResult.setOnMouseClicked(e -> evaluateSettingsChanges());
        trackNumberSettingResult.setTranslateY(480);

        applicationSettingTitle = new Label("Application Configuration");
        applicationSettingTitle.setId("settingsHeader");

        darkModeSetting = new Label("Theme: ");
        darkModeSetting.setId("settingInfo");

        darkModeSettingResult = new ComboBox<>(FXCollections.observableArrayList("Normal", "Night"));
        darkModeSettingResult.setOnAction(e -> {evaluateSettingsChanges(); switchTheme(darkModeSettingResult.getSelectionModel().getSelectedIndex() == 1);});
        darkModeSettingResult.setId("combobox");

        dataSaverSetting = new Label("Data Saver Mode: ");
        dataSaverSetting.setId("settingInfo");

        dataSaverSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        dataSaverSettingResult.setOnAction(e -> evaluateSettingsChanges());
        dataSaverSettingResult.setId("combobox");

        confirmChanges = new Button("Confirm");
        confirmChanges.setOnAction(e -> submit());
        confirmChanges.setId("confirm_button");
        confirmChanges.setPrefSize(100, 50);
        confirmChanges.setTranslateX(30);
        confirmChanges.setVisible(false);

        cancelBackButton = new Button("Back");
        cancelBackButton.setOnAction(e -> searchMode());
        cancelBackButton.setId("button");
        cancelBackButton.setPrefSize(100, 50);
        cancelBackButton.setVisible(false);

        // Settings Lines
        settingTitleLine = new Line(0, 0, 0, 0);
        settingTitleLine.setId("line");

        programSettingsTitleLine = new Line(0, 0, 0, 0);
        programSettingsTitleLine.setId("line");

        fileSettingsTitleLine = new Line(0, 0, 0, 0);
        fileSettingsTitleLine.setId("line");

        metaDataTitleLine = new Line(0, 0, 0, 0);
        metaDataTitleLine.setId("line");

        applicationSettingTitleLine = new Line(0, 0, 0, 0);
        applicationSettingTitleLine.setId("line");

        // "Top" Title Section
        VBox titleContainer = new VBox();
        titleContainer.getChildren().addAll(settingsTitle, settingTitleLine);

        // Program Settings Title & Line
        VBox programSettingNameContainer = new VBox();
        programSettingNameContainer.getChildren().addAll(programSettingsTitle, programSettingsTitleLine);
        programSettingNameContainer.setPadding(new Insets(20, 0, 0, 0));

        // Program Settings Information
        VBox programSettingInfoContainer = new VBox();
        programSettingInfoContainer.getChildren().addAll(
                version,
                latestVersion,
                youtubeDlVerification,
                ffmpegVerification
        );
        programSettingInfoContainer.setPadding(new Insets(50, 0, 0, 0));

        // Program Settings Data: YoutubeDlVerification
        versionResultContainer = new VBox();
        versionResultContainer.getChildren().add(versionResult);

        // Program Settings Data: YoutubeDlVerification
        latestVersionResultContainer = new VBox();
        latestVersionResultContainer.getChildren().add(latestVersionResult);

        // Program Settings Data: YoutubeDlVerification
        youtubeDlVerificationResultContainer = new VBox();
        youtubeDlVerificationResultContainer.getChildren().add(youtubeDlVerificationResult);

        // Program Settings Data: ffmpegVerification
        ffmpegVerificationResultContainer = new VBox();
        ffmpegVerificationResultContainer.getChildren().add(ffmpegVerificationResult);

        // File Settings Title & Line
        VBox fileSettingNameContainer = new VBox();
        fileSettingNameContainer.getChildren().addAll(fileSettingsTitle, fileSettingsTitleLine);
        fileSettingNameContainer.setPadding(new Insets(150, 0, 0, 0));

        outputDirectoryContainer = new VBox();
        outputDirectoryContainer.getChildren().add(outputDirectory);
        outputDirectoryContainer.setPadding(new Insets(180, 0, 0, 0));

        songDownloadFormatContainer = new VBox();
        songDownloadFormatContainer.getChildren().add(songDownloadFormat);
        songDownloadFormatContainer.setPadding(new Insets(210, 0, 0, 0));

        saveAlbumArtContainer = new VBox();
        saveAlbumArtContainer.getChildren().add(saveAlbumArt);
        saveAlbumArtContainer.setPadding(new Insets(240, 0, 0, 0));

        outputDirectoryResultContainer = new VBox();
        outputDirectoryResultContainer.getChildren().add(outputDirectoryResult);

        songDownloadFormatResultContainer = new VBox();
        songDownloadFormatResultContainer.getChildren().add(songDownloadFormatResult);

        saveAlbumArtResultContainer = new VBox();
        saveAlbumArtResultContainer.getChildren().add(saveAlbumArtResult);

        VBox metaDataNameContainer = new VBox();
        metaDataNameContainer.getChildren().addAll(metaDataTitle, metaDataTitleLine);
        metaDataNameContainer.setPadding(new Insets(280, 0, 0, 0));

        metaDataInfoContainer = new VBox();
        metaDataInfoContainer.getChildren().addAll(
                albumArtSetting,
                albumTitleSetting,
                songTitleSetting,
                artistSetting,
                yearSetting,
                trackNumberSetting
        );
        metaDataInfoContainer.setSpacing(12.5);
        metaDataInfoContainer.setPadding(new Insets(310, 0, 0, 0));

        albumArtSettingResultContainer = new VBox();
        albumArtSettingResultContainer.getChildren().add(albumArtSettingResult);

        albumTitleSettingResultContainer = new VBox();
        albumTitleSettingResultContainer.getChildren().add(albumTitleSettingResult);

        songTitleSettingResultContainer = new VBox();
        songTitleSettingResultContainer.getChildren().add(songTitleSettingResult);

        artistSettingResultContainer = new VBox();
        artistSettingResultContainer.getChildren().add(artistSettingResult);

        yearSettingResultContainer = new VBox();
        yearSettingResultContainer.getChildren().add(yearSettingResult);

        trackNumberSettingResultContainer = new VBox();
        trackNumberSettingResultContainer.getChildren().add(trackNumberSettingResult);

        VBox applicationSettingNameContainer = new VBox();
        applicationSettingNameContainer.getChildren().addAll(applicationSettingTitle, applicationSettingTitleLine);
        applicationSettingNameContainer.setPadding(new Insets(510, 0, 0, 0));

        VBox applicationSettingsInfoContainer = new VBox(7.5);
        applicationSettingsInfoContainer.getChildren().addAll(darkModeSetting, dataSaverSetting);
        applicationSettingsInfoContainer.setPadding(new Insets(540, 0, 0, 0));

        darkModeSettingResultContainer = new VBox();
        darkModeSettingResultContainer.getChildren().add(darkModeSettingResult);

        dataSaverSettingResultContainer = new VBox();
        dataSaverSettingResultContainer.getChildren().add(dataSaverSettingResult);

        // Left Stack Pane Titles, Lines, Information
        Pane leftStack = new Pane();
        leftStack.getChildren().addAll(
                programSettingNameContainer,
                programSettingInfoContainer,
                fileSettingNameContainer,
                outputDirectoryContainer,
                songDownloadFormatContainer,
                saveAlbumArtContainer,
                metaDataNameContainer,
                metaDataInfoContainer,
                applicationSettingNameContainer,
                applicationSettingsInfoContainer
        );

        // Right Stack Pane, Data
        Pane rightStack = new Pane();
        rightStack.getChildren().addAll(
                dataSaverSettingResultContainer,
                darkModeSettingResultContainer,
                trackNumberSettingResultContainer,
                yearSettingResultContainer,
                artistSettingResultContainer,
                songTitleSettingResultContainer,
                albumTitleSettingResultContainer,
                albumArtSettingResultContainer,
                saveAlbumArtResultContainer,
                songDownloadFormatResultContainer,
                outputDirectoryResultContainer,
                ffmpegVerificationResultContainer,
                youtubeDlVerificationResultContainer,
                latestVersionResultContainer,
                versionResultContainer
        );

        // Settings Scroll-pane
        settingsContainerBase = new BorderPane();
        settingsContainerBase.setPadding(new Insets(10, 10, 10, 10));
        settingsContainerBase.setTop(titleContainer);
        settingsContainerBase.setLeft(leftStack);
        settingsContainerBase.setRight(rightStack);

        // Overall Container
        settingsContainer = new ScrollPane();
        settingsContainer.setContent(settingsContainerBase);
        settingsContainer.setId("settingsHandler");
        settingsContainer.setTranslateX(30);
        settingsContainer.setTranslateY(20);

        settingsContainer.setVisible(false);

        // Downloads Page Vars
        downloadsTitle = new Label("Downloads");
        downloadsTitle.setTranslateX(20);
        downloadsTitle.setTranslateY(20);
        downloadsTitle.setVisible(false);
        downloadsTitle.setId("title");

        downloadSpeedLabel = new Label();
        downloadTotal = new Label();

        downloadsInfo = new VBox();
        downloadsInfo.getChildren().addAll(downloadSpeedLabel, downloadTotal);

        downloadsInfoScrollPane = new ScrollPane();
        downloadsInfoScrollPane.setContent(downloadsInfo);
        downloadsInfoScrollPane.setTranslateX(20);
        downloadsInfoScrollPane.setTranslateY(60);
        downloadsInfoScrollPane.setVisible(false);

        downloadGraph = new LineChart<>(new NumberAxis(), new NumberAxis());
        downloadGraph.setTranslateY(60);
        downloadGraph.setVisible(false);

        downloadEventsView = new ListView<BorderPane>();
        downloadEventsView.setTranslateX(20);
        downloadEventsView.setVisible(false);
        downloadEventsView.setId("downloadHistory");

        downloadBack = new Button("Back");
        downloadBack.setPrefSize(120, 40);
        downloadBack.setOnMouseClicked(e -> {

            // Should clear table, hide download contents and show search contents
            downloadsTitle.setVisible(false);
            downloadsInfoScrollPane.setVisible(false);
            downloadGraph.setVisible(false);
            downloadEventsView.setVisible(false);
            downloadBack.setVisible(false);
            downloadEventsView.getItems().clear();

            title.setVisible(true);
            searchRequest.setVisible(true);
            footerMarker.setVisible(true);
            settingsLink.setVisible(true);
            downloadsLink.setVisible(true);

        });
        downloadBack.setVisible(false);

        // Search Page
        pane.getChildren().addAll(
                title,
                searchRequest,
                loadingIcon,
                searchErrorMessage,
                autocompleteResultsTable,
                footerMarker,
                settingsLink,
                downloadsLink
        );

        // Search Page
        pane.getChildren().addAll(
                searchResultsTitle,
                resultsTable,
                downloadButton,
                cancelButton
        );
        // Search Page: Downloads
        pane.getChildren().addAll(
                loading,
                searchesProgressText,
                timeRemainingLabel,
                downloadSpeedLabel,
                downloadsViewLink,
                backgroundDownload,
                queueAdditionProgress
        );

        // Settings Page
        pane.getChildren().addAll(
                settingsContainer,
                confirmChanges,
                cancelBackButton
        );

        // Downloads Page
        pane.getChildren().addAll(
                downloadsTitle,
                downloadsInfoScrollPane,
                downloadGraph,
                downloadEventsView,
                downloadBack
        );

        scene = new Scene(pane);
        scene.getStylesheets().add("file:///" + new File("resources/css/main.css").getAbsolutePath().replace("\\", "/"));
        switchTheme(darkTheme);
        scene.setOnKeyPressed(this);

        window.setScene(scene);
        window.setTitle("Music Downloader");

        window.show();

        // Bindings: Search Page
        title.layoutYProperty().bind(mainWindow.heightProperty().divide(2).add(-119.5));
        title.layoutXProperty().bind(mainWindow.widthProperty().divide(2).add(-title.getWidth() / 2));
        searchRequest.layoutXProperty().bind(mainWindow.widthProperty().divide(2).add(- searchRequest.getWidth() / 2));
        searchRequest.layoutYProperty().bind(mainWindow.heightProperty().divide(2).add(-79.5));
        loadingIcon.fitHeightProperty().bind(searchRequest.heightProperty());
        loadingIcon.fitWidthProperty().bind(searchRequest.heightProperty());
        loadingIcon.layoutXProperty().bind(mainWindow.widthProperty().divide(2).add(searchRequest.widthProperty().divide(2).add(5)));
        loadingIcon.layoutYProperty().bind(mainWindow.heightProperty().divide(2).subtract(79.5));
        searchErrorMessage.layoutXProperty().bind(mainWindow.widthProperty().divide(2).subtract(searchErrorMessage.widthProperty().divide(2)));
        searchErrorMessage.layoutYProperty().bind(mainWindow.heightProperty().divide(2).subtract(50));
        autocompleteResultsTable.layoutXProperty().bind(mainWindow.widthProperty().divide(2).add(- searchRequest.getWidth() / 2));
        autocompleteResultsTable.layoutYProperty().bind(mainWindow.heightProperty().divide(2).add(-56));
        autocompleteResultsTable.maxHeightProperty().bind(mainWindow.heightProperty().divide(2).subtract(185));

        footerMarker.endXProperty().bind(mainWindow.widthProperty());
        footerMarker.startYProperty().bind(mainWindow.heightProperty().subtract(89));
        footerMarker.endYProperty().bind(mainWindow.heightProperty().subtract(89));
        settingsLink.layoutYProperty().bind(mainWindow.heightProperty().subtract(79));
        downloadsLink.layoutYProperty().bind(mainWindow.heightProperty().subtract(79));
        downloadsLink.layoutXProperty().bind(mainWindow.widthProperty().subtract(downloadsLink.widthProperty().add(19.5 + 10)));

        // Bindings: Settings
        settingsContainer.prefWidthProperty().bind(mainWindow.widthProperty().subtract(73));
        settingsContainer.prefHeightProperty().bind(mainWindow.heightProperty().subtract(confirmChanges.getHeight()+104));

        versionResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(50, 0, 0, -versionResult.getWidth())));
        latestVersionResultContainer.setPadding(new Insets(70, 0, 0, -latestVersionResult.getWidth()));
        youtubeDlVerificationResultContainer.setPadding(new Insets(90, 0, 0, -youtubeDlVerificationResult.getWidth()));
        ffmpegVerificationResultContainer.setPadding(new Insets(110, 0, 0, -ffmpegVerificationResult.getWidth()));

        outputDirectoryResultContainer.setPadding(new Insets(180, 0, 0, -outputDirectoryResult.getWidth()));
        songDownloadFormatResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(210, 0, 0, -songDownloadFormatResult.getWidth())));
        saveAlbumArtResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(240, 0, 0, -saveAlbumArtResult.getWidth())));

        albumArtSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(310, 0, 0, -albumArtSettingResult.getWidth())));
        albumTitleSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(340, 0, 0, -albumTitleSettingResult.getWidth())));
        songTitleSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(370, 0, 0, -songTitleSettingResult.getWidth())));
        artistSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(400, 0, 0, -artistSettingResult.getWidth())));
        yearSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(430, 0, 0, -yearSettingResult.getWidth())));
        trackNumberSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(-20, 0, 0, -trackNumberSettingResult.getWidth())));

        darkModeSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(540, 0, 0, -darkModeSettingResult.getWidth())));
        dataSaverSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(570, 0, 0, -dataSaverSettingResult.getWidth())));

        settingTitleLine.endXProperty().bind(mainWindow.widthProperty().subtract(99.5));
        programSettingsTitleLine.endXProperty().bind(mainWindow.widthProperty().subtract(99.5));
        fileSettingsTitleLine.endXProperty().bind(mainWindow.widthProperty().subtract(99.5));
        metaDataTitleLine.endXProperty().bind(mainWindow.widthProperty().subtract(99.5));
        applicationSettingTitleLine.endXProperty().bind(mainWindow.widthProperty().subtract(99.5));

        confirmChanges.layoutYProperty().bind(mainWindow.heightProperty().subtract( 64 + confirmChanges.getHeight()));
        cancelBackButton.layoutXProperty().bind(mainWindow.widthProperty().subtract(44 + cancelBackButton.getWidth()));
        cancelBackButton.layoutYProperty().bind(mainWindow.heightProperty().subtract(64 + cancelBackButton.getHeight()));

        // Bindings: Search Results
        resultsTable.prefWidthProperty().bind(mainWindow.widthProperty().subtract(119.5));
        resultsTable.prefHeightProperty().bind(mainWindow.heightProperty().subtract(180));
        downloadButton.layoutYProperty().bind(mainWindow.heightProperty().subtract(120));
        cancelButton.layoutXProperty().bind(mainWindow.widthProperty().subtract(69.5 + cancelButton.getWidth()));
        cancelButton.layoutYProperty().bind(mainWindow.heightProperty().subtract(120));
        searchesProgressText.layoutYProperty().bind(mainWindow.heightProperty().subtract(115));
        timeRemainingLabel.setTranslateX( (loading.getWidth() / 2) + (timeRemainingLabel.getWidth()/2) - 19.5 );
        timeRemainingLabel.layoutYProperty().bind(mainWindow.heightProperty().subtract(115));
        downloadSpeedLabel.layoutXProperty().bind(loading.widthProperty().subtract(19.5));
        downloadSpeedLabel.layoutYProperty().bind(mainWindow.heightProperty().subtract(115));
        loading.prefWidthProperty().bind(resultsTable.widthProperty());
        loading.layoutYProperty().bind(mainWindow.heightProperty().subtract(95));
        downloadsViewLink.layoutYProperty().bind(loading.layoutYProperty().add(30));
        backgroundDownload.layoutYProperty().bind(loading.layoutYProperty().add(30));
        backgroundDownload.layoutXProperty().bind(loading.layoutXProperty().add(loading.widthProperty().subtract(57)));
        queueAdditionProgress.layoutXProperty().bind(downloadButton.layoutXProperty().add(downloadButton.widthProperty()).add(49.5));
        queueAdditionProgress.layoutYProperty().bind(mainWindow.heightProperty().subtract(117));

        // Bindings: Downloads
        downloadsInfoScrollPane.prefWidthProperty().bind(mainWindow.widthProperty().divide(4));
        downloadsInfoScrollPane.prefHeightProperty().bind(mainWindow.heightProperty().divide(6));

        downloadGraph.layoutXProperty().bind(downloadsInfoScrollPane.layoutXProperty().add(downloadsInfoScrollPane.widthProperty()).add(mainWindow.widthProperty().divide(10)));
        downloadGraph.prefWidthProperty().bind(mainWindow.widthProperty().subtract(downloadsInfoScrollPane.widthProperty().add(mainWindow.widthProperty().divide(10).add(20+19.5))));
        downloadGraph.prefHeightProperty().bind(mainWindow.heightProperty().divide(6));

        downloadEventsView.layoutYProperty().bind(downloadsInfoScrollPane.layoutXProperty().add(downloadsInfoScrollPane.heightProperty()).add(mainWindow.heightProperty().divide(10)));
        downloadEventsView.prefWidthProperty().bind(mainWindow.widthProperty().subtract(40+19.5));
        downloadEventsView.prefHeightProperty().bind(mainWindow.heightProperty().subtract(160).subtract(downloadsInfoScrollPane.layoutXProperty().add(downloadsInfoScrollPane.heightProperty()).add(mainWindow.heightProperty().divide(10))));

        downloadBack.layoutXProperty().bind(downloadEventsView.layoutXProperty().add(downloadEventsView.widthProperty()).subtract(downloadBack.widthProperty().subtract(9)));
        downloadBack.layoutYProperty().bind(mainWindow.heightProperty().subtract(120));
    }

    public void handle(KeyEvent event) {
        try {
            controller.userKeyInteraction(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void handleSearch() {

        // Check if a search is already in progress, fine to search if first search or search isn't in progress
        try {
            if (searchQuery.inProgress()) {
                Debug.trace(null, "Failed to search, search already in progress");
                searchErrorMessage.setText("Already searching, please wait...");
                searchErrorMessage.setVisible(true);
                return;
            }
        } catch (NullPointerException ignored) {}

        // Could look at changing how vars are passed, less global ideally
        searchQuery = new allMusicQuery();
        threadManagement.add(new ArrayList<>(Arrays.asList("allMusicQuery", searchQuery)));

        try { autocompleteGenerator.kill(); } catch (NullPointerException ignored) {}

        // Make the loading bar spin for a little bit till the thread reports the table as populated then transition to TableView smoothly
        loadingIcon.setVisible(true);
        timerRotate = new Timer();
        timerRotate.schedule(new TimerTask() {
            @Override
            public void run() {
                loadingIcon.setRotate(loadingIcon.getRotate() + 18);
            }
        }, 0, 100);

    }

    public synchronized void setMetaDataVisibility() {

        boolean disableComboBoxes;
        Color colour;

        if (songDownloadFormatResult.getSelectionModel().selectedIndexProperty().getValue() == 0) {
            disableComboBoxes = false;
            colour = Color.BLACK;
        } else {
            disableComboBoxes = true;
            colour = Color.LIGHTGRAY;
        }

        albumArtSettingResult.setDisable(disableComboBoxes);
        albumTitleSettingResult.setDisable(disableComboBoxes);
        songTitleSettingResult.setDisable(disableComboBoxes);
        artistSettingResult.setDisable(disableComboBoxes);
        yearSettingResult.setDisable(disableComboBoxes);
        trackNumberSettingResult.setDisable(disableComboBoxes);

        albumArtSetting.setTextFill(colour);
        albumTitleSetting.setTextFill(colour);
        songTitleSetting.setTextFill(colour);
        artistSetting.setTextFill(colour);
        yearSetting.setTextFill(colour);
        trackNumberSetting.setTextFill(colour);
    }

    public synchronized void cancel(boolean cancelDownload) {

        // Stops the search thread from running
        if (cancelDownload) {
            try {

                searchQuery.kill();
                //handleDownload.kill();

            } catch (NullPointerException ignored) {}
        }

        // Clear Table and Hide, Revert to search state
        resultsTable.getItems().clear();
        resultsTable.setVisible(false);
        searchResultsTitle.setVisible(false);
        searchRequest.setVisible(true);
        searchRequest.clear();
        title.setVisible(true);
        downloadButton.setDisable(true);
        downloadButton.setVisible(false);
        cancelButton.setVisible(false);
        downloadsViewLink.setVisible(false);
        backgroundDownload.setVisible(false);

        loading.setVisible(false);
        loading.setProgress(0);

        resultsTable.prefHeightProperty().unbind();
        resultsTable.prefHeightProperty().bind(mainWindow.heightProperty().subtract(180));
        cancelButton.layoutYProperty().unbind();
        cancelButton.layoutYProperty().bind(mainWindow.heightProperty().subtract(120));
        downloadButton.layoutYProperty().unbind();
        downloadButton.layoutYProperty().bind(mainWindow.heightProperty().subtract(120));

        searchesProgressText.setVisible(false);
        searchesProgressText.setText("Search Songs: 0%");
        searchesProgressText.setTextFill(Color.BLACK);

        downloadSpeedLabel.setVisible(false);
        timeRemainingLabel.setVisible(false);

        footerMarker.setVisible(true);
        settingsLink.setVisible(true);
        downloadsLink.setVisible(true);

        downloadButton.setDisable(false);

    }

    public synchronized void selectNewFolder() {

        try {

            JFileChooser newFolder = new JFileChooser();
            newFolder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            newFolder.showSaveDialog(null);

            OutputDirectorySettingNew = newFolder.getSelectedFile().getPath();

        } catch (NullPointerException ignored) {} // User cancels the operation
    }

    public synchronized void switchSettingVisibility(boolean settingVisibility) {
        // Set search to invisible
        searchRequest.setVisible(!settingVisibility);
        title.setVisible(!settingVisibility);

        // Setting settings link footer to invisible
        footerMarker.setVisible(!settingVisibility);
        settingsLink.setVisible(!settingVisibility);
        downloadsLink.setVisible(!settingVisibility);

        // Set setting elements visible
        settingsContainer.setVisible(settingVisibility);

        // Buttons
        confirmChanges.setVisible(settingVisibility);
        cancelBackButton.setVisible(settingVisibility);

    }

    public synchronized void settingsMode() {

        switchSettingVisibility(true);

        // Additional selection
        songDownloadFormatResult.getSelectionModel().select(musicFormatSetting);
        saveAlbumArtResult.getSelectionModel().select(saveAlbumArtSetting);

        albumArtSettingResult.setSelected(applyAlbumArt);
        albumTitleSettingResult.setSelected(applyAlbumTitle);
        songTitleSettingResult.setSelected(applySongTitle);
        artistSettingResult.setSelected(applyArtist);
        yearSettingResult.setSelected(applyYear);
        trackNumberSettingResult.setSelected(applyTrack);
        darkModeSettingResult.getSelectionModel().select(darkTheme ? 1 : 0);
        dataSaverSettingResult.getSelectionModel().select(dataSaver ? 0 : 1);

        setMetaDataVisibility();
        evaluateSettingsChanges();

        // Scheduling getting latest version, if data saver disabled
        if (!dataSaver) {
            threadManagement.add(new ArrayList<>(Arrays.asList("getLatestVersion", new getLatestVersion())));
        }

        directoryListener = new outputDirectoryListener();
        threadManagement.add(new ArrayList<>(Arrays.asList("youtubeDlVerification", new youtubeDlVerification())));
        threadManagement.add(new ArrayList<>(Arrays.asList("ffmpegVerificationThread", new ffmpegVerificationThread())));
        threadManagement.add(new ArrayList<>(Arrays.asList("outputDirectoryListener", directoryListener)));
    }

    public synchronized void searchMode() {

        directoryListener.kill();
        switchSettingVisibility(false);
        switchTheme(darkTheme);
    }

    public synchronized void submit() {

        // Saving to file
        try {
            SettingsFunc.saveSettings(
                    OutputDirectorySettingNew,
                    songDownloadFormatResult
                            .getSelectionModel()
                            .selectedIndexProperty()
                            .getValue(),
                    saveAlbumArtResult
                            .getSelectionModel()
                            .selectedIndexProperty()
                            .getValue(),
                    albumArtSettingResult.isSelected() ? 1 : 0,
                    albumTitleSettingResult.isSelected() ? 1 : 0,
                    songTitleSettingResult.isSelected() ? 1 : 0,
                    artistSettingResult.isSelected() ? 1 : 0,
                    yearSettingResult.isSelected() ? 1 : 0,
                    trackNumberSettingResult.isSelected() ? 1 : 0,
                    darkModeSettingResult
                            .getSelectionModel()
                            .selectedIndexProperty()
                            .getValue(),
                    Math.abs(
                            dataSaverSettingResult
                                    .getSelectionModel()
                                    .selectedIndexProperty()
                                    .getValue()
                                    - 1
                    )
            );

            // Setting the vars
            outputDirectorySetting = OutputDirectorySettingNew;
            musicFormatSetting = songDownloadFormatResult
                    .getSelectionModel()
                    .selectedIndexProperty()
                    .getValue();
            saveAlbumArtSetting = saveAlbumArtResult
                    .getSelectionModel()
                    .selectedIndexProperty()
                    .getValue();
            applyAlbumArt = albumArtSettingResult.isSelected();
            applyAlbumTitle = albumTitleSettingResult.isSelected();
            applySongTitle = songTitleSettingResult.isSelected();
            applyArtist = artistSettingResult.isSelected();
            applyYear = yearSettingResult.isSelected();
            applyTrack = trackNumberSettingResult.isSelected();
            darkTheme = darkModeSettingResult
                    .getSelectionModel()
                    .selectedIndexProperty()
                    .getValue() != 0;
            dataSaver = dataSaverSettingResult
                    .getSelectionModel()
                    .selectedIndexProperty()
                    .getValue() == 0;

            evaluateSettingsChanges();

        } catch (JSONException e) {
            Debug.error(null, "Error saving settings", e.getStackTrace());
        }
    }

    public synchronized void evaluateSettingsChanges() {

        if (
                musicFormatSetting != songDownloadFormatResult.getSelectionModel().getSelectedIndex() ||
                        saveAlbumArtSetting != saveAlbumArtResult.getSelectionModel().getSelectedIndex() ||
                        ((applyAlbumArt && !albumArtSettingResult.isSelected()) || (!applyAlbumArt && albumArtSettingResult.isSelected())) ||
                        ((applyAlbumTitle && !albumTitleSettingResult.isSelected()) || (!applyAlbumTitle && albumTitleSettingResult.isSelected())) ||
                        ((applyArtist && !artistSettingResult.isSelected()) || (!applyArtist && artistSettingResult.isSelected())) ||
                        ((applySongTitle && !songTitleSettingResult.isSelected()) || (!applySongTitle && songTitleSettingResult.isSelected())) ||
                        ((applyYear && !yearSettingResult.isSelected()) || (!applyYear && yearSettingResult.isSelected())) ||
                        ((applyTrack && !trackNumberSettingResult.isSelected()) || (!applyTrack && trackNumberSettingResult.isSelected())) ||
                        ((darkTheme && darkModeSettingResult.getSelectionModel().getSelectedIndex() != 1) || (!darkTheme && darkModeSettingResult.getSelectionModel().getSelectedIndex() == 1)) ||
                        ((dataSaver && dataSaverSettingResult.getSelectionModel().getSelectedIndex() != 0) || (!dataSaver && dataSaverSettingResult.getSelectionModel().getSelectedIndex() == 0))
        ) {
            confirmChanges.setDisable(false);
            cancelBackButton.setText("Cancel");
        } else {
            // Default button states
            confirmChanges.setDisable(true);
            cancelBackButton.setText("Back");
        }


    }

    public synchronized void switchTheme(boolean nightMode) {

        try {
            if (nightMode) {
                scene.getStylesheets().add("file:///" + new File("resources/css/night_theme.css").getAbsolutePath().replace("\\", "/"));
                scene.getStylesheets().remove("file:///" + new File("resources/css/normal_theme.css").getAbsolutePath().replace("\\", "/"));
            } else {
                scene.getStylesheets().add("file:///" + new File("resources/css/normal_theme.css").getAbsolutePath().replace("\\", "/"));
                scene.getStylesheets().remove("file:///" + new File("resources/css/night_theme.css").getAbsolutePath().replace("\\", "/"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public synchronized void generateQueryAutocomplete(String searchQuery) {

        if (!dataSaver) {

            if (searchQuery.length() > 3) {

                try { autocompleteGenerator.kill(); } catch (Exception ignored) {}
                autocompleteGenerator = new generateAutocomplete();
                threadManagement.add(new ArrayList<>(Arrays.asList("generateAutocomplete", autocompleteGenerator)));
                autocompleteGenerator.setAutocompleteQuery(searchQuery);

            } else {

                autocompleteResultsTable.setVisible(false);
                autocompleteResultsTable.getItems().clear();

            }

        }

    }

    public synchronized void downloadMode() {

        // Hiding Search
        title.setVisible(false);
        searchRequest.setVisible(false);
        footerMarker.setVisible(false);
        settingsLink.setVisible(false);
        downloadsLink.setVisible(false);

        // Hiding Downloads
        searchResultsTitle.setVisible(false);
        resultsTable.setVisible(false);
        downloadButton.setVisible(false);
        cancelButton.setVisible(false);
        loading.setVisible(false);
        searchesProgressText.setVisible(false);
        timeRemainingLabel.setVisible(false);
        downloadSpeedLabel.setVisible(false);
        downloadsViewLink.setVisible(false);
        backgroundDownload.setVisible(false);

        // Showing Downloads
        downloadsTitle.setVisible(true);
        downloadsInfoScrollPane.setVisible(true);
        downloadGraph.setVisible(true);
        downloadEventsView.setVisible(true);
        downloadBack.setVisible(true);

        // Drawing the listview history
        new generateDownloadHistory();

    }

    public BorderPane generateResult(ImageView art, String title, String artistName, String statusMessage, String id, String dir, Double loaderProgress, boolean crossToDelete, boolean useLoader, boolean clickable, boolean greyScale) {

        BorderPane result = new BorderPane();

        if (greyScale) {
            // Greyscale the album art
            ColorAdjust desaturate = new ColorAdjust();
            desaturate.setSaturation(-1);
            art.setEffect(desaturate);
        }

        Label songTitle = new Label(title);
        Label artist = new Label(artistName);
        artist.setId("downloadHistoryArtistTitle");
        songTitle.setId("downloadHistoryResultTitle");

        VBox songInfoContainer = new VBox(songTitle, artist);
        songInfoContainer.setPadding(new Insets(0, 0, 0, 5));

        Label status = new Label(statusMessage);
        status.setId("downloadStatus");
        HBox statusInfo = new HBox(status);

        VBox statusAndLoading = new VBox(statusInfo);

        if (useLoader) {

            ProgressBar downloadProgress = new ProgressBar(loaderProgress);
            Label percentMessage = new Label(Math.round(loaderProgress * 100) + "%");
            percentMessage.setTextFill(darkTheme ? Color.WHITE : Color.BLACK);

            HBox downloadInfo = new HBox(downloadProgress, percentMessage);
            downloadInfo.setSpacing(5);

            downloadProgress.prefWidthProperty().bind( (result.widthProperty().subtract(85 + 60).subtract(percentMessage.widthProperty())).divide(2) );

            statusAndLoading.getChildren().setAll(
                    downloadInfo,
                    statusInfo
            );
        }

        statusAndLoading.setPadding(new Insets(0, 0, 0, 5));

        BorderPane textInfoContainer = new BorderPane();
        textInfoContainer.setTop(songInfoContainer);
        textInfoContainer.setBottom(statusAndLoading);

        Line crossLine0 = new Line(20, 0, 0, 20);
        Line crossLine1 = new Line(20, 20, 0, 0);
        crossLine0.setStrokeWidth(2);
        crossLine0.setStroke(Color.GRAY);
        crossLine1.setStrokeWidth(2);
        crossLine1.setStroke(Color.GRAY);

        Group cross = new Group(crossLine0, crossLine1);
        HBox crossBox = new HBox(cross);
        crossBox.setAlignment(Pos.CENTER_RIGHT);
        Tooltip.install(crossBox, new Tooltip("Delete record."));
        crossBox.setOnMouseEntered(e -> { crossLine0.setStroke(darkTheme ? Color.WHITE : Color.BLACK); crossLine1.setStroke(darkTheme ? Color.WHITE : Color.BLACK); });
        crossBox.setOnMouseExited(e -> {crossLine0.setStroke(Color.GRAY);crossLine1.setStroke(Color.GRAY); });
        crossBox.setOnMouseClicked(
                crossToDelete ? (
                        e -> deleteDownloadHistory(id)
                ) : (
                        e -> cancelDownload(id)
                )
        );
        HBox historyItem = new HBox(art, textInfoContainer);

        result.setLeft(historyItem);
        result.setRight(crossBox);
        result.setPadding(new Insets(0, 10, 0, 0));
        result.setId(id);

        result.setOnMouseEntered(e -> result.setStyle("-fx-background-color: WHITE;"));
        result.setOnMouseExited(e -> result.setStyle("-fx-background-color: transparent;"));

        if (clickable) {

            Tooltip.install(result, new Tooltip("View in explorer"));
            result.setCursor(Cursor.HAND);

            result.setOnMouseClicked(e -> {
                int targetId = -1;
                for (Object downloadEvent: downloadEventsView.getItems()) {

                    if ( ((BorderPane) downloadEvent).getId().equals(result.getId()) ) {
                        targetId = downloadEventsView.getItems().indexOf(downloadEvent);
                    }

                }
                if (lastClicked.get(targetId) + 400 > Instant.now().toEpochMilli()) {
                    try {
                        Desktop.getDesktop().open(new File(dir));
                    } catch (IOException ignored) {}
                } else {
                    lastClicked.set(targetId, Instant.now().toEpochMilli());
                }

            });
        }

        return result;

    }

    public void deleteDownloadHistory(String id) {

        try {
            // Remove from table
            for (Object resultObj : downloadEventsView.getItems()) {
                BorderPane result = (BorderPane) resultObj;
                if (result.getId().equals(id)) {
                    downloadEventsView.getItems().remove(resultObj);
                }
            }

            // Delete in file
            JSONArray histories = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());
            JSONArray newHistories = new JSONArray();

            for (int i = 0; i < histories.length(); i++) {

                if (!histories.getJSONObject(i).get("id").toString().equals(id)) {
                    newHistories.put(histories.getJSONObject(i));
                }
            }

            FileWriter updateRecords = new FileWriter("resources\\json\\downloads.json");
            updateRecords.write(newHistories.toString());
            updateRecords.close();


        } catch (ConcurrentModificationException ignored) {
            deleteDownloadHistory(id);
        } catch (IOException | JSONException e) {
            Debug.error(null, "Failed to delete record with id: " + id, e.getStackTrace());
        }

    }

    public void cancelDownload(String id) {

        // Removing item from the table
        try {
            for (Object resultObj : downloadEventsView.getItems()) {
                BorderPane result = (BorderPane) resultObj;
                if (result.getId().equals(id)) {
                    downloadEventsView.getItems().remove(resultObj);
                }
            }
        } catch (ConcurrentModificationException ignored) {}

        // Removing item from the downloads queue
        try {
            for (int i = 0; i < downloadQueue.length(); i++) {
                for (int j = 0; j < downloadQueue.getJSONObject(i).getJSONArray("songs").length(); j++) {

                    if (downloadQueue.getJSONObject(i).getJSONArray("songs").getJSONObject(j).get("id").equals(id)) {
                        // Located the element we're interested in

                        String directory = downloadQueue.getJSONObject(i).getJSONObject("meta").get("directory").toString();
                        String cached = System.getProperty("user.dir") + "\\" + "resources\\cache\\" + "no.jpg";

                        directory = directory.replaceAll("\\\\", "/");
                        cached = cached.replaceAll("\\\\", "/");

                        // Format the structure to be saved in downloads
                        JSONObject downloadRecord = new JSONObject(
                                String.format(
                                        "{\"downloadFolder\": \"%s\", \"art\": \"%s\", \"artist\": \"%s\", \"cached\": \"%s\", \"playtime\": %s, \"id\": %s, \"title\": \"%s\", \"timestamp\": %s, \"cancelled\": 1}",
                                        directory,
                                        downloadQueue.getJSONObject(i).getJSONObject("meta").get("art").toString(),
                                        downloadQueue.getJSONObject(i).getJSONObject("meta").get("artist").toString(),
                                        cached,
                                        downloadQueue.getJSONObject(i).getJSONArray("songs").getJSONObject(j).get("playtime").toString(),
                                        downloadQueue.getJSONObject(i).getJSONArray("songs").getJSONObject(j).get("id").toString(),
                                        downloadQueue.getJSONObject(i).getJSONArray("songs").getJSONObject(j).get("title"),
                                        Instant.now().toEpochMilli()
                                )
                        );

                        System.out.println(downloadRecord);

                        // Tell the download handler to skip this if the ID matches what it's downloading by checking the data
                        if (i == 0 && j == 0) {
                            downloader.skip();
                        }

                        // Add record to the downloads history
                        try {
                            Utils.updateDownloads(downloadRecord);
                        } catch (IOException | JSONException e) {
                            Debug.error(null, "Error adding to download history for cancelled.", e.getStackTrace());
                        }

                        // Add the cancelled song as a new element to the table
                        String finalCached = cached;
                        int finalI = i;
                        int finalJ = j;
                        Platform.runLater(() ->
                                {
                                    try {
                                        downloadEventsView.getItems().add(
                                                generateResult(
                                                        Files.exists(
                                                                Paths.get(finalCached)
                                                        ) ? new ImageView(
                                                                new Image(
                                                                        finalCached,
                                                                        85,
                                                                        85,
                                                                        false,
                                                                        true
                                                                )
                                                        ) : new ImageView(
                                                                new Image(
                                                                        downloadQueue.getJSONObject(finalI).getJSONObject("meta").get("art").toString(),
                                                                        85,
                                                                        85,
                                                                        false,
                                                                        true
                                                                )
                                                        ),
                                                        downloadQueue.getJSONObject(finalI).getJSONArray("songs").getJSONObject(finalJ).get("title").toString(),
                                                        downloadQueue.getJSONObject(finalI).getJSONObject("meta").get("artist").toString(),
                                                        "Cancelled",
                                                        downloadQueue.getJSONObject(finalI).getJSONArray("songs").getJSONObject(finalJ).get("id").toString(),
                                                        downloadQueue.getJSONObject(finalI).getJSONObject("meta").get("directory").toString(),
                                                        null,
                                                        true,
                                                        false,
                                                        false,
                                                        true
                                                )
                                        );
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                        );

                    }

                }
            }
        } catch (JSONException e) {
            Debug.error(null, "Failed to remove element from downloads queue.", e.getStackTrace());
        }


        // Creating the object to be saved in downloads history as cancelled

        // Removing the item from the downloads queue

        // Adding the item to the downloads history

    }

    // Search: Added
    class generateAutocomplete implements Runnable {

        Thread t;
        String autocompleteQuery;
        private volatile boolean killRequest = false;

        generateAutocomplete (){
            t = new Thread(this, "autocomplete");
            t.start();
        }

        public void setAutocompleteQuery(String request){
            autocompleteQuery = request;
        }

        public void kill() {
            killRequest = true;
        }

        public void run() {

            // Wait for either the thread kill signal or the web request to be completed
            autoCompleteWeb requestThread = new autoCompleteWeb();
            threadManagement.add(new ArrayList<>(Arrays.asList("autoCompleteWeb", requestThread)));
            requestThread.setSearchQuery(autocompleteQuery);

            while (true) {

                // Calling this too rapidly without delay eats up performance and won't work
                try {
                    Thread.sleep(100); // TODO: Switch with a timer task
                } catch (InterruptedException ignored) {}

                // Not being found, ideally need to kill the web request
                if (killRequest) {
                    break;
                }

                if (!requestThread.getAutocompleteResults().isEmpty()) {

                    autocompleteResultsTable.setVisible(true);
                    autocompleteResultsTable.getItems().clear();

                    for (ArrayList<String> queryResult : requestThread.getAutocompleteResults()) {
                        autocompleteResultsTable.getItems().add(
                                new Utils.autocompleteResultsSet(
                                        queryResult.get(0).equals("Album") ? new ImageView(new Image(new File("resources/album_default.png").toURI().toString())) : new ImageView(new Image(new File("resources/song_default.png").toURI().toString())),
                                        queryResult.get(1)
                                )
                        );
                    }

                    autocompleteResultsTable.setPrefHeight(autocompleteResultsTable.getItems().size() * 31);
                    break;

                }

            }

        }

    }

    // Search: Added
    class allMusicQuery implements Runnable {

        Thread t;
        private volatile boolean working = true;
        private volatile boolean kill = false;
        private volatile String status = "Initializing";
        private final long startTime = Instant.now().toEpochMilli();
        private volatile long endTime = Long.MIN_VALUE;

        public boolean inProgress() {
            return working;
        }

        public void kill() {
            kill = true;
        }

        allMusicQuery (){
            t = new Thread(this, "query");
            t.start();
        }

        public ArrayList<String> getInfo() {
            return new ArrayList<>(
                    Arrays.asList(
                            t.getName(),
                            Long.toString(t.getId()),
                            Long.toString(startTime),
                            Long.toString(endTime),
                            status,
                            Boolean.toString(!working)
                    )
            );
        }

        public void run() {

            try {

                status = "Sending web request to search...";
                Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + searchRequest.getText()).get();
                searchResults = Utils.allmusicQuery(doc);

            } catch (IOException e) {

                Platform.runLater(() -> {
                    timerRotate.cancel();
                    loadingIcon.setVisible(false);
                    searchErrorMessage.setText("Invalid Search");
                    searchErrorMessage.setVisible(true);
                });
                Debug.error(t, "Error connecting to allmusic", e.getStackTrace());
                return;

            }

            if (searchResults.size() > 0) {

                try {

                    status = "Processing search results";

                    // Signal is always sent immediately when running, only want to kill other threads that are in loop, not this one
                    // Needs to check this won't result in future threads being killed
                    resultsData = new ArrayList<>();
                    IntStream.range(0, searchResults.size()).forEach(i -> resultsData.add(null));

                    searchResultFullData = new Utils.resultsSet[searchResults.size()];

                    for (ArrayList<String> searchResult : searchResults) {

                        // Sending a new query requires quitting the old
                        if (kill) {
                            resultsTable.getItems().clear();
                            break;
                        }

                        addToTable tableAdder = new addToTable();
                        threadManagement.add(new ArrayList<>(Arrays.asList("addToTable", tableAdder)));
                        tableAdder.setSearchResult(searchResult);
                    }

                    while (true) {

                        int completedThreads = 0;
                        int totalThreads = searchResultFullData.length;

                        for (Utils.resultsSet resultPending: searchResultFullData) {

                            // Thread has completed when it is not null
                            if (resultPending != null) {
                                completedThreads++;
                            }

                        }

                        if (completedThreads == totalThreads) {
                            break;
                        }

                        status = "Awaitng detail threads to complete, " + completedThreads + " out of " + totalThreads + " so far.";
                    }

                    for (Utils.resultsSet result: searchResultFullData)
                        resultsTable.getItems().add(result);

                } catch (NullPointerException e) {
                    Debug.error(t, "Invalid search error", e.getStackTrace());
                }

            } else {

                Debug.trace(t, "No search results found for query: " + searchRequest.getText());

                Platform.runLater(() -> {
                    timerRotate.cancel();
                    loadingIcon.setVisible(false);
                    searchErrorMessage.setText("No Search Results Found");
                    searchErrorMessage.setVisible(true);
                });

                return;

            }

            Platform.runLater(() -> {
                timerRotate.cancel();
                loadingIcon.setVisible(false);
                downloadButton.setDisable(true);
                resultsTable.setVisible(true);
                searchResultsTitle.setVisible(true);
                downloadButton.setVisible(true);
                cancelButton.setVisible(true);

                autocompleteResultsTable.setVisible(false);
                autocompleteResultsTable.getItems().clear();
                searchRequest.setVisible(false);
                title.setVisible(false);
                footerMarker.setVisible(false);
                settingsLink.setVisible(false);
                downloadsLink.setVisible(false);
                searchErrorMessage.setVisible(false);
            });

            endTime = Instant.now().toEpochMilli();
            working = false;

        }

    }

    // Search: Added
    static class autoCompleteWeb implements Runnable {

        Thread t;
        String searchQuery;
        ArrayList<ArrayList<String>> autocompleteResults = new ArrayList<>();
        private volatile String status = "Initializing";
        private final long startTime = Instant.now().toEpochMilli();
        private volatile long endTime = Long.MIN_VALUE;
        private volatile boolean completed = false;

        autoCompleteWeb (){
            t = new Thread(this, "autocomplete-web");
            t.start();
        }

        public void setSearchQuery(String setRequest) {
            searchQuery = setRequest;
        }

        public ArrayList<ArrayList<String>> getAutocompleteResults() {
            return autocompleteResults;
        }

        public ArrayList<String> getInfo() {
            return new ArrayList<>(
                    Arrays.asList(
                            t.getName(),
                            Long.toString(t.getId()),
                            Long.toString(startTime),
                            Long.toString(endTime),
                            status,
                            Boolean.toString(completed)
                    )
            );
        }

        public void run() {

            try {

                status = "Waiting on web request...";

                Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + searchQuery).get();

                status = "Web data finished, processing data...";

                for (ArrayList<String> searchResult: Utils.allmusicQuery(doc)) {

                    ArrayList<String> resultsData = new ArrayList<>();
                    resultsData.add(searchResult.get(4));
                    resultsData.add(searchResult.get(0));

                    // Prevents duplicated results
                    if (!autocompleteResults.contains(resultsData)) {

                        autocompleteResults.add(resultsData); // Song or Album and Name

                    }
                }

            } catch (IOException e) {
                Debug.error(t, "Error sending web request: https://www.allmusic.com/search/all/" + searchQuery, e.getStackTrace());
            }

            endTime = Instant.now().toEpochMilli();
            completed = true;

        }

    }

    // Search: Added
    class addToTable implements Runnable {

        Thread t;
        ArrayList<String> searchResult;

        addToTable() {
            t = new Thread(this, "table-adder");
            t.start();
        }

        public void setSearchResult(ArrayList<String> searchResultSet) {
            searchResult = searchResultSet;
        }

        public void run() {

            try {

                // Reference for web requests
                Document songDataPage;

                // Determining the album art to use, and the year
                ImageView selectedImage = null;
                String year = searchResult.get(2);
                String genre = searchResult.get(3);

                if (searchResult.get(4).equals("Album")) {

                    if (dataSaver) {
                        selectedImage = new ImageView(new Image(new File("resources/album_default.png").toURI().toString()));
                    } else {
                        // This is an album request, year must be known or won't be found and hence doesn't require a check
                        if (searchResult.get(5).equals("")) {
                            selectedImage = new ImageView(new Image(new File("resources/album_default.png").toURI().toString()));
                        } else {
                            selectedImage = new ImageView(new Image(searchResult.get(5)));
                        }
                    }
                } else {

                    if (dataSaver) {

                        selectedImage = new ImageView(new Image(new File("resources/song_default.png").toURI().toString()));

                    } else {
                        try {
                            songDataPage = Jsoup.connect(searchResult.get(6)).get();

                            if (songDataPage.selectFirst("td.cover").selectFirst("img").attr("src") != null && songDataPage.selectFirst("td.cover").selectFirst("img").attr("src").startsWith("https")) {
                                selectedImage = new ImageView(new Image(songDataPage.selectFirst("td.cover").selectFirst("img").attr("src")));
                            } else {
                                selectedImage = new ImageView(new Image(new File("resources/song_default.png").toURI().toString()));
                            }


                            if (songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear").length() == 4 && year.length() == 0) {
                                year = songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear");
                            }

                            try {
                                genre = songDataPage.selectFirst("div.song_genres").selectFirst("div.middle").selectFirst("a").text().split("\\(")[0].substring(0, songDataPage.selectFirst("div.song_genres").selectFirst("div.middle").selectFirst("a").text().split("\\(")[0].length() - 1);
                            } catch (NullPointerException ignored) {}

                        } catch (IllegalArgumentException e) {
                            Debug.error(t, "Invalid search request", e.getStackTrace());
                        }
                    }

                }

                Utils.resultsSet results = new Utils.resultsSet(
                        selectedImage,
                        searchResult.get(0),
                        searchResult.get(1),
                        year,
                        genre,
                        searchResult.get(4)
                );

                searchResultFullData[searchResults.indexOf(searchResult)] = results;
                resultsData.set(searchResults.indexOf(searchResult), results);


            } catch (IOException e) {
                Debug.error(t, "Error adding to table", e.getStackTrace());
            }

        }

    }

    // Settings: Added
    class getLatestVersion implements Runnable {

        Thread t;

        getLatestVersion (){
            t = new Thread(this, "get-latest-version");
            t.start();
        }

        public void run() {

            double originalWidth = latestVersionResult.getWidth();
            String latestVersion = SettingsFunc.getLatestVersion();

            Platform.runLater(() -> latestVersionResult.setText(latestVersion == null ? "Unknown" : latestVersion));
            while (latestVersionResult.getWidth() == originalWidth) { try {Thread.sleep(10);} catch (InterruptedException ignored) {} }
            Platform.runLater(() -> latestVersionResultContainer.setPadding(new Insets(70, 0, 0, -latestVersionResult.getWidth())));
        }

    }

    // Settings: Not Added
    class selectFolder implements  Runnable {

        Thread t;

        selectFolder (){
            t = new Thread(this, "folder-selection");
            t.start();
        }

        public void run() {

            selectNewFolder();

            try {
                // 0 is a cancelled operation, hence don't do anything throw an error to quickly exit
                if (OutputDirectorySettingNew.hashCode() == 0) {
                    OutputDirectorySettingNew = outputDirectorySetting;
                    throw new Exception();
                }

                double originalWidth = outputDirectoryResult.getWidth();
                Platform.runLater(() -> outputDirectoryResult.setText(OutputDirectorySettingNew));

                // Wait for the text to to be set, then update the rest
                while (originalWidth == outputDirectoryResult.getWidth());
                Platform.runLater(() -> outputDirectoryResultContainer.setPadding(new Insets(180, 0, 0, -outputDirectoryResult.getWidth())));

                // Save the settings
                outputDirectorySetting = OutputDirectorySettingNew;
                submit();

            } catch (Exception ignored) {}

        }
    }

    // Model: Not Added
    class smartQuitDownload implements  Runnable {

        Thread t;
        private volatile boolean windowIsShowing;
        private volatile boolean completed = false;
        private final long startTime = Instant.now().toEpochMilli();
        private volatile long endTime = Long.MIN_VALUE;
        private volatile String status = "Initializing...";

        smartQuitDownload () {
            t = new Thread(this, "smart-quit");
            t.start();
        }

        public ArrayList<String> getInfo() {
            return new ArrayList<>(
                    Arrays.asList(
                            t.getName(),
                            Long.toString(t.getId()),
                            Long.toString(startTime),
                            Long.toString(endTime),
                            status,
                            Boolean.toString(completed)
                    )
            );
        }

        public void run() {

            Debug.trace(t, "Started");
            status = "Current loading, window not yet loaded.";
            // Initial where window doesn't exist
            while (true) {

                windowIsShowing = mainWindow.isShowing();

                try {
                    // Temporarily false as things load in
                    if (windowIsShowing)
                        break;

                } catch (NullPointerException ignored) {}
            }

            Debug.trace(t, "Detected window is now open.");
            status = "Detected window as open, waiting for it to be closed.";

            // Keep in background until the window is closed, does need to be a variable
            while (windowIsShowing){
                windowIsShowing = mainWindow.isShowing();
            }

            Debug.trace(t,"Window closed detected, killing threads.");
            status = "Killing threads.";

            // Quit running threads, download is important query is mostly for performance
            /*
            try {
                handleDownload.kill();
                while (!handleDownload.isDead());
            } catch (NullPointerException ignored) {}
             */

            Debug.trace(t, "All threads reporting dead, program should safely exit.");
            endTime = Instant.now().toEpochMilli();
            completed = true;
            System.exit(0);
        }
    }

    // Settings: Added
    class youtubeDlVerification implements Runnable {

        Thread t;

        youtubeDlVerification () {
            t = new Thread(this, "youtube-dl-verification");
            t.start();
        }

        public void run() {

            boolean youtubeDlStatus = SettingsFunc.checkYouTubeDl();
            double originalWidth = youtubeDlVerificationResult.getWidth();

            if (youtubeDlStatus) {

                Platform.runLater(() -> youtubeDlVerificationResult.setText("Fully Operational"));

            } else {

                Platform.runLater(() -> youtubeDlVerificationResult.setText("YouTubeDl: Not Configured"));

            }

            while (youtubeDlVerificationResult.getWidth() == originalWidth) {}
            Platform.runLater(() -> youtubeDlVerificationResultContainer.setPadding(new Insets(90, 0, 0, -youtubeDlVerificationResult.getWidth())));

        }
    }

    // Settings: Added
    class ffmpegVerificationThread implements Runnable {

        Thread t;

        ffmpegVerificationThread () {
            t = new Thread(this, "ffmpeg-verification");
            t.start();
        }

        public void run() {

            Debug.trace(t, "Initialized");

            boolean ffmpegStatus = SettingsFunc.checkFFMPEG();
            double originalWidth = ffmpegVerificationResult.getWidth();

            if (ffmpegStatus) {
                Platform.runLater(() -> ffmpegVerificationResult.setText("Fully Operational"));
            } else {
                Platform.runLater(() -> ffmpegVerificationResult.setText("FFMPEG: Not Configured"));
            }

            while (ffmpegVerificationResult.getWidth() == originalWidth);
            Platform.runLater(() -> ffmpegVerificationResultContainer.setPadding(new Insets(110, 0, 0, -ffmpegVerificationResult.getWidth())));

        }

    }

    // Settings: Not Added
    class outputDirectoryVerification implements Runnable {

        Thread t;
        private volatile boolean complete = false;
        private boolean resetDirectory = false;

        outputDirectoryVerification() {
            t = new Thread(this, "output-directory-verification");
            t.start();
        }

        public boolean changed() {
            return resetDirectory;
        }

        public boolean isComplete() {
            return complete;
        }

        public void run() {

            if (!Files.exists(Paths.get(outputDirectorySetting))) {

                // User specified directory no longer exists, hence return to default directory
                outputDirectorySetting = System.getProperty("user.dir");

                JSONObject savedSettings = SettingsFunc.getSettings();

                try {
                    SettingsFunc.saveSettings(
                            outputDirectorySetting,
                            Math.toIntExact((long) savedSettings.get("music_format")),
                            Math.toIntExact((long) savedSettings.get("save_album_art")),
                            Math.toIntExact((long) savedSettings.get("album_art")),
                            Math.toIntExact((long) savedSettings.get("album_title")),
                            Math.toIntExact((long) savedSettings.get("song_title")),
                            Math.toIntExact((long) savedSettings.get("artist")),
                            Math.toIntExact((long) savedSettings.get("year")),
                            Math.toIntExact((long) savedSettings.get("track")),
                            Math.toIntExact((long) savedSettings.get("theme")),
                            Math.toIntExact((long) savedSettings.get("data_saver"))
                    );
                } catch (JSONException e) {
                    Debug.error(t, "Error saving new settings.", e.getStackTrace());
                }

                resetDirectory = true;

            }

            complete = true;


        }

    }

    // Settings: Not Added
    class outputDirectoryListener implements Runnable {

        Thread t;
        private volatile boolean kill = false;

        public outputDirectoryListener() {

            t = new Thread(this, "output-directory-listener");
            t.start();

        }

        public void kill() {
            kill = true;
        }

        public void run() {

            while (!kill) {

                try {

                    Thread.sleep(100);

                    directoryVerify = new outputDirectoryVerification();
                    // Add to thread debugger but implement specific thread debugging instead of everything, lest be spammed
                    while (!directoryVerify.isComplete());

                    if (directoryVerify.changed()) {

                        Debug.trace(t, "Detected directory changed, updating settings.");

                        double originalWidth = outputDirectoryResult.getWidth();
                        Platform.runLater(() -> outputDirectoryResult.setText(outputDirectorySetting));

                        // Wait for the text to to be set, then update the rest
                        while (originalWidth == outputDirectoryResult.getWidth());
                        Platform.runLater(() -> outputDirectoryResultContainer.setPadding(new Insets(180, 0, 0, -outputDirectoryResult.getWidth())));

                    }

                } catch (InterruptedException ignored) {}

            }

        }

    }

    // Downloads: Not Added
    class generateDownloadHistory implements Runnable {

        private Thread t;

        public generateDownloadHistory() {
            t = new Thread(this, "download-history");
            t.start();
        }

        public void run() {

            // Drawing download queue
            try {
                for (int i = 0; i < downloadQueue.length(); i++) {
                    for (int j = 0; j < (downloadQueue.getJSONObject(i)).getJSONArray("songs").length(); j++) {
                        JSONObject jSong = ((downloadQueue.getJSONObject(i)).getJSONArray("songs")).getJSONObject(j);
                        lastClicked.add(Long.MIN_VALUE);

                        int finalI = i;
                        int finalJ = j;
                        Platform.runLater(() -> {
                            try {
                                downloadEventsView.getItems().add(
                                        generateResult(
                                                Files.exists(
                                                        Paths.get(
                                                                ((downloadQueue.getJSONObject(0)).getJSONObject("meta")).get("directory") + "art.jpg"
                                                        )
                                                ) ? new ImageView(
                                                        new Image(
                                                                new File((downloadQueue.getJSONObject(0)).getJSONObject("meta").get("directory") + "art.jpg").toURI().toString(),
                                                                85,
                                                                85,
                                                                false,
                                                                true
                                                        )
                                                ) : new ImageView(
                                                        new Image(
                                                                ((JSONObject) ((JSONObject) downloadQueue.get(0)).get("meta")).get("art").toString(),
                                                                85,
                                                                85,
                                                                false,
                                                                true
                                                        )
                                                ),
                                                jSong.get("title").toString(),
                                                ((JSONObject) ((JSONObject) downloadQueue.get(0)).get("meta")).get("artist").toString(),
                                                downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).has("eta") ? downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("eta") + " Remaining" : finalI == 0 ? "Downloading..." : "Scheduled",
                                                (String) jSong.get("id"),
                                                ((JSONObject) ((JSONObject) downloadQueue.get(0)).get("meta")).get("directory").toString(),
                                                downloadQueue.getJSONObject(finalI).getJSONArray("songs").getJSONObject(finalJ).has("percentComplete") ? Double.parseDouble(downloadQueue.getJSONObject(finalI).getJSONArray("songs").getJSONObject(finalJ).get("percentComplete").toString()) : null,
                                                false,
                                                downloadQueue.getJSONObject(finalI).getJSONArray("songs").getJSONObject(finalJ).has("percentComplete"),
                                                false,
                                                false
                                        )
                                );
                            } catch (JSONException e) {
                                Debug.error(t, "Failed to form new record from download queue.", e.getStackTrace());
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(0);
                            }
                        });
                    }

                }
            } catch (IndexOutOfBoundsException | JSONException ignored) {}

            // Drawing download history
            try {
                JSONArray downloadHistory = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());

                if (downloadHistory.length() > 0) {
                    for (int i = 0; i < downloadHistory.length(); i++) {

                        lastClicked.add(Long.MIN_VALUE);

                        int finalI = i;
                        Platform.runLater(() -> {
                            try {
                                downloadEventsView.getItems().add(
                                        generateResult(
                                                new ImageView(
                                                        new Image(
                                                                Files.exists(
                                                                        Paths.get(
                                                                                downloadHistory.getJSONObject(finalI).get("cached").toString()
                                                                        )
                                                                ) ? new File(downloadHistory
                                                                            .getJSONObject(finalI)
                                                                            .get("cached")
                                                                            .toString())
                                                                                .toURI()
                                                                                .toString()
                                                                : downloadHistory
                                                                        .getJSONObject(finalI)
                                                                        .get("art")
                                                                        .toString(),
                                                                85,
                                                                85,
                                                                false,
                                                                true
                                                        )
                                                ),
                                                downloadHistory.getJSONObject(finalI).get("title").toString(),
                                                downloadHistory.getJSONObject(finalI).get("artist").toString(),
                                                downloadHistory.getJSONObject(finalI).get("cancelled").toString().equals("0") ? Files.exists(Paths.get(downloadHistory.getJSONObject(finalI).get("downloadFolder").toString())) ? "Downloaded" : "Moved" : "Cancelled",
                                                downloadHistory.getJSONObject(finalI).get("id").toString(),
                                                downloadHistory.getJSONObject(finalI).get("downloadFolder").toString(),
                                                null,
                                                true,
                                                false,
                                                downloadHistory.getJSONObject(finalI).get("cancelled").toString().equals("0") && Files.exists(Paths.get(downloadHistory.getJSONObject(finalI).get("downloadFolder").toString())),
                                                !Files.exists(Paths.get(downloadHistory.getJSONObject(finalI).get("downloadFolder").toString()))
                                        )
                                );
                            } catch (JSONException e) {
                                Debug.error(t, "Failed to form new record from history.", e.getStackTrace());
                            }
                        });

                    }
                } else {
                    // No download history found
                    Debug.trace(null, "didn't find anything");
                }

            } catch (IOException | JSONException e) {

                Debug.error(t, "Error drawing downloads in queue.", e.getStackTrace());

                try {

                    if (Files.exists(Paths.get("resources\\json\\downloads.json")))
                        Files.delete(Paths.get("resources\\json\\downloads.json"));

                    File regenerateFile = new File("resources\\json\\downloads.json");

                    if (!regenerateFile.createNewFile())
                        Debug.error(t, "Failed to regenerate the downloads file.", new IOException().getStackTrace());
                    else
                        Debug.warn(t, "Generated a new downloads folder");
                } catch (IOException ignored1) {}

            } catch (NoSuchElementException ignored) {}
        }
    }

    // Results: Not Added
    class addToQueue implements Runnable {

        private Thread t;
        private ArrayList<String> givenData;
        private volatile boolean killRequest = false;

        public addToQueue(ArrayList<String> givenData) {
            this.givenData = givenData;
            t = new Thread(this, "add-to-queue");
            t.start();
        }

        public void kill() {
            killRequest = true;
        }

        public void restoreButtons() {

            Platform.runLater(() -> {
                cancelButton.setText("Back");
                cancelButton.setOnMouseClicked(g -> cancel(false));
                downloadButton.setText("Download");
                downloadButton.setDisable(false);
                queueAdditionProgress.setVisible(false);
                queueAdditionProgress.setProgress(0);
            });

        }

        public void run() {

            // Attempting to build full object
            JSONObject newQueueItem = new JSONObject();
            JSONObject metaData = new JSONObject();
            JSONArray songsData = new JSONArray();
            Timer timer = new Timer();

            try {

                metaData.put("artist", givenData.get(1));

                if (givenData.get(4).equals("Album")) {

                    metaData.put("albumTitle", givenData.get(0));
                    metaData.put("year", givenData.get(2));
                    metaData.put("genre", givenData.get(3));
                    metaData.put("directory", outputDirectorySetting + "\\" + givenData.get(0) + "\\");
                    metaData.put("art", givenData.get(5));
                    metaData.put("artId", Utils.generateNewCacheArtId(downloadQueue));
                    metaData.put("playtime", 0);

                    // Process songs data
                    long startTime = Instant.now().toEpochMilli();
                    Document albumData = Jsoup.connect(givenData.get(6)).get();
                    Elements tracks = albumData.select("tr.track");

                    for (Element track : tracks) {

                        timer.cancel();
                        timer = new Timer();
                        long finalStartTime = startTime;
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                double newProgress = (double) tracks.indexOf(track) / (tracks.size()-1) + (((double) (Instant.now().toEpochMilli() - finalStartTime) / 1000) / (tracks.size()-1));
                                if (newProgress <= (double) (tracks.indexOf(track)+1) / (tracks.size()-1) )
                                    queueAdditionProgress.setProgress((double) tracks.indexOf(track) / (tracks.size()-1) + (((double) (Instant.now().toEpochMilli() - finalStartTime) / 1000) / (tracks.size()-1)));
                            }
                        }, 0, (int) (((double) (Instant.now().toEpochMilli() - startTime) / 1000) / (tracks.size()-1)*1000) );
                        Platform.runLater(() -> queueAdditionProgress.setProgress((double) tracks.indexOf(track) / (tracks.size()-1) ));

                        startTime = Instant.now().toEpochMilli();

                        if (killRequest) {restoreButtons(); return;}
                        try {
                            JSONObject trackDetails = new JSONObject();
                            trackDetails.put("title", track.select("div.title").text());
                            trackDetails.put("playtime", Integer.toString(Utils.timeConversion(track.select("td.time").text())));
                            trackDetails.put(
                                    "source",
                                    Utils.evaluateBestLink(
                                            Utils.youtubeQuery(
                                                    metaData.get("artist") + " " + track.select("div.title").text()
                                            ),
                                            Integer.parseInt(
                                                    Integer.toString(
                                                            Utils.timeConversion(
                                                                    track.select("td.time")
                                                                            .text()
                                                            )
                                                    )
                                            )
                                    ));
                            trackDetails.put("position", tracks.indexOf(track)+1);
                            trackDetails.put("id", Utils.generateNewId(songsData, downloadQueue));

                            metaData.put("playtime", Integer.parseInt(metaData.get("playtime").toString()) + Utils.timeConversion(track.select("td.time").text()));
                            songsData.put(trackDetails);

                        } catch (IndexOutOfBoundsException ignored) {
                            ignored.printStackTrace();
                            System.exit(0);
                            // Should check here if youtube queries are failing, but likely only occurs because the operation has been cancelled
                        }

                    }

                } else if (givenData.get(4).equals("Song")) {

                    // Three web requests
                    if (killRequest) {restoreButtons(); return;}
                    Document songDataRequest = Jsoup.connect(givenData.get(6)).get();
                    Platform.runLater(() -> queueAdditionProgress.setProgress((double) 1/3));
                    String genre = "";
                    JSONObject songData = new JSONObject();

                    try {
                        genre = songDataRequest.selectFirst("div.song_genres").selectFirst("div.middle").select("a").text();
                        genre = genre.split("\\(")[0];
                        genre = genre.substring(0, genre.length() - 1);
                    } catch (NullPointerException ignored) {}

                    if (killRequest) {restoreButtons(); return;}
                    Document albumDataRequest = Jsoup.connect(songDataRequest.selectFirst("div.title").selectFirst("a").attr("href")).get();
                    Platform.runLater(() -> queueAdditionProgress.setProgress((double) 2/3));

                    Elements tracks0 = albumDataRequest.select("tr.track");
                    Elements tracks1 = albumDataRequest.select("tr.track pick");
                    String positionInAlbum = "-1";

                    for (Element track: tracks0)
                    {
                        if (track.selectFirst("div.title").selectFirst("a").text().equals(givenData.get(0)))
                        {
                            positionInAlbum = track.selectFirst("td.tracknum").text();
                            metaData.put("playtime", Integer.toString(Utils.timeConversion(track.select("td.time").text())));
                            songData.put("playtime", Integer.toString(Utils.timeConversion(track.select("td.time").text())));
                        }
                    }
                    if (positionInAlbum.equals("-1")){
                        for (Element track: tracks1)
                        {
                            if (track.selectFirst("div.title").selectFirst("a").text().equals(givenData.get(0)))
                            {
                                positionInAlbum = track.selectFirst("td.tracknum").text();
                            }
                        }
                    }

                    metaData.put("art", songDataRequest.selectFirst("td.cover").select("img").attr("src"));
                    metaData.put("albumTitle", songDataRequest.selectFirst("div.title").select("a").text());
                    metaData.put("year", songDataRequest.selectFirst("td.year").text());
                    metaData.put("genre", genre);
                    metaData.put("directory", outputDirectorySetting + "\\");
                    metaData.put("artId", Utils.generateNewCacheArtId(downloadQueue));

                    // Add songs data
                    System.out.println(Utils.timeConversion(
                            albumDataRequest.select("tr.track").get(Integer.parseInt(positionInAlbum)).select("td.time")
                                    .text()
                            ));
                    Platform.runLater(() -> queueAdditionProgress.setProgress(1));
                    songData.put("title", givenData.get(0));
                    songData.put("source", Utils.evaluateBestLink(
                                Utils.youtubeQuery(
                                        metaData.get("artist") + " " + givenData.get(0)
                                ),
                                Utils.timeConversion(
                                        songDataRequest.select("td.time").text()
                                )
                            )
                    );
                    songData.put("id", Utils.generateNewId(songsData, downloadQueue));
                    songData.put("position", positionInAlbum);
                    songsData.put(songData);

                } else {
                    Debug.error(t, "Unknown type given: " + givenData.get(4), new Exception().getStackTrace());
                }

            } catch (IOException | JSONException e) {
                Debug.error(t, "Error fetching from url: " + givenData.get(6), e.getStackTrace());
                System.exit(0);
            }

            timer.cancel();

            // Now to piece together
            if (killRequest) {restoreButtons(); return;}

            try {
                newQueueItem.put("meta", metaData);
                newQueueItem.put("songs", songsData);
            } catch (JSONException e) {
                Debug.error(t, "Error piecing together new queue result.", e.getStackTrace());
            }
            Platform.runLater(() -> {
                downloadQueue.put(newQueueItem);

                System.out.println(downloadQueue);

                downloadButton.setText("Download");
                downloadButton.setDisable(false);
                cancelButton.setText("Back");
                cancelButton.setOnMouseClicked(e -> cancel(false));
                queueAdditionProgress.setVisible(false);
                queueAdditionProgress.setProgress(0);
            });
        }

    }

    // Model: Not Added
    class downloadsListener implements Runnable {

        Thread t;
        private volatile boolean kill = false;

        public downloadsListener() {
            t = new Thread(this, "downloads-listener");
            t.start();
        }

        // Must use when exiting
        public void kill() {
            kill = true;
        }

        // Add functionality
        public void skip() {

            // Skip the downloading of the current song

        }

        // Could switch to passing a JSONObject
        private void applyMetaData(String targetFile, String newFile, RandomAccessFile art, String title, String albumTitle, String artist, String year, String albumPosition) throws IOException, InvalidDataException, UnsupportedTagException {

            Mp3File mp3Applicator = new Mp3File(targetFile);

            ID3v2 id3v2tag = new ID3v24Tag();
            mp3Applicator.setId3v2Tag(id3v2tag);

            if (applyAlbumArt) {

                // Could break this up into mb loads
                byte[] bytes;
                bytes = new byte[(int) art.length()]; // Maybe make this 1024
                art.read(bytes);
                art.close();

                id3v2tag.setAlbumImage(bytes, "image/jpg");
            }

            // Applying remaining data
            if (applySongTitle)
                id3v2tag.setTitle(title);

            if (applyAlbumTitle)
                id3v2tag.setAlbum(albumTitle);

            if (applyArtist) {
                id3v2tag.setArtist(artist);
                id3v2tag.setAlbumArtist(artist);
            }

            if (applyYear)
                id3v2tag.setYear(year);

            // Add this as a possible setting, also contain
            id3v2tag.setTrack(albumPosition);

            try {
                mp3Applicator.save(newFile);

                // Delete old file
                if ( !new File(targetFile).delete()) {
                    Debug.error(t, "Failed to delete file: " + targetFile, new IOException().getStackTrace());
                }

            } catch (IOException | NotSupportedException e) {
                e.printStackTrace();
            }

        }

        public void run() {

            while (!kill) {

                // Done like this to allow the data to be modified and downloads easily cancelled or added
                while (downloadQueue.length() > 0) {

                    try {

                        // Updating the downloads queue to inform of the progress
                        downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).put("percentComplete", 0);

                        // Checking the folder and album art, creating is necessary
                        if (!Files.exists(Paths.get(downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory") + "art.jpg"))) {

                            // Creating the folder if necessary
                            if (!Files.exists(Paths.get(downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory").toString())))
                                downloadQueue.getJSONObject(0).getJSONObject("meta").put("directory", Utils.generateFolder(downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory").toString()));

                            // Album art
                            try {
                                // Downloading album art and saving to target directory
                                FileUtils.copyURLToFile(new URL(downloadQueue.getJSONObject(0).getJSONObject("meta").get("art").toString()), new File(downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory").toString() + "\\art.jpg"));

                                // Clone the album art to the cache
                                Files.copy(
                                        Paths.get(downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory").toString() + "\\art.jpg"),
                                        Paths.get("resources\\cache\\" + downloadQueue.getJSONObject(0).getJSONObject("meta").get("artId").toString() + ".jpg")
                                );
                            }
                            catch (IOException ignored) {}

                        }

                        // Updating the table if possible
                        /*
                        try {

                            // Determining the target row ID based off of songId
                            for (Object result: downloadEventsView.getItems()) {

                                if (((BorderPane) result).getId().equals(downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("id"))) {

                                    // Update the table
                                    Platform.runLater(() -> {
                                                try {

                                                    downloadEventsView.getItems().set(
                                                            downloadEventsView.getItems().indexOf(result),
                                                            generateResult(
                                                                    Files.exists(
                                                                            Paths.get(
                                                                                    downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory") + "art.jpg"
                                                                            )
                                                                    ) ? new ImageView(
                                                                            new Image(
                                                                                    new File(downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory") + "art.jpg")
                                                                                            .toURI()
                                                                                            .toString(),
                                                                                    85,
                                                                                    85,
                                                                                    false,
                                                                                    true
                                                                            )
                                                                    ) : new ImageView(
                                                                            new Image(
                                                                                    ((downloadQueue.getJSONObject(0)).getJSONObject("meta")).get("art").toString(),
                                                                                    85,
                                                                                    85,
                                                                                    false,
                                                                                    true
                                                                            )
                                                                    ),
                                                                    downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("title").toString(),
                                                                    ((downloadQueue.getJSONObject(0)).getJSONObject("meta")).get("artist").toString(),
                                                                    "0% Complete | ETA: Calculating...",
                                                                    downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("id").toString(),
                                                                    ((downloadQueue.getJSONObject(0)).getJSONObject("meta")).get("directory").toString(),
                                                                    (double) 0,
                                                                    false,
                                                                    true,
                                                                    false,
                                                                    false
                                                            )
                                                    );
                                                } catch (JSONException ignored) {
                                                }
                                            }
                                    );
                                }
                            }

                        } catch (ConcurrentModificationException ignored) {}

                         */
                        // Now begin the download and update the record as it downloads
                        download songDownload = new download(
                                downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory").toString(),
                                downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("source").toString(),
                                formatReferences.get(musicFormatSetting)
                        );

                        // Update the UI as the download progresses
                        String referenceEta = "";
                        double percentComplete = 0;

                        // Could switch to a timer and run every 50ms to avoid excess work for the cpu
                        while (!songDownload.isComplete()) {

                            // Continuously update the UI with the progress, when any values change
                            try {
                                if (!referenceEta.equals(songDownload.getEta()) || percentComplete != songDownload.getPercentComplete()) {
                                    // Updating the variables for reference
                                    referenceEta = songDownload.getEta();
                                    percentComplete = songDownload.getPercentComplete();

                                    // Updating the downloads queue data
                                    downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).put("percentComplete", songDownload.getPercentComplete() / 100);
                                    downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).put("eta", songDownload.getEta());

                                    // Update the UI, seems to fail
                                    /*
                                    Platform.runLater(() -> {

                                        // Update the UI
                                        for (Object result : downloadEventsView.getItems()) {

                                            try {

                                                if (((BorderPane) result).getId().equals(downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("id"))) {

                                                    System.out.println(downloadEventsView.getItems().indexOf(result));

                                                    BorderPane updatedItem = generateResult(
                                                            new ImageView(
                                                                    new Image(
                                                                            new File(downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory") + "art.jpg")
                                                                                    .toURI()
                                                                                    .toString(),
                                                                            85,
                                                                            85,
                                                                            true,
                                                                            true
                                                                    )
                                                            ),
                                                            downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("title").toString(),
                                                            downloadQueue.getJSONObject(0).getJSONObject("meta").get("artist").toString(),
                                                            songDownload.getEta(),
                                                            ((BorderPane) result).getId(),
                                                            downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("directory").toString().replace("\\", "/"),
                                                            songDownload.getPercentComplete() / 100,
                                                            false,
                                                            true,
                                                            true,
                                                            false
                                                    );
                                                    updatedItem.setId(((BorderPane) downloadEventsView.getItems().get(downloadEventsView.getItems().indexOf(result))).getId());

                                                    downloadEventsView.getItems().set(
                                                            downloadEventsView.getItems().indexOf(result),
                                                            updatedItem
                                                    );

                                                }

                                            } catch (JSONException e) {

                                                Debug.error(t, "Error updating the table with new downloads information", e.getStackTrace());

                                            } catch (ConcurrentModificationException ignored) {}
                                        }

                                    });

                                     */

                                }
                            } catch (NullPointerException ignored) {}

                        }

                        // Apply meta-data and move the file
                        if (formatReferences.get(musicFormatSetting).equals("mp3")) {

                            // Waiting for ffmpeg processing
                            while (!new File(System.getProperty("user.dir") + "\\" + songDownload.getFileName() + ".mp3").renameTo(new File(System.getProperty("user.dir") + "\\" + songDownload.getFileName() + ".mp3")));

                            try {
                                applyMetaData(
                                        System.getProperty("user.dir") + "\\" + songDownload.getFileName() + ".mp3",
                                        downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory").toString() + downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("title").toString() + ".mp3",
                                        new RandomAccessFile(downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory").toString() + "art.jpg", "r"),
                                        downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("title").toString(),
                                        downloadQueue.getJSONObject(0).getJSONObject("meta").get("albumTitle").toString(),
                                        downloadQueue.getJSONObject(0).getJSONObject("meta").get("artist").toString(),
                                        downloadQueue.getJSONObject(0).getJSONObject("meta").get("year").toString(),
                                        downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("position").toString()
                                );
                            } catch (IOException | InvalidDataException | UnsupportedTagException e) {
                                Debug.error(t, "Error applying meta data & creating new file.", e.getStackTrace());
                            }

                        }

                        // Add to downloads history
                        try {
                            Utils.updateDownloads(
                                    new JSONObject(
                                            String.format(
                                                    "{\"downloadFolder\": \"%s\", \"art\": \"%s\", \"artist\": \"%s\", \"cached\": \"%s\", \"cancelled\": 0, \"playtime\": %d, \"id\": %.0f, \"title\": \"%s\", \"timestamp\": %d}",
                                                    downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory").toString().replace("\\", "/"),
                                                    downloadQueue.getJSONObject(0).getJSONObject("meta").get("art"),
                                                    downloadQueue.getJSONObject(0).getJSONObject("meta").get("artist"),
                                                    "resources/cached/" + downloadQueue.getJSONObject(0).getJSONObject("meta").get("artId") + ".jpg",
                                                    Integer.parseInt(downloadQueue.getJSONObject(0).getJSONObject("meta").get("playtime").toString()),
                                                    Double.parseDouble(downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("id").toString()),
                                                    downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("title"),
                                                    Instant.now().toEpochMilli()
                                            )
                                    )
                            );
                        } catch (JSONException | IOException e) {
                            Debug.error(t, "Failed to update downloads", e.getStackTrace());
                        }

                        // Deleting the current downloaded song
                        int eventsViewEntityCheck = downloadEventsView.getItems().size();
                        Platform.runLater(() -> {

                                try {

                                    for (Object result : downloadEventsView.getItems()) {

                                        if (((BorderPane) result).getId().equals(downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("id"))) {

                                            downloadEventsView.getItems().remove(result);

                                        }
                                    }

                                } catch (Exception e) {
                                }
                            });

                        // Now add to the table as a completed element & remove existing
                        while (downloadEventsView.getItems().size() == eventsViewEntityCheck);

                        eventsViewEntityCheck = downloadEventsView.getItems().size();
                        Platform.runLater(() -> {

                            try {
                                // Appears to be adding one ahead from what it should
                                downloadEventsView.getItems().add(
                                        generateResult(
                                                new ImageView(
                                                        new Image(
                                                                new File(downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory") + "art.jpg")
                                                                        .toURI()
                                                                        .toString(),
                                                                85,
                                                                85,
                                                                false,
                                                                true
                                                        )
                                                ),
                                                downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("title").toString(),
                                                downloadQueue.getJSONObject(0).getJSONObject("meta").get("artist").toString(),
                                                "Downloaded",
                                                downloadQueue.getJSONObject(0).getJSONArray("songs").getJSONObject(0).get("id").toString(),
                                                downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory").toString(),
                                                songDownload.getPercentComplete() / 100,
                                                false,
                                                false,
                                                true,
                                                false
                                        )
                                );

                            } catch (JSONException e) {
                                Debug.error(t, "Error updating the table.", e.getStackTrace());
                            } catch (ConcurrentModificationException ignored) {}

                        });

                        // Not waiting for the previous code to execute in theory
                        while (downloadEventsView.getItems().size() == eventsViewEntityCheck);// {System.out.println("This is where I think we are stuck");};

                        // Now to update the downloads queue and remove the object
                        if (downloadQueue.getJSONObject(0).getJSONArray("songs").length() == 1) {

                            Debug.trace(t, "Removing download request 0 of " + downloadQueue.length());

                            // Check if we should now delete the album art file if that was the last song in the request to download
                            if (downloadQueue.getJSONObject(0).getJSONArray("songs").length() == 1) {

                                // Check the users setting here against meta and settings
                                File albumArt = new File(downloadQueue.getJSONObject(0).getJSONObject("meta").get("directory") + "art.jpg");
                                if (!albumArt.delete()) {
                                    Debug.error(t, "Failed to delete file: " + albumArt.getAbsolutePath(), new IOException().getStackTrace());
                                }

                                // Remove the entire thing from the downloads queue
                                JSONArray newQueue = new JSONArray();
                                for (int i = 1; i < downloadQueue.length(); i++)
                                    newQueue.put(downloadQueue.getJSONObject(i));
                                downloadQueue = newQueue;

                            }

                        } else {

                            Debug.trace(t, "Removing element 0 of " + downloadQueue.getJSONObject(0).getJSONArray("songs").length());

                            // Simply remove the element
                            JSONArray newSongs = new JSONArray();
                            for (int i = 1; i < downloadQueue.getJSONObject(0).getJSONArray("songs").length(); i++)
                                newSongs.put(downloadQueue.getJSONObject(0).getJSONArray("songs").get(i));

                            downloadQueue.getJSONObject(0).put("songs", newSongs);


                        }

                    } catch (JSONException e) {
                        Debug.error(t, "Error while downloading...", e.getStackTrace());
                    }

                }

            }

            Debug.trace(null, "Download listener finished, program should now be safe to exit.");

        }

    }

    // Downloads: Not Added
    static class download implements Runnable {

        Thread t;
        private String url;
        private String directory;
        private String format;
        private volatile String downloadSpeed;
        private volatile String eta;
        private volatile String targetFileName;
        private double percentComplete = 0;
        private volatile boolean complete = false;

        public download(String dir, String urlRequest, String form) {
            directory = dir;
            url = urlRequest;
            format = form;

            t = new Thread(this, "download");
            t.start();
        }

        public String getDownloadSpeed() {
            return downloadSpeed;
        }

        public double getPercentComplete() {
            return percentComplete;
        }

        public String getEta() {
            return eta;
        }

        public boolean isComplete() {
            return complete;
        }

        public String getFileName() {

            return targetFileName;

        }

        public void run() {

            try {

                // Making the bat to execute
                FileWriter batCreator = new FileWriter(directory + "\\exec.bat");

                batCreator.write("youtube-dl " + url + " --extract-audio --audio-format " + format + " --ignore-errors --retries 10");
                batCreator.close();

                // Sending the Youtube-DL Request
                ProcessBuilder builder = new ProcessBuilder(directory + "\\exec.bat");
                builder.redirectErrorStream(true);
                Process process = builder.start();
                InputStream is = process.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = reader.readLine()) != null) {

                    try {

                        if (line.contains("Destination")) {

                            targetFileName = line.split(":")[1].split("\\.")[0].strip();

                        }

                        if (line.contains("[download]") && !line.contains("Destination")) {

                            // Parse Percent Complete
                            percentComplete = Double.parseDouble(line.split("%")[0].split("]")[1].split("\\.")[0].replaceAll("\\D+", "").replaceAll("\\s",""));

                            // Parse Download Speed
                            downloadSpeed = line.split("at")[1].split("ETA")[0].replaceAll("\\s","");

                            // Parse ETA
                            eta = Integer.parseInt(line.split("ETA")[1].replaceAll("\\s","").split(":")[0]) *60 + line.split("ETA")[1].replaceAll("\\s","").split(":")[1];

                            // Seems to hang without this
                            if (percentComplete == (double) 100)
                                break;

                        }

                    } catch (ArrayIndexOutOfBoundsException | NumberFormatException ignored) {}
                }

                File deleteBat = new File(directory + "\\exec.bat");
                if (!deleteBat.delete())
                    Debug.error(t, "Failed to delete bat file: " + deleteBat.getAbsolutePath(), new IOException().getStackTrace());

            } catch (IOException e) {
                Debug.error(t, "Error in youtube-dl wrapper execution", e.getStackTrace());
            }

            complete = true;

        }

    }

    // Model: Not Added
    static class optimiseCache implements Runnable {

        Thread t;

        public optimiseCache() {
            t = new Thread(this, "cache-optimisation");
            t.start();
        }

        public void run() {

            // List all files
            File[] cachedFiles = new File("resources\\cached").listFiles();
            ArrayList<String> imgData = new ArrayList<>();
            ArrayList<ArrayList<String>> rename = new ArrayList<>();

            // File sizes at different times
            int deleteNonJpgFiles = 0;
            int duplicatedDeletedFiles = 0;
            int preOptimisationDirectorySize = 0;
            int preNonJpgRemoval = 0;
            int directoryOptimisedSize = 0;

            if (cachedFiles == null | Objects.requireNonNull(cachedFiles).length == 0)
                return;

            for (File checkFile: Objects.requireNonNull(new File("resources\\cached\\").listFiles()))
                preNonJpgRemoval += checkFile.length();

            // Load binary data, md5 hashes instead of keeping full file in memory
            for (File workFile : cachedFiles) {
                if (workFile.getName().split("\\.")[1].equals("jpg")) {

                    try {
                        String hash = DigestUtils.md5Hex(
                                Files.newInputStream(
                                        Paths.get(
                                                workFile.getAbsolutePath()
                                        )
                                )
                        );

                        if (imgData.contains(hash)) {

                            // Exists in data hence to setup a rename reference
                            rename.add(
                                    new ArrayList<>(
                                            Arrays.asList(
                                                    workFile.getName(),
                                                    cachedFiles[imgData.indexOf(hash)].getName()
                                            )
                                    )
                            );

                        } else {
                            imgData.add(hash);
                        }


                    } catch (IOException ignored) {}

                } else {

                    if (!workFile.delete())
                        Debug.warn(t, "Failed to delete non jpeg file in cache: " + workFile.getAbsolutePath());
                    else
                        deleteNonJpgFiles++;

                }

            }

            // Getting the size of the cache before the operation and after
            for (File existingFile: Objects.requireNonNull(new File("resources\\cached\\").listFiles())) {
                preOptimisationDirectorySize += existingFile.length();
            }

            // Inform user is non-jpg files were deleted and how much of a reduction that was
            if (deleteNonJpgFiles > 0)
                Debug.trace(t, "Deleted " + deleteNonJpgFiles + " non jpg files from cache, reduction of " + Math.round(((double)(preNonJpgRemoval - preOptimisationDirectorySize) / preNonJpgRemoval)*100) + "%");

            // If there are no files to rename just end this now
            if (rename.size() == 0)
                return;

            // Begin the process of deleting files and renaming references
            for (ArrayList<String> fileNames: rename) {

                // Delete the old no longer relevant file
                if (!new File("resources/cached/" + fileNames.get(0)).delete())
                    Debug.warn(t, "Failed to delete file: " + new File("resources\\cached\\" + fileNames.get(0)).getAbsolutePath());
                else
                    duplicatedDeletedFiles++;

                // Delete all references to the file in the downloads history
                try {

                    // Can cause an error if the file is blank, but will load the jsonarray
                    JSONArray downloadsHistory = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());

                    // Iterate through downloads history, replacing the old references with the new
                    for (int i = 0; i < downloadsHistory.length(); i++) {

                        try {
                            if (downloadsHistory.getJSONObject(i).get("cached").toString().equals("resources/cached/" + fileNames.get(1)))
                                downloadsHistory.getJSONObject(i).put("cached", "resources/cached/" + fileNames.get(0));
                        } catch (JSONException ignored) {}

                    }

                    // Writing changes
                    try {
                        FileWriter updateDownloads = new FileWriter("resources\\json\\downloads.json");
                        updateDownloads.write(downloadsHistory.toString());
                        updateDownloads.close();

                    } catch (IOException e) {
                        // Likely file deleted, adjust to handle, for now just error
                        Debug.error(t, "Error writing to downloads file", e.getStackTrace());
                    }

                } catch (JSONException e) {

                    // There is no download history and no use of the cached files, hence delete them
                    for (File deleteFile: cachedFiles) {
                        if (!deleteFile.delete()) {
                            Debug.warn(t, "Failed to delete file: " + deleteFile.getAbsolutePath());
                        }
                    }
                    return;

                } catch (FileNotFoundException ignored) {
                    // Generate a new downloads folder
                }

            }

            for (File existingFile: Objects.requireNonNull(new File("resources/cached/").listFiles()))
                directoryOptimisedSize += existingFile.length();

            Debug.trace(t, "Deleted " + duplicatedDeletedFiles + " duplicate files from the cache, size reduction of " + Math.round(((double) (preOptimisationDirectorySize - directoryOptimisedSize) / preOptimisationDirectorySize)*100) + "%");

        }

    }

}