package sample;

import com.mpatric.mp3agic.*;
import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.IntStream;

/*
 Git
 TODO: Add a README
 TODO: Add a license
 TODO: Add a gitignore: music files, art.jpg, possible class files?

 Optimisations
 TODO: If a particular request is taking too long, let's say >2 seconds, just use default data to save query time
 TODO: When the download is completed hide the timer and change the completed text to green
 TODO: Move CSS Files somewhere else
 TODO: Rewrite Main.css and redesign general look of the application
 TODO: Fix warnings
 TODO: Restructure threading to separate files and threading objects for statuses and debugging, start time, end time, status, exit details etc
 TODO: Try to use better objects for transmissions
 TODO: Look at how global variables are used
 TODO: Change a lot of start content of a java-fx page thing
 TODO: Windows file explorer would preferable to JavaFX one
 TODO: Change youtube-dl wrapper to my own wrapper where I can get progress as it downloads and report that
 TODO: Go through each function and optimise

 Known Bugs
 TODO: Fix bug where sometimes table would be visible without elements, not sure how to trigger
 TODO: Buttons appear to overlap in settings
 TODO: Add error handling when the request directory no longer exists for downloads, default to standard
 TODO: Estimated timer is far greater than it should be

 Features
 TODO: Add a "real" download speed indicator instead of just effective
 TODO: Try and make searching a bit nicer, no just no results page, maybe a table with default elements, searching page... or something else
 TODO: Add button to install and configure youtube-dl & ffmpeg
 TODO: Look into macOS and Linux compatibility

 Misc
 TODO: Add testing
 TODO: Remove JARs and use Gradle
 TODO: Use debugging and error classes properly

*/

public class View implements EventHandler<KeyEvent>
{
    public Controller controller;
    public Settings settings;

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
    public TableView resultsTable;
    public Label searchResultsTitle;
    public Label title;
    public Button downloadButton;
    public Button cancelButton;
    public ProgressBar loading;
    public Label searchesProgressText;
    public Line footerMarker;
    public Label settingsLink;
    public Button settingsLinkButton;
    public TableView autocompleteResultsTable;
    public Label downloadSpeedLabel;
    public Label timeRemainingLabel;

    // Settings
    VBox versionResultContainer;
    VBox latestVersionResultContainer;
    VBox youtubeDlVerificationResultContainer;
    VBox ffmpegVerificationResultContainer;

    VBox outputDirectoryResultContainer;
    VBox outputDirectoryButtonContainer;
    VBox songDownloadFormatResultContainer;
    VBox saveAlbumArtResultContainer;

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
    public Button outputDirectoryButton;
    public Label songDownloadFormat;
    public ComboBox<String> songDownloadFormatResult;
    public Label saveAlbumArt;
    public ComboBox<String> saveAlbumArtResult;

    // Metadata
    public Label metaDataTitle;
    public Label albumArtSetting;
    public ComboBox<String> albumArtSettingResult;
    public Label albumTitleSetting;
    public ComboBox<String> albumTitleSettingResult;
    public Label songTitleSetting;
    public ComboBox<String> songTitleSettingResult;
    public Label artistSetting;
    public ComboBox<String> artistSettingResult;
    public Label yearSetting;
    public ComboBox<String> yearSettingResult;
    public Label trackNumberSetting;
    public ComboBox<String> trackNumberSettingResult;

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

    // Data
    public ArrayList<ArrayList<String>> searchResults;
    public Utils.resultsSet[] searchResultFullData;
    public ArrayList<ArrayList<String>> songsData;
    public ArrayList<Utils.resultsSet> resultsData;
    public HashMap<String, String> metaData;

    public ArrayList<String> formatReferences = new ArrayList<>(Arrays.asList("mp3", "wav", "ogg", "aac"));

    public double loadingPercent;
    public double percentIncrease;

    public int downloadSpeed;

    generateAutocomplete autocompleteGenerator;
    timerCountdown countDown;
    loadingIncrementer prettyLoader;

    public boolean quitQueryThread = false;
    public boolean quitDownloadThread = false;

    public View(int w, int h) {
        Debug.trace("View::<constructor>");
        width = w;
        height = h;
    }

    public void start(Stage window) {

        /* JAVA-FX INITIALISATION */

        pane = new Pane();
        pane.setId("initial");

        canvas = new Canvas(width, height);
        pane.getChildren().add(canvas);

        /* LOADING DATA */

        new smartQuitDownload();

        mainWindow = window;
        settings = new Settings();
        JSONObject config = settings.getSettings();

        outputDirectorySetting = (String) config.get("output_directory");
        OutputDirectorySettingNew = outputDirectorySetting;
        musicFormatSetting = Integer.parseInt(Long.toString((Long) config.get("music_format")));
        saveAlbumArtSetting = Integer.parseInt(Long.toString((Long) config.get("save_album_art")));

        applyAlbumArt = (Long) config.get("album_art") != 0;
        applyAlbumTitle = (Long) config.get("album_title") != 0;
        applySongTitle = (Long) config.get("song_title") != 0;
        applyArtist = (Long) config.get("artist") != 0;
        applyYear = (Long) config.get("year") != 0;
        applyTrack = (Long) config.get("track") != 0;

        darkTheme = (Long) config.get("theme") != 0;
        dataSaver = (Long) config.get("data_saver") != 0;

        programVersion = settings.getVersion();

        /* JAVA-FX DESIGN */

        /* Search Page */
        title = new Label("Music Downloader");
        title.setId("title");

        searchRequest = new TextField();
        searchRequest.setId("search");
        searchRequest.setPrefSize(400, 20);
        searchRequest.textProperty().addListener((observableValue, s, t1) -> generateQueryAutocomplete(t1));

        footerMarker = new Line(0, 0, 0, 0);
        footerMarker.setId("line");

        settingsLink = new Label("Settings");
        settingsLink.setTranslateX(10);
        settingsLink.setId("subTitle2");

        settingsLinkButton = new Button();
        settingsLinkButton.setPrefSize(100, 25);
        settingsLinkButton.setId("button");
        settingsLinkButton.setOpacity(0);
        settingsLinkButton.setOnAction(e -> settingsMode());
        settingsLinkButton.setTranslateX(10);

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

        autocompleteResultsTable.getColumns().addAll(iconColumn, nameColumn);

        downloadButton = new Button("Download");
        downloadButton.setPrefSize(120, 40);
        downloadButton.setTranslateX(50);
        downloadButton.setOnAction(
                e -> {
                    try {
                        initializeDownload();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
        );
        downloadButton.setVisible(false);

        cancelButton = new Button("Back");
        cancelButton.setPrefSize(120, 40);
        cancelButton.setOnAction(e -> cancel());
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

        /* Settings Page */
        settingsTitle = new Label("Settings");
        settingsTitle.setId("subTitle");

        programSettingsTitle = new Label("Information");
        programSettingsTitle.setId("settingsHeader");

        version = new Label("Version: ");
        version.setId("settingInfo");

        versionResult = new Label(programVersion);
        versionResult.setId("settingInfo");

        latestVersion = new Label("Latest Version: ");
        latestVersion.setId("settingInfo");

        latestVersionResult = new Label("Locating...");
        latestVersionResult.setId("settingInfo");
        if (dataSaver) {
            new getLatestVersion();
        }

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
        outputDirectoryResult.setId("settingInfo");

        outputDirectoryButton = new Button();
        outputDirectoryButton.setId("button");
        outputDirectoryButton.setOpacity(0);
        outputDirectoryButton.setPrefHeight(25);
        outputDirectoryButton.setOnAction(e -> new selectFolder());

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

        albumArtSetting = new Label("Album Art: ");
        albumArtSetting.setId("settingInfo");

        albumArtSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        albumArtSettingResult.setOnAction(e -> evaluateSettingsChanges());
        albumArtSettingResult.setId("combobox");

        albumTitleSetting = new Label("Album Title: ");
        albumTitleSetting.setId("settingInfo");

        albumTitleSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        albumTitleSettingResult.setOnAction(e -> evaluateSettingsChanges());
        albumTitleSettingResult.setId("combobox");

        songTitleSetting = new Label("Song Title: ");
        songTitleSetting.setId("settingInfo");

        songTitleSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        songTitleSettingResult.setOnAction(e -> evaluateSettingsChanges());
        songTitleSettingResult.setId("combobox");

        artistSetting = new Label("Artist: ");
        artistSetting.setId("settingInfo");

        artistSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        artistSettingResult.setOnAction(e -> evaluateSettingsChanges());
        artistSettingResult.setId("combobox");

        yearSetting = new Label("Year: ");
        yearSetting.setId("settingInfo");

        yearSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        yearSettingResult.setOnAction(e -> evaluateSettingsChanges());
        yearSettingResult.setId("combobox");

        trackNumberSetting = new Label("Track Number: ");
        trackNumberSetting.setId("settingInfo");

        trackNumberSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        trackNumberSettingResult.setOnAction(e -> evaluateSettingsChanges());
        trackNumberSettingResult.setTranslateY(480);
        trackNumberSettingResult.setId("combobox");

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
        programSettingNameContainer.getChildren().addAll(
                programSettingsTitle,
                programSettingsTitleLine
        );
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

        VBox fileSettingInfoContainer = new VBox(7.5);
        fileSettingInfoContainer.getChildren().addAll(
                outputDirectory,
                songDownloadFormat,
                saveAlbumArt
        );
        fileSettingInfoContainer.setPadding(new Insets(180, 0, 0, 0));

        outputDirectoryResultContainer = new VBox();
        outputDirectoryResultContainer.getChildren().add(outputDirectoryResult);

        outputDirectoryButtonContainer = new VBox();
        outputDirectoryButtonContainer.getChildren().add(outputDirectoryButton);

        songDownloadFormatResultContainer = new VBox();
        songDownloadFormatResultContainer.getChildren().add(songDownloadFormatResult);

        saveAlbumArtResultContainer = new VBox();
        saveAlbumArtResultContainer.getChildren().add(saveAlbumArtResult);

        VBox metaDataNameContainer = new VBox();
        metaDataNameContainer.getChildren().addAll(metaDataTitle, metaDataTitleLine);
        metaDataNameContainer.setPadding(new Insets(270, 0, 0, 0));

        metaDataInfoContainer = new VBox();
        metaDataInfoContainer.getChildren().addAll(
                albumArtSetting,
                albumTitleSetting,
                songTitleSetting,
                artistSetting,
                yearSetting,
                trackNumberSetting
        );
        metaDataInfoContainer.setSpacing(7.5);
        metaDataInfoContainer.setPadding(new Insets(300, 0, 0, 0));

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
        applicationSettingNameContainer.setPadding(new Insets(470, 0, 0, 0));

        VBox applicationSettingsInfoContainer = new VBox(7.5);
        applicationSettingsInfoContainer.getChildren().addAll(darkModeSetting, dataSaverSetting);
        applicationSettingsInfoContainer.setPadding(new Insets(500, 0, 0, 0));

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
                fileSettingInfoContainer,
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
                outputDirectoryButtonContainer,
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

        // Search Page
        pane.getChildren().addAll(
                title,
                searchRequest,
                autocompleteResultsTable,
                footerMarker,
                settingsLink,
                settingsLinkButton
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
                downloadSpeedLabel
        );

        // Settings Page
        pane.getChildren().addAll(
                settingsContainer,
                confirmChanges,
                cancelBackButton
        );

        scene = new Scene(pane);
        scene.getStylesheets().add(getClass().getResource("main.css").toExternalForm());
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
        autocompleteResultsTable.layoutXProperty().bind(mainWindow.widthProperty().divide(2).add(- searchRequest.getWidth() / 2));
        autocompleteResultsTable.layoutYProperty().bind(mainWindow.heightProperty().divide(2).add(-56));
        autocompleteResultsTable.maxHeightProperty().bind(mainWindow.heightProperty().divide(2).subtract(185));

        footerMarker.endXProperty().bind(mainWindow.widthProperty());
        footerMarker.startYProperty().bind(mainWindow.heightProperty().add(-89));
        footerMarker.endYProperty().bind(mainWindow.heightProperty().add(-89));
        settingsLink.layoutYProperty().bind(mainWindow.heightProperty().add(-79));
        settingsLinkButton.layoutYProperty().bind(mainWindow.heightProperty().add(-79));

        // Bindings: Settings
        settingsContainer.prefWidthProperty().bind(mainWindow.widthProperty().subtract(73));
        settingsContainer.prefHeightProperty().bind(mainWindow.heightProperty().subtract(confirmChanges.getHeight()+104));

        versionResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(50, 0, 0, -versionResult.getWidth())));
        latestVersionResultContainer.setPadding(new Insets(70, 0, 0, -latestVersionResult.getWidth()));
        youtubeDlVerificationResultContainer.setPadding(new Insets(90, 0, 0, -youtubeDlVerificationResult.getWidth()));
        ffmpegVerificationResultContainer.setPadding(new Insets(110, 0, 0, -ffmpegVerificationResult.getWidth()));

        outputDirectoryResultContainer.setPadding(new Insets(180, 0, 0, -outputDirectoryResult.getWidth()));
        outputDirectoryButtonContainer.setPadding(new Insets(180, 0, 0, -outputDirectoryResult.getWidth()));
        songDownloadFormatResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(205, 0, 0, -songDownloadFormatResult.getWidth())));
        saveAlbumArtResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(230, 0, 0, -saveAlbumArtResult.getWidth())));

        albumArtSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(300, 0, 0, -albumArtSettingResult.getWidth())));
        albumTitleSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(325, 0, 0, -albumTitleSettingResult.getWidth())));
        songTitleSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(350, 0, 0, -songTitleSettingResult.getWidth())));
        artistSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(375, 0, 0, -artistSettingResult.getWidth())));
        yearSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(400, 0, 0, -yearSettingResult.getWidth())));
        trackNumberSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(-55, 0, 0, -trackNumberSettingResult.getWidth())));

        darkModeSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(500, 0, 0, -darkModeSettingResult.getWidth())));
        dataSaverSettingResultContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(525, 0, 0, -dataSaverSettingResult.getWidth())));

        settingTitleLine.endXProperty().bind(mainWindow.widthProperty().subtract(99.5));
        programSettingsTitleLine.endXProperty().bind(mainWindow.widthProperty().subtract(99.5));
        fileSettingsTitleLine.endXProperty().bind(mainWindow.widthProperty().subtract(99.5));
        metaDataTitleLine.endXProperty().bind(mainWindow.widthProperty().subtract(99.5));
        applicationSettingTitleLine.endXProperty().bind(mainWindow.widthProperty().subtract(99.5));

        confirmChanges.layoutYProperty().bind(mainWindow.heightProperty().subtract( 64 + confirmChanges.getHeight()));
        cancelBackButton.layoutXProperty().bind(mainWindow.widthProperty().subtract(44 + cancelBackButton.getWidth()));
        cancelBackButton.layoutYProperty().bind(mainWindow.heightProperty().subtract(64 + cancelBackButton.getHeight()));
        outputDirectoryButton.setPrefWidth(outputDirectoryResult.getWidth());

        // Bindings: Search Results
        resultsTable.prefWidthProperty().bind(mainWindow.widthProperty().subtract(119.5));
        resultsTable.prefHeightProperty().bind(mainWindow.heightProperty().subtract(230));
        downloadButton.layoutYProperty().bind(mainWindow.heightProperty().subtract(170));
        cancelButton.layoutXProperty().bind(mainWindow.widthProperty().subtract(69.5 + cancelButton.getWidth()));
        cancelButton.layoutYProperty().bind(mainWindow.heightProperty().subtract(170));
        searchesProgressText.layoutYProperty().bind(mainWindow.heightProperty().subtract(115));
        timeRemainingLabel.setTranslateX( (loading.getWidth() / 2) + (timeRemainingLabel.getWidth()/2) - 19.5 );
        timeRemainingLabel.layoutYProperty().bind(mainWindow.heightProperty().subtract(115));
        downloadSpeedLabel.layoutXProperty().bind(loading.widthProperty().subtract(19.5));
        downloadSpeedLabel.layoutYProperty().bind(mainWindow.heightProperty().subtract(115));
        loading.prefWidthProperty().bind(resultsTable.widthProperty());
        loading.layoutYProperty().bind(mainWindow.heightProperty().subtract(95));

    }

    public void handle(KeyEvent event) {
        try {
            controller.userKeyInteraction(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized ArrayList<ArrayList<String>> handleSearch() {

        // Could look at changing how vars are passed, less global ideally
        new allMusicQuery();
        try { autocompleteGenerator.kill(); } catch (Exception ignored) {}

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
        settingsLinkButton.setVisible(false);
        settingsLink.setVisible(false);

        return searchResults;

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

    public synchronized void cancel() {

        // Stops the search thread from running
        quitQueryThread = true;
        quitDownloadThread = true;

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

        loading.setVisible(false);
        loading.setProgress(0);

        searchesProgressText.setVisible(false);
        searchesProgressText.setText("Search Songs: 0%");

        footerMarker.setVisible(true);
        settingsLinkButton.setVisible(true);
        settingsLink.setVisible(true);

    }

    public synchronized void initializeDownload() throws IOException {

        ArrayList<String> request = searchResults.get(
                resultsData.indexOf(
                        resultsTable
                                .getItems()
                                .get(
                                    resultsTable
                                            .getSelectionModel()
                                            .getSelectedIndex()
                        )
                )
        );
        loading.setVisible(true);



        songsData = new ArrayList<>();
        metaData = new HashMap<>();
        String directoryName;

        if (request.get(4).equals("Album")) {
            // Prepare all songs

            // Producing the folder to save the data to
            directoryName = Utils.generateFolder(outputDirectorySetting + "\\" + request.get(0)); // Create new unique directory with album name

            // Meta data required is found in the search
            metaData.put("albumTitle", request.get(0));
            metaData.put("artist", request.get(1));
            metaData.put("year", request.get(2));
            metaData.put("genre", request.get(3));
            metaData.put("downloadRequestType", "album");
            metaData.put("directory", directoryName + "\\");

            // Downloading the album art
            Utils.downloadAlbumArt(directoryName + "\\", request.get(5));

            // Extracting all songs from album name & playtime
            Document albumData = Jsoup.connect(request.get(6)).get();
            Elements tracks = albumData.select("tr.track");

            for (Element track: tracks)
            {
                ArrayList<String> songDataToAdd = new ArrayList<>();
                songDataToAdd.add(track.select("div.title").text());
                songDataToAdd.add(Integer.toString(Utils.timeConversion(track.select("td.time").text())));
                songsData.add(songDataToAdd);
            }


        } else {
            // Prepare all songs and meta-data

            // Meta data must be extracted

            Document songDataRequest = Jsoup.connect(request.get(6)).get();

            String genre = "";

            try {
                genre = songDataRequest.selectFirst("div.song_genres").selectFirst("div.middle").select("a").text();
                genre = genre.split("\\(")[0];
                genre = genre.substring(0, genre.length() - 1);
            } catch (NullPointerException ignored) {}

            Document albumDataRequest = Jsoup.connect(songDataRequest.selectFirst("div.title").selectFirst("a").attr("href")).get();

            Elements tracks0 = albumDataRequest.select("tr.track");
            Elements tracks1 = albumDataRequest.select("tr.track pick");
            String positionInAlbum = "-1";

            for (Element track: tracks0)
            {
                if (track.selectFirst("div.title").selectFirst("a").text().equals(request.get(0)))
                {
                    positionInAlbum = track.selectFirst("td.tracknum").text();
                }
            }
            if (positionInAlbum.equals("-1")){
                for (Element track: tracks1)
                {
                    if (track.selectFirst("div.title").selectFirst("a").text().equals(request.get(0)))
                    {
                        positionInAlbum = track.selectFirst("td.tracknum").text();
                    }
                }
            }

            metaData.put("albumTitle", songDataRequest.selectFirst("div.title").select("a").text());
            metaData.put("artist", request.get(1));
            metaData.put("year", songDataRequest.selectFirst("td.year").text());
            metaData.put("genre", genre);
            metaData.put("positionInAlbum", positionInAlbum);
            metaData.put("directory", outputDirectorySetting + "\\");

            // Download album art
            Utils.downloadAlbumArt(outputDirectorySetting + "\\", songDataRequest.selectFirst("td.cover").select("img").attr("src"));

            // Generate song length and playtime
            ArrayList<String> songDataToAdd = new ArrayList<>();
            songDataToAdd.add(request.get(0));
            songDataToAdd.add(Integer.toString(Utils.timeConversion(songDataRequest.select("td.time").text())));
            songsData.add(songDataToAdd);

        }

        searchesProgressText.setVisible(true);
        timeRemainingLabel.setVisible(true);
        downloadSpeedLabel.setVisible(true);

        // Make Progress Bar Visible
        new downloadHandler();

        cancelButton.setText("Cancel");
        searchesProgressText.setText("0% Complete");
        downloadSpeedLabel.setText("Calculating...");
        timeRemainingLabel.setText("Calculating...");
        timeRemainingLabel.setTranslateX( (mainWindow.getWidth()/2) - (timeRemainingLabel.getWidth()/2) - 19.5);
        loading.setVisible(true);
        loading.setProgress(0);


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
        settingsLinkButton.setVisible(!settingVisibility);

        // Set setting elements visible
        settingsContainer.setVisible(settingVisibility);

        // Buttons
        outputDirectoryButton.setVisible(settingVisibility);
        confirmChanges.setVisible(settingVisibility);
        cancelBackButton.setVisible(settingVisibility);

    }

    public synchronized void settingsMode() {

        switchSettingVisibility(true);

        // Additional selection
        songDownloadFormatResult.getSelectionModel().select(musicFormatSetting);
        saveAlbumArtResult.getSelectionModel().select(saveAlbumArtSetting);

        albumArtSettingResult.getSelectionModel().select(applyAlbumArt ? 0 : 1);
        albumTitleSettingResult.getSelectionModel().select(applyAlbumTitle ? 0 : 1);
        songTitleSettingResult.getSelectionModel().select(applySongTitle ? 0 : 1);
        artistSettingResult.getSelectionModel().select(applyArtist ? 0 : 1);
        yearSettingResult.getSelectionModel().select(applyYear ? 0 : 1);
        trackNumberSettingResult.getSelectionModel().select(applyTrack ? 0 : 1);
        darkModeSettingResult.getSelectionModel().select(darkTheme ? 1 : 0);
        dataSaverSettingResult.getSelectionModel().select(dataSaver ? 0 : 1);

        setMetaDataVisibility();
        evaluateSettingsChanges();



        // Scheduling getting latest version, if data saver disabled
        if (!dataSaver) {
            new getLatestVersion();
        }
        new youtubeDlVerification();
        new ffmpegVerificationThread();

    }

    public synchronized void searchMode() {

        switchSettingVisibility(false);
        switchTheme(darkTheme);
    }

    public synchronized void submit() {

        // Saving to file
        settings.saveSettings(
                OutputDirectorySettingNew,
                songDownloadFormatResult
                        .getSelectionModel()
                        .selectedIndexProperty()
                        .getValue(),
                saveAlbumArtResult
                        .getSelectionModel()
                        .selectedIndexProperty()
                        .getValue(),
                Math.abs(
                        albumArtSettingResult
                            .getSelectionModel()
                            .selectedIndexProperty()
                            .getValue()
                        - 1
                ),
                Math.abs(
                        albumTitleSettingResult
                                .getSelectionModel()
                                .selectedIndexProperty()
                                .getValue()
                                - 1
                ),
                Math.abs(
                        songTitleSettingResult
                                .getSelectionModel()
                                .selectedIndexProperty()
                                .getValue()
                                - 1
                ),
                Math.abs(
                        artistSettingResult
                                .getSelectionModel()
                                .selectedIndexProperty()
                                .getValue()
                                - 1
                ),
                Math.abs(
                        yearSettingResult
                                .getSelectionModel()
                                .selectedIndexProperty()
                                .getValue()
                                - 1
                ),
                Math.abs(
                        trackNumberSettingResult
                                .getSelectionModel()
                                .selectedIndexProperty()
                                .getValue()
                                - 1
                ),
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
        applyAlbumArt = albumArtSettingResult
                .getSelectionModel()
                .selectedIndexProperty()
                .getValue() == 0;
        applyAlbumTitle = albumTitleSettingResult
                .getSelectionModel()
                .selectedIndexProperty()
                .getValue() == 0;
        applySongTitle = songTitleSettingResult
                .getSelectionModel()
                .selectedIndexProperty()
                .getValue() == 0;
        applyArtist = artistSettingResult
                .getSelectionModel()
                .selectedIndexProperty()
                .getValue() == 0;
        applyYear = yearSettingResult
                .getSelectionModel()
                .selectedIndexProperty()
                .getValue() == 0;
        applyTrack = trackNumberSettingResult
                .getSelectionModel()
                .selectedIndexProperty()
                .getValue() == 0;
        darkTheme = darkModeSettingResult
                .getSelectionModel()
                .selectedIndexProperty()
                .getValue() != 0;
        dataSaver = dataSaverSettingResult
                .getSelectionModel()
                .selectedIndexProperty()
                .getValue() == 0;

        evaluateSettingsChanges();
    }

    public synchronized void evaluateSettingsChanges() {

        if (
            musicFormatSetting != songDownloadFormatResult.getSelectionModel().getSelectedIndex() ||
            saveAlbumArtSetting != saveAlbumArtResult.getSelectionModel().getSelectedIndex() ||
            ((applyAlbumArt && albumArtSettingResult.getSelectionModel().getSelectedIndex() != 0) || (!applyAlbumArt && albumArtSettingResult.getSelectionModel().getSelectedIndex() == 0)) ||
            ((applyAlbumTitle && albumTitleSettingResult.getSelectionModel().getSelectedIndex() != 0) || (!applyAlbumTitle && albumTitleSettingResult.getSelectionModel().getSelectedIndex() == 0)) ||
            ((applyArtist && artistSettingResult.getSelectionModel().getSelectedIndex() != 0) || (!applyArtist && artistSettingResult.getSelectionModel().getSelectedIndex() == 0)) ||
            ((applySongTitle && songTitleSettingResult.getSelectionModel().getSelectedIndex() != 0) || (!applySongTitle && songTitleSettingResult.getSelectionModel().getSelectedIndex() == 0)) ||
            ((applyYear && yearSettingResult.getSelectionModel().getSelectedIndex() != 0) || (!applyYear && yearSettingResult.getSelectionModel().getSelectedIndex() == 0)) ||
            ((applyTrack && trackNumberSettingResult.getSelectionModel().getSelectedIndex() != 0) || (!applyTrack && trackNumberSettingResult.getSelectionModel().getSelectedIndex() == 0)) ||
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
                scene.getStylesheets().add(getClass().getResource("night_theme.css").toExternalForm());
                scene.getStylesheets().remove(getClass().getResource("normal_theme.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("normal_theme.css").toExternalForm());
                scene.getStylesheets().remove(getClass().getResource("night_theme.css").toExternalForm());
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
                autocompleteGenerator.setAutocompleteQuery(searchQuery);

            } else {

                autocompleteResultsTable.setVisible(false);
                autocompleteResultsTable.getItems().clear();

            }

        }

    }

    class generateAutocomplete implements Runnable {

        Thread t;
        String autocompleteQuery;
        boolean killRequest = false;

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
            requestThread.setSearchQuery(autocompleteQuery);

            while (true) {

                // Calling this too rapidly without delay eats up performance and won't work
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }

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
                                        queryResult.get(0).equals("Album") ? new ImageView(new Image(new File("resources/album_default.jpg").toURI().toString())) : new ImageView(new Image(new File("resources/song_default.png").toURI().toString())),
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

    static class autoCompleteWeb implements Runnable {

        Thread t;
        String searchQuery;
        ArrayList<ArrayList<String>> autocompleteResults = new ArrayList<>();

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

        public void run() {

            try {

                Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + searchQuery).get();
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
                e.printStackTrace();
            }

        }

    }

    class allMusicQuery implements Runnable {

        Thread t;
        allMusicQuery (){
            t = new Thread(this, "query");
            t.start();
        }

        public void run() {

            try {

                Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + searchRequest.getText()).get();
                searchResults =  Utils.allmusicQuery(doc);

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (searchResults.size() > 0) {

                try {

                    // Signal is always sent immediately when running, only want to kill other threads that are in loop, not this one

                    // Needs to check this won't result in future threads being killed
                    quitQueryThread = false;
                    resultsData = new ArrayList<>();
                    IntStream.range(0, searchResults.size()).forEach(i -> resultsData.add(null));

                    searchResultFullData = new Utils.resultsSet[searchResults.size()];

                    for (ArrayList<String> searchResult : searchResults) {

                        // Sending a new query requires quitting the old
                        if (quitQueryThread) {
                            quitQueryThread = false;
                            resultsTable.getItems().clear();
                            break;
                        }

                        addToTable tableAdder = new addToTable();
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
                    }

                    for (Utils.resultsSet result: searchResultFullData) {
                        resultsTable.getItems().add(result);
                    }

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

            } else {

                System.out.println("Invalid Search");

                // Display a info bit to the user here

                // Cannot just cancel or it will error due to thread differences

            }

        }

    }

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
                ImageView selectedImage;
                String year = searchResult.get(2);
                String genre = searchResult.get(3);
                if (searchResult.get(4).equals("Album")) {

                    if (dataSaver) {
                        selectedImage = new ImageView(new Image(new File("resources/album_default.jpg").toURI().toString()));
                    } else {
                        // This is an album request, year must be known or won't be found and hence doesn't require a check
                        if (searchResult.get(5).equals("")) {
                            selectedImage = new ImageView(new Image(new File("resources/album_default.jpg").toURI().toString()));
                        } else {
                            selectedImage = new ImageView(new Image(searchResult.get(5)));
                        }
                    }
                } else {

                    if (dataSaver) {
                        selectedImage = new ImageView(new Image(new File("resources/song_default.png").toURI().toString()));
                    } else {
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
                        } catch (NullPointerException ignored) {
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
                e.printStackTrace();
            }

        }

    }

    class downloadHandler implements Runnable {

        Thread t;

        downloadHandler() {
            t = new Thread(this, "downloader");
            t.start();
        }

        public void run() {

            // Await for the change and reposition download speed and time remaining

            int totalPlayTime = 0;
            for (ArrayList<String> song: songsData) {
                totalPlayTime += Integer.parseInt(song.get(1));
            }
            int totalPlayTimeRemaining = totalPlayTime;

            loadingPercent = 0;
            percentIncrease = ((double)1 / (double)songsData.size()) * (0.5518 * songsData.size() / (0.5518 * songsData.size() + totalPlayTime * 0.02313));

            for (ArrayList<String> songsDatum : songsData) {

                youtubeQueryThread searchThread = new youtubeQueryThread();
                searchThread.setSongsDatum(songsDatum);

            }

            long initialTime = Instant.now().toEpochMilli();

            while (true) {

                int completedThreads = 0;

                for (ArrayList<String> lengthCheck : songsData) {

                    if (lengthCheck.size() == 3) {
                        completedThreads++;
                    }

                }

                if (completedThreads == songsData.size()) {
                    break;
                }
            }

            loadingPercent = (0.5518 * songsData.size() / (0.5518 * songsData.size() + totalPlayTime * 0.02313));

            for (ArrayList<String> song: songsData)
            {

                if (quitDownloadThread) {
                    Debug.trace("Download thread quit signal received before downloading song " + songsData.indexOf(song) + " of " + songsData.size());
                    break;
                }

                //RemainingTime * ( (100 - startPercent) / (endPercent - startPercent) )
                prettyLoader = new loadingIncrementer();
                prettyLoader.initialize(
                        loadingPercent,
                        (
                                loadingPercent +
                                        (
                                                (double)Integer.parseInt(song.get(1)) / (double)totalPlayTime
                                        ) * (
                                                totalPlayTime * 0.02313
                                                        / (0.5518 * songsData.size() + totalPlayTime * 0.02313)
                                        )
                        ),
                        (
                                (
                                        totalPlayTimeRemaining - Integer.parseInt(song.get(1))
                                ) / (
                                        Integer.parseInt(song.get(1)) / ((double)(Instant.now().toEpochMilli() - initialTime) / 1000)
                                )
                        ) / (
                                (100 - (loadingPercent*100) ) / (
                                        (
                                                (
                                                        loadingPercent +
                                                                (
                                                                        (double) Integer.parseInt(song.get(1))
                                                                                / (double)totalPlayTime
                                                                )
                                                                        * (totalPlayTime * 0.02313 / (0.5518 * songsData.size() + totalPlayTime * 0.02313))
                                                ) * 100
                                        ) - (loadingPercent*100) )
                        )
                );

                try {
                    YoutubeDLRequest request = new YoutubeDLRequest(song.get(2), metaData.get("directory"));
                    request.setOption("extract-audio");
                    request.setOption("audio-format " + formatReferences.get(musicFormatSetting));
                    request.setOption("ignore-errors");
                    request.setOption("retries", 10);

                    YoutubeDL.execute(request);
                    
                    // We want the name of the file which is output to the current working directory in the format [YOUTUBE-TITLE]-[YOUTUBE-WATCH-ID]
                    File folder = new File(metaData.get("directory"));
                    File[] folderContents = folder.listFiles();
                    String targetFileName = "";

                    for (File file : Objects.requireNonNull(folderContents)) {
                        if (file.isFile()) {
                            if (file.getName().endsWith("-" + song.get(2).substring(32) + "." + formatReferences.get(musicFormatSetting))) {
                                targetFileName = file.getName();
                            }
                        }
                    }

                    // Now to apply the metadata
                    if (formatReferences.get(musicFormatSetting).equals("mp3")) {
                        Mp3File mp3Applicator = new Mp3File(metaData.get("directory") + targetFileName);

                        ID3v2 id3v2tag = new ID3v24Tag();
                        mp3Applicator.setId3v2Tag(id3v2tag);

                        if (applyAlbumArt) {

                            // Album Art Application
                            RandomAccessFile albumArtImg = new RandomAccessFile(metaData.get("directory") + "art.jpg", "r");

                            // Could break this up into mb loads
                            byte[] bytes;
                            bytes = new byte[(int) albumArtImg.length()];
                            albumArtImg.read(bytes);
                            albumArtImg.close();

                            id3v2tag.setAlbumImage(bytes, "image/jpg");
                        }

                        // Applying remaining data
                        if (applySongTitle)
                            id3v2tag.setTitle(song.get(0));

                        if (applyAlbumTitle)
                            id3v2tag.setAlbum(metaData.get("albumTitle"));

                        if (applyArtist) {
                            id3v2tag.setArtist(metaData.get("artist"));
                            id3v2tag.setAlbumArtist(metaData.get("artist"));
                        }

                        if (applyYear)
                            id3v2tag.setYear(metaData.get("year"));

                        if (metaData.containsKey("positionInAlbum")) {
                            id3v2tag.setTrack(metaData.get("positionInAlbum"));
                        } else {
                            id3v2tag.setTrack(Integer.toString(songsData.indexOf(song)));
                        }

                        try {
                            mp3Applicator.save(metaData.get("directory") + song.get(0) + ".mp3");
                        } catch (IOException | NotSupportedException e) {
                            e.printStackTrace();
                        }

                        File deletion = new File(metaData.get("directory") + targetFileName);
                        if (!deletion.delete()) {
                            Debug.trace("Failed to delete file: " + metaData.get("directory") + targetFileName);
                        }
                    } else {
                        File file = new File(outputDirectorySetting.equals("") ? metaData.get("directory") : outputDirectorySetting + "\\" + metaData.get("directory") + targetFileName);
                        if (!file.renameTo(new File(outputDirectorySetting.equals("") ? metaData.get("directory") : outputDirectorySetting + "\\" + metaData.get("directory") + song.get(0) + "." + formatReferences.get(musicFormatSetting))))
                            System.out.println("Failed to rename file to: " + (outputDirectorySetting.equals("") ? metaData.get("directory") : outputDirectorySetting + "\\" + metaData.get("directory") + song.get(0) + "." + formatReferences.get(musicFormatSetting)));
                    }

                    percentIncrease = ((double)Integer.parseInt(song.get(1)) / (double)totalPlayTime) * (totalPlayTime * 0.02313 / (0.5518 * songsData.size() + totalPlayTime * 0.02313));
                    loadingPercent += percentIncrease;
                    double tempLoadingPercent = loadingPercent;

                    prettyLoader.kill();
                    while (!prettyLoader.isDead());

                    Platform.runLater(() -> loading.setProgress(tempLoadingPercent));
                    Platform.runLater(() -> searchesProgressText.setText(Math.round(tempLoadingPercent*10000)/100 + "% Complete"));

                    // Remaining Playtime / Time Taken Per second to download last song = Estimated Time Remaining <variance for smaller songs
                    totalPlayTimeRemaining -= Integer.parseInt(song.get(1));

                    int finalTotalPlayTimeRemaining = totalPlayTimeRemaining;

                    try {
                        // Kill the existing timer and wait until it's dead before continuing
                        System.out.println("Waiting for thread to die");
                        countDown.kill();
                        while (!countDown.isDead());
                    } catch (NullPointerException ignored) {} // On first execution

                    countDown = new timerCountdown();
                    countDown.setTimeRemaining((int) Math.round(finalTotalPlayTimeRemaining / (Integer.parseInt(song.get(1)) / ((double)(Instant.now().toEpochMilli() - initialTime) / 1000))));

                    Platform.runLater(() -> timeRemainingLabel.setTranslateX( (mainWindow.getWidth()/2) - (timeRemainingLabel.getWidth()/2) -19.5));

                    // Download speed calculation Bytes per Second

                    double realDownloadSpeed = new File(metaData.get("directory") + song.get(0) + ".mp3").length() / ((double)(Instant.now().toEpochMilli() - initialTime) / 1000);
                    double prettyDownloadSpeed = 0;
                    String units = null;


                    if (realDownloadSpeed < 1024) {
                        units = "b";
                        prettyDownloadSpeed = Math.round(realDownloadSpeed);
                    } else if (realDownloadSpeed >= 1024 && downloadSpeed < 1024 * 1024) {
                        units = "kb";
                        prettyDownloadSpeed = Math.round(realDownloadSpeed / 1024);
                    } else if (realDownloadSpeed >= 1024 * 1024) {
                        units = "mb";
                        prettyDownloadSpeed = Math.round(realDownloadSpeed / (1024 * 1024));
                    }

                    double finalPrettyDownloadSpeed = prettyDownloadSpeed;
                    String finalUnits = units;
                    double originalWidth = downloadSpeedLabel.getWidth();
                    long preTime = Instant.now().toEpochMilli();

                    Platform.runLater(() -> downloadSpeedLabel.setText(finalPrettyDownloadSpeed + " " + finalUnits + "/s"));
                    while (downloadSpeedLabel.getWidth() == originalWidth && Instant.now().toEpochMilli() - preTime < 100);
                    Platform.runLater(() -> downloadSpeedLabel.setTranslateX(loading.getTranslateX() + 19.5 - downloadSpeedLabel.getWidth()));

                } catch (IOException | YoutubeDLException| InvalidDataException | UnsupportedTagException  e) {
                    e.printStackTrace();
                }

            }

            Platform.runLater(() -> {
                searchesProgressText.setText("Completed");
                searchesProgressText.setTextFill(Color.GREEN);
                timeRemainingLabel.setVisible(false);
                downloadSpeedLabel.setVisible(false);
            });

            if (saveAlbumArtSetting == 0 || (saveAlbumArtSetting == 1 && metaData.containsKey("positionInAlbum")) || (saveAlbumArtSetting == 2 && !metaData.containsKey("positionInAlbum"))) {
                try {
                    Files.delete(Paths.get(metaData.get("directory") + "\\art.jpg"));
                } catch (IOException e) {
                    Debug.error("Failed to delete file: " + metaData.get("directory") + "\\art.jpg");
                }
            }

            Platform.runLater(() -> cancelButton.setText("Back"));

            if (quitDownloadThread) {
                quitDownloadThread = false;

                if (metaData.containsKey("positionInAlbum")) {
                    // Deleting a song, just name and album art

                    // Delete album art, can't delete song
                    try {
                        Files.delete(Paths.get(outputDirectorySetting.equals("") ? metaData.get("directory") + "/art.jpg" : outputDirectorySetting + "/art.jpg"));
                    } catch (IOException e) {
                        System.out.println(outputDirectorySetting.equals("") ? metaData.get("directory") + "/art.jpg" : outputDirectorySetting + "/art.jpg");
                        e.printStackTrace();
                    }
                } else {
                    // Deleting an album
                    File albumFolder = new File(metaData.get("directory"));
                    String[] files = albumFolder.list();
                    for (String file : Objects.requireNonNull(files)) {
                        File current = new File(albumFolder.getPath(), file);
                        if (!current.delete()) {
                            System.out.println("Failed to delete: " + current.getAbsolutePath());
                        }
                    }
                    if (!albumFolder.delete())
                        System.out.println("Failed to delete: " + albumFolder.getAbsolutePath());
                }

            }

        }
    }

    class youtubeQueryThread implements Runnable {

        Thread t;
        ArrayList<String> songsDatum;

        youtubeQueryThread() {
            t = new Thread(this, "youtube-query");
            t.start();
        }

        public void setSongsDatum(ArrayList<String> set) {
            songsDatum = set;
        }

        public void run() {

            try {
                songsDatum.add(
                        Utils.evaluateBestLink(
                                Utils.youtubeQuery(
                                        metaData.get("artist") + " " + songsDatum.get(0)),
                                        Integer.parseInt(songsDatum.get(1))
                        )
                );
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

            loadingPercent += percentIncrease;
            double tempLoadingPercent = loadingPercent;
            Platform.runLater(() -> searchesProgressText.setText(Math.round(tempLoadingPercent*10000)/100 + "% Complete"));
            Platform.runLater(() -> loading.setProgress(tempLoadingPercent));
        }

    }
    
    class getLatestVersion implements Runnable {

        Thread t;
        getLatestVersion (){
            t = new Thread(this, "get-latest-version");
            t.start();
        }

        public void run() {

            Settings settings = new Settings();
            double originalWidth = latestVersionResult.getWidth();

            Platform.runLater(() -> latestVersionResult.setText(settings.getLatestVersion()));
            while (latestVersionResult.getWidth() == originalWidth) { try {Thread.sleep(10);} catch (InterruptedException ignored) {} }
            Platform.runLater(() -> latestVersionResultContainer.setPadding(new Insets(70, 0, 0, -latestVersionResult.getWidth())));

        }

    }

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

                // Update the button click
                originalWidth = outputDirectoryButton.getWidth();
                Platform.runLater(() -> outputDirectoryButton.setPrefWidth(outputDirectoryResult.getWidth()));
                while (originalWidth == outputDirectoryButton.getWidth());
                Platform.runLater(() -> outputDirectoryButtonContainer.setPadding(new Insets(180, 0, 0, -outputDirectoryResult.getWidth())));

                // Save the settings
                outputDirectorySetting = OutputDirectorySettingNew;
                submit();

            } catch (Exception ignored) {}

        }
    }

    class smartQuitDownload implements  Runnable {
        Thread t;

        smartQuitDownload () {
            t = new Thread(this, "smart-quit");
            t.start();
        }

        public void run() {

            // Initial where window doesn't exist
            while (true) {
                try {

                    Thread.sleep(50);

                    // Temporarily false as things load in
                    if (mainWindow.isShowing())
                        break;
                } catch (NullPointerException | InterruptedException ignored) {}
            }

            // Keep in background until the window is closed
            while (mainWindow.isShowing());
            Debug.trace("Detected application is closed, killing running threads.");

            // Quit running threads, download is important query is mostly for performance
            quitDownloadThread = true;
            quitQueryThread = true;

        }
    }

    class youtubeDlVerification implements Runnable {
        Thread t;

        youtubeDlVerification () {
            t = new Thread(this, "youtube-dl-verification");
            t.start();
        }

        public void run() {

            boolean youtubeDlStatus = Settings.checkYouTubeDl();
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

    class ffmpegVerificationThread implements Runnable {

        Thread t;

        ffmpegVerificationThread () {
            t = new Thread(this, "ffmpeg-verification");
            t.start();
        }

        public void run() {

            boolean ffmpegStatus = Settings.checkFFMPEG();
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

    class timerCountdown implements Runnable {

        // Consider looking into moving into ScheduledExecutorService
        // https://stackoverflow.com/questions/54394042/java-how-to-avoid-using-thread-sleep-in-a-loop

        Thread t;
        int timeRemaining;
        boolean killSignal = false;
        boolean dead = false;

        timerCountdown (){
            t = new Thread(this, "timer-countdown");
            t.start();
        }

        public void setTimeRemaining(int newTime) {
            timeRemaining = newTime;
        }

        public boolean isDead() {
            return dead;
        }

        public void kill() {
            killSignal = true;
        }

        public void run(){

            // Count down from time remaining
            for (; timeRemaining > 0; timeRemaining--) {

                if (killSignal)
                    break;

                try {
                    Thread.sleep(1000);

                    Platform.runLater(
                            () -> timeRemainingLabel.setText(
                                    Duration.ofSeconds(timeRemaining)
                                    .toString()
                                            .substring(2)
                                            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                                            .toLowerCase()
                                    + " Remaining"
                            )
                    );

                } catch (InterruptedException ignored) {}

            }

            dead = true;

        }

    }

    class loadingIncrementer implements Runnable {

        Thread t;
        boolean killSignal = false;
        boolean dead = false;
        double startPercentage;
        double currentPercentage;
        double targetPercentage;
        int time;

        public loadingIncrementer() {
            t = new Thread(this, "loading-incrementor");
            t.start();
        }

        public void initialize(double start, double end, double t) {
            startPercentage = start;
            currentPercentage = startPercentage;
            targetPercentage = end;
            time = Math.toIntExact(Math.round(t));
        }

        public boolean isDead() {
            return dead;
        }

        public void kill() {
            killSignal = true;
        }

        public void run() {

            double increment = (targetPercentage - startPercentage) / time;

            // For each percentage wait the time and increment the loading bar
            for (; time > 0; time--) {

                if (killSignal)
                    break;

                try {Thread.sleep(1000);} catch (InterruptedException ignored) {}

                currentPercentage += increment;

                Platform.runLater(() -> loading.setProgress(currentPercentage));

            }

            dead = true;

        }

    }
}