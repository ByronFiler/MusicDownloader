package sample;

import com.mpatric.mp3agic.*;
import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

// TODO: Fix dark theme, should set text colours properly and the results table
// TODO: Add button to install and configure youtube-dl & ffmpeg
// TODO: Look if I can speed up search by sending all jpeg & download requests simultaneously
// TODO: Fix errors with changing CSS
// TODO: Move CSS Files somewhere else
// TODO: Rewrite Main.css and redesign general look of the application

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
    public ArrayList<ArrayList<String>> songsData;
    public ArrayList<Utils.resultsSet> resultsData;
    public HashMap<String, String> metaData;

    public ArrayList<String> formatReferences = new ArrayList<>(Arrays.asList("mp3", "wav", "ogg", "aac"));

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

        final InvalidationListener resizeListener = observable -> {
            mainWindow = window;
            restructureElements(window.getWidth(), window.getHeight());
        };
        window.widthProperty().addListener(resizeListener);
        window.heightProperty().addListener(resizeListener);

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

        footerMarker = new Line(0, 0, 0, 0);
        footerMarker.setId("line");

        settingsLink = new Label("Settings");
        settingsLink.setId("subTitle2");

        settingsLinkButton = new Button();
        settingsLinkButton.setPrefSize(100, 25);
        settingsLinkButton.setId("button");
        settingsLinkButton.setOpacity(0);
        settingsLinkButton.setOnAction(e -> settingsMode());

        /* Search Results Page */
        searchResultsTitle = new Label("Search Results");
        searchResultsTitle.setId("subTitle");
        searchResultsTitle.setVisible(false);

        resultsTable = new TableView<PropertyValueFactory<TableColumn<String, Utils.resultsSet>, Utils.resultsSet>>();
        resultsTable.setId("table");
        resultsTable.getSelectionModel().selectedIndexProperty().addListener((obs, oldSelection, newSelection) -> downloadButton.setDisable(newSelection == null));
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

        downloadButton = new Button("Download");
        downloadButton.setPrefSize(120, 40);
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
        searchesProgressText.setVisible(false);

        loading = new ProgressBar();
        loading.setProgress(0);
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
        if (dataSaver) {
            // Calls it once on program startup, instead of each time settings is accessed
            new youtubeDlVerification();
        }

        ffmpegVerification = new Label("FFMPEG Status: ");
        ffmpegVerification.setId("settingInfo");

        ffmpegVerificationResult = new Label("Checking...");
        ffmpegVerificationResult.setId("settingInfo");
        if (dataSaver) {
            new ffmpegVerificationThread();
        }

        fileSettingsTitle = new Label("Files");
        fileSettingsTitle.setId("settingsHeader");

        outputDirectory = new Label("Save music to: ");
        outputDirectory.setId("settingInfo");

        outputDirectoryResult = new Label(outputDirectorySetting.equals("") ? System.getProperty("user.dir") : outputDirectorySetting);
        outputDirectoryResult.setId("settingInfo");

        outputDirectoryButton = new Button();
        outputDirectoryButton.setId("button");
        outputDirectoryButton.setOpacity(0);
        outputDirectoryButton.setOnAction(e -> new selectFolder());

        songDownloadFormat = new Label("Music format: ");
        songDownloadFormat.setId("settingInfo");

        songDownloadFormatResult = new ComboBox<>(FXCollections.observableArrayList("mp3", "wav", "ogg", "aac"));
        songDownloadFormatResult.setOnAction(e -> {setMetaDataVisibility(); evaluateSettingsChanges();} );

        saveAlbumArt = new Label("Save Album Art: ");
        saveAlbumArt.setId("settingInfo");

        saveAlbumArtResult = new ComboBox<>(FXCollections.observableArrayList("No", "Albums Only", "Songs Only", "All"));
        saveAlbumArtResult.setOnAction(e -> evaluateSettingsChanges());

        metaDataTitle = new Label("Meta-Data Application");
        metaDataTitle.setId("settingsHeader");

        albumArtSetting = new Label("Album Art: ");
        albumArtSetting.setId("settingInfo");

        albumArtSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        albumArtSettingResult.setOnAction(e -> evaluateSettingsChanges());

        albumTitleSetting = new Label("Album Title: ");
        albumTitleSetting.setId("settingInfo");

        albumTitleSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        albumTitleSettingResult.setOnAction(e -> evaluateSettingsChanges());

        songTitleSetting = new Label("Song Title: ");
        songTitleSetting.setId("settingInfo");

        songTitleSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        songTitleSettingResult.setOnAction(e -> evaluateSettingsChanges());

        artistSetting = new Label("Artist: ");
        artistSetting.setId("settingInfo");

        artistSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        artistSettingResult.setOnAction(e -> evaluateSettingsChanges());

        yearSetting = new Label("Year: ");
        yearSetting.setId("settingInfo");

        yearSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        yearSettingResult.setOnAction(e -> evaluateSettingsChanges());

        trackNumberSetting = new Label("Track Number: ");
        trackNumberSetting.setId("settingInfo");

        trackNumberSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        trackNumberSettingResult.setOnAction(e -> evaluateSettingsChanges());
        trackNumberSettingResult.setTranslateY(480);

        applicationSettingTitle = new Label("Application Configuration");
        applicationSettingTitle.setId("settingsHeader");

        darkModeSetting = new Label("Theme: ");
        darkModeSetting.setId("settingInfo");

        darkModeSettingResult = new ComboBox<>(FXCollections.observableArrayList("Normal", "Night"));
        darkModeSettingResult.setOnAction(e -> {evaluateSettingsChanges(); switchTheme(darkModeSettingResult.getSelectionModel().getSelectedIndex() == 1);});

        dataSaverSetting = new Label("Data Saver Mode: ");
        dataSaverSetting.setId("settingInfo");

        dataSaverSettingResult = new ComboBox<>(FXCollections.observableArrayList("Enabled", "Disabled"));
        dataSaverSettingResult.setOnAction(e -> evaluateSettingsChanges());

        confirmChanges = new Button("Confirm");
        confirmChanges.setOnAction(e -> submit());
        confirmChanges.setId("confirm_button");
        confirmChanges.setPrefSize(100, 50);
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
        latestVersionResultContainer.getChildren().add(latestVersionResult);;

        // Program Settings Data: YoutubeDlVerification
        youtubeDlVerificationResultContainer = new VBox();
        youtubeDlVerificationResultContainer.getChildren().add(youtubeDlVerificationResult);

        // Program Settings Data: ffmpegVerification
        ffmpegVerificationResultContainer = new VBox();
        ffmpegVerificationResultContainer.getChildren().add(ffmpegVerificationResult);

        // File Settings Title & Line
        VBox fileSettingNameContainer = new VBox();
        fileSettingNameContainer.getChildren().addAll(
                fileSettingsTitle,
                fileSettingsTitleLine
        );
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
        settingsContainer.setVisible(false);

        // Search Page
        pane.getChildren().addAll(
                title,
                searchRequest,
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
                searchesProgressText
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

        downloadButton.setDisable(true);
        resultsTable.setVisible(true);
        searchResultsTitle.setVisible(true);
        downloadButton.setVisible(true);
        cancelButton.setVisible(true);

        searchRequest.setVisible(false);
        title.setVisible(false);
        footerMarker.setVisible(false);
        settingsLinkButton.setVisible(false);
        settingsLink.setVisible(false);

        restructureElements(mainWindow.getWidth(), mainWindow.getHeight());

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

        restructureElements(mainWindow.getWidth(), mainWindow.getHeight());

    }

    public synchronized void initializeDownload() throws IOException {

        ArrayList<String> request = searchResults.get(
                resultsData.indexOf(
                        resultsTable
                                .getItems().get(
                                    resultsTable
                                            .getSelectionModel()
                                            .getSelectedIndex()
                        )
                )
        );
        loading.setVisible(true);
        restructureElements(mainWindow.getWidth(), mainWindow.getHeight());

        songsData = new ArrayList<>();
        metaData = new HashMap<>();
        String directoryName = "";

        if (request.get(4).equals("Album")) {
            // Prepare all songs

            // Producing the folder to save the data to
            directoryName = Utils.generateFolder(request.get(0)); // Create new unique directory with album name

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
            metaData.put("directory", System.getProperty("user.dir") + "\\");

            // Folder link is not as song will be place in CWD

            // Download album art
            Utils.downloadAlbumArt(directoryName, songDataRequest.selectFirst("td.cover").select("img").attr("src"));

            // Generate song length and playtime
            ArrayList<String> songDataToAdd = new ArrayList<>();
            songDataToAdd.add(request.get(0));
            songDataToAdd.add(Integer.toString(Utils.timeConversion(songDataRequest.select("td.time").text())));
            songsData.add(songDataToAdd);

        }

        searchesProgressText.setVisible(true);

        // Make Progress Bar Visible
        new downloadHandler();


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
        settingsTitle.setVisible(settingVisibility);

        // Program
        programSettingsTitle.setVisible(settingVisibility);
        latestVersion.setVisible(settingVisibility);
        latestVersionResult.setVisible(settingVisibility);
        version.setVisible(settingVisibility);
        versionResult.setVisible(settingVisibility);
        youtubeDlVerification.setVisible(settingVisibility);
        youtubeDlVerificationResult.setVisible(settingVisibility);

        // File
        fileSettingsTitle.setVisible(settingVisibility);
        outputDirectory.setVisible(settingVisibility);
        outputDirectoryResult.setVisible(settingVisibility);
        songDownloadFormat.setVisible(settingVisibility);
        songDownloadFormatResult.setVisible(settingVisibility);
        saveAlbumArt.setVisible(settingVisibility);
        saveAlbumArtResult.setVisible(settingVisibility);

        // Metadata
        metaDataTitle.setVisible(settingVisibility);
        albumArtSetting.setVisible(settingVisibility);
        albumArtSettingResult.setVisible(settingVisibility);
        albumTitleSetting.setVisible(settingVisibility);
        albumTitleSettingResult.setVisible(settingVisibility);
        songTitleSetting.setVisible(settingVisibility);
        songTitleSettingResult.setVisible(settingVisibility);
        artistSetting.setVisible(settingVisibility);
        artistSettingResult.setVisible(settingVisibility);
        yearSetting.setVisible(settingVisibility);
        yearSettingResult.setVisible(settingVisibility);
        trackNumberSetting.setVisible(settingVisibility);
        trackNumberSettingResult.setVisible(settingVisibility);

        // Application
        applicationSettingTitle.setVisible(settingVisibility);
        darkModeSetting.setVisible(settingVisibility);
        darkModeSettingResult.setVisible(settingVisibility);
        dataSaverSetting.setVisible(settingVisibility);
        dataSaverSettingResult.setVisible(settingVisibility);

        // Buttons
        outputDirectoryButton.setVisible(settingVisibility);
        confirmChanges.setVisible(settingVisibility);
        cancelBackButton.setVisible(settingVisibility);

        // Lines
        settingTitleLine.setVisible(settingVisibility);
        programSettingsTitleLine.setVisible(settingVisibility);
        fileSettingsTitleLine.setVisible(settingVisibility);
        metaDataTitleLine.setVisible(settingVisibility);
        applicationSettingTitleLine.setVisible(settingVisibility);

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

        restructureElements(mainWindow.getWidth(), mainWindow.getHeight());

        // Scheduling getting latest version, if data saver disabled
        if (!dataSaver) {
            new getLatestVersion();
            new youtubeDlVerification();
            new ffmpegVerificationThread();
        }

    }

    public synchronized void searchMode() {

        switchSettingVisibility(false);
        restructureElements(mainWindow.getWidth(), mainWindow.getHeight());
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

    public synchronized void restructureElements(double width, double height) {

        // Detect the mode
        if (title.isVisible()) {

            // Default search mode
            title.setTranslateX(width/2 - title.getWidth()/2);
            title.setTranslateY(height/2 - 119.5);

            searchRequest.setTranslateX(width/2 - searchRequest.getWidth()/2);
            searchRequest.setTranslateY(height/2 - 79.5);

            footerMarker.setStartX(0);
            footerMarker.setEndX(width);
            footerMarker.setStartY(height-50 - 39);
            footerMarker.setEndY(height-50 - 39);

            settingsLink.setTranslateX(10);
            settingsLink.setTranslateY(height - 40 - 39);

            settingsLinkButton.setTranslateX(10);
            settingsLinkButton.setTranslateY(height - 40 - 39);
            settingsLinkButton.setPrefSize(settingsLink.getWidth(), 25);

        } else if (settingsTitle.isVisible()) {

            // Settings mode
            settingsContainer.setPrefSize(width - 78 + 5, height - 20 - (height - (height- confirmChanges.getHeight() - 25 - 39)) - 20);

            settingsContainer.setTranslateX(30);
            settingsContainer.setTranslateY(20);

            versionResultContainer.setPadding(new Insets(50, 0, 0, -versionResult.getWidth()));
            latestVersionResultContainer.setPadding(new Insets(70, 0, 0, -latestVersionResult.getWidth()));
            youtubeDlVerificationResultContainer.setPadding(new Insets(90, 0, 0, -youtubeDlVerificationResult.getWidth()));
            ffmpegVerificationResultContainer.setPadding(new Insets(110, 0, 0, -ffmpegVerificationResult.getWidth()));

            outputDirectoryResultContainer.setPadding(new Insets(180, 0, 0, -outputDirectoryResult.getWidth()));
            outputDirectoryButtonContainer.setPadding(new Insets(180, 0, 0, -outputDirectoryResult.getWidth()));
            songDownloadFormatResultContainer.setPadding(new Insets(205, 0, 0, -songDownloadFormatResult.getWidth()));
            saveAlbumArtResultContainer.setPadding(new Insets(230, 0, 0, -saveAlbumArtResult.getWidth()));

            albumArtSettingResultContainer.setPadding(new Insets(300, 0, 0, -albumArtSettingResult.getWidth()));
            albumTitleSettingResultContainer.setPadding(new Insets(325, 0, 0, -albumTitleSettingResult.getWidth()));
            songTitleSettingResultContainer.setPadding(new Insets(350, 0, 0, -songTitleSettingResult.getWidth()));
            artistSettingResultContainer.setPadding(new Insets(375, 0, 0, -artistSettingResult.getWidth()));
            yearSettingResultContainer.setPadding(new Insets(400, 0, 0, -yearSettingResult.getWidth()));
            trackNumberSettingResultContainer.setPadding(new Insets(-55, 0, 0, -trackNumberSettingResult.getWidth())); // Strange Top Position, 480 instead of 0

            darkModeSettingResultContainer.setPadding(new Insets(500, 0, 0, -darkModeSettingResult.getWidth()));
            dataSaverSettingResultContainer.setPadding(new Insets(525, 0, 0, -dataSaverSettingResult.getWidth()));

            // Lines
            settingTitleLine.setEndX(width-30-19.5-50);
            programSettingsTitleLine.setEndX(width-30-19.5-50);
            fileSettingsTitleLine.setEndX(width-30-19.5-50);
            metaDataTitleLine.setEndX(width-30-19.5-50);
            applicationSettingTitleLine.setEndX(width-30-19.5-50);

            // Buttons
            confirmChanges.setTranslateY(height- confirmChanges.getHeight() - 25 - 39);
            confirmChanges.setTranslateX(30);
            cancelBackButton.setTranslateY(height- cancelBackButton.getHeight() - 25 - 39);
            cancelBackButton.setTranslateX(width - 19 - 30 +5 - cancelBackButton.getWidth());
            outputDirectoryButton.setPrefSize(outputDirectoryResult.getWidth(), 25);

        } else if (resultsTable.isVisible()) {

            // Search mode
            searchResultsTitle.setTranslateX(50);
            searchResultsTitle.setTranslateY(10);

            resultsTable.setTranslateX(50);
            resultsTable.setTranslateY(50);
            resultsTable.setPrefWidth(width - 100 - 19.5);
            resultsTable.setPrefHeight(height - 30 - 200);

            downloadButton.setTranslateX(50);
            downloadButton.setTranslateY(height - 30 - 140);
            cancelButton.setTranslateX(width - 19.5 - 50 - cancelButton.getWidth());
            cancelButton.setTranslateY(height - 30 - 140);

            if (loading.isVisible()) {
                // Loading data should be restructured too
                searchesProgressText.setTranslateY(height - 30 - 85);
                searchesProgressText.setTranslateX(50);

                loading.setTranslateX(50);
                loading.setPrefWidth(resultsTable.getWidth());
                loading.setPrefHeight(20);
                loading.setTranslateY(height - 30 - 65);
            }
        }

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

    class allMusicQuery implements Runnable {

        Thread t;
        allMusicQuery (){
            t = new Thread(this, "query");
            t.start();
        }

        public void run() {

            try {
                searchResults =  Utils.allmusicQuery(searchRequest.getText());

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (searchResults.size() > 0) {

                try {

                    // Signal is always sent immediately when running, only want to kill other threads that are in loop, not this one

                    // Needs to check this won't result in future threads being killed
                    quitQueryThread = false;
                    resultsData = new ArrayList<>();
                    for (ArrayList<String> searchResult : searchResults) {

                        // Sending a new query requires quitting the old
                        if (quitQueryThread) {
                            quitQueryThread = false;
                            resultsTable.getItems().clear();
                            break;
                        }

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
                        resultsTable.getItems().add(results);
                        resultsData.add(results);
                    }
                } catch (NullPointerException | IOException e) {
                    e.printStackTrace();
                }

            } else {

                System.out.println("Invalid Search");

                // Display a info bit to the user here

                // Cannot just cancel or it will error due to thread differences

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

            Platform.runLater(() -> cancelButton.setText("Cancel"));
            Platform.runLater(() -> searchesProgressText.setText("0% Complete"));
            Platform.runLater(() -> loading.setVisible(true));
            Platform.runLater(() -> loading.setProgress(0));

            int totalPlayTime = 0;
            for (ArrayList<String> song: songsData) {
                totalPlayTime += Integer.parseInt(song.get(1));
            }

            double loadingPercent = 0;
            double percentIncrease = ((double)1 / (double)songsData.size()) * (0.5518 * songsData.size() / (0.5518 * songsData.size() + totalPlayTime * 0.02313));

            for (ArrayList<String> songsDatum : songsData) {

                try {
                    songsDatum.add(Utils.evaluateBestLink(Utils.youtubeQuery(metaData.get("artist") + " " + songsDatum.get(0)), Integer.parseInt(songsDatum.get(1))));
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }

                loadingPercent += percentIncrease;
                double tempLoadingPercent = loadingPercent;
                Platform.runLater(() -> searchesProgressText.setText(Math.round(tempLoadingPercent*10000)/100 + "% Complete"));
                Platform.runLater(() -> loading.setProgress(tempLoadingPercent));

            }

            loadingPercent = (0.5518 * songsData.size() / (0.5518 * songsData.size() + totalPlayTime * 0.02313));

            for (ArrayList<String> song: songsData)
            {

                if (quitDownloadThread) {
                    break;
                }

                try {

                    YoutubeDLRequest request = new YoutubeDLRequest(song.get(2),outputDirectorySetting.equals("") ? metaData.get("directory") : outputDirectorySetting + "\\" + metaData.get("directory"));
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

                    Platform.runLater(() -> loading.setProgress(tempLoadingPercent));
                    Platform.runLater(() -> searchesProgressText.setText(Math.round(tempLoadingPercent*10000)/100 + "% Complete"));

                } catch (IOException | YoutubeDLException| InvalidDataException | UnsupportedTagException  e) {
                    e.printStackTrace();
                }

            }

            if (saveAlbumArtSetting == 0 || (saveAlbumArtSetting == 1 && metaData.containsKey("positionInAlbum")) || (saveAlbumArtSetting == 2 && !metaData.containsKey("positionInAlbum"))) {
                File deletion = new File(outputDirectorySetting.equals("") ? metaData.get("directory") : outputDirectorySetting + "\\" + "art.jpg");
                if (!deletion.delete()) {
                    Debug.trace("Failed to delete file: " + (outputDirectorySetting.equals("") ? metaData.get("directory") : outputDirectorySetting) + "\\" + "art.jpg");
                }
            }

            Platform.runLater(() -> cancelButton.setText("Back"));

            if (quitDownloadThread) {
                quitDownloadThread = false;

                if (metaData.containsKey("positionInAlbum")) {
                    // Deleting a song, just name and album art

                    // Delete album art, can't delete song
                    File albumArt = new File(outputDirectorySetting.equals("") ? metaData.get("directory") : outputDirectorySetting + "\\" + "art.jpg");
                    if (!albumArt.delete())
                        System.out.println("Failed to delete: " + albumArt.getAbsolutePath());

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
            while (latestVersionResult.getWidth() == originalWidth) {} // Ugly fix to an ugly fix?
            Platform.runLater(() -> restructureElements(mainWindow.getWidth(), mainWindow.getHeight()));

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

            if (OutputDirectorySettingNew.hashCode() == 0) {
                OutputDirectorySettingNew = outputDirectorySetting;
                return;
            }

            // Change this to checking the original width and wait until it's different from new

            Platform.runLater(() -> outputDirectoryResult.setText(OutputDirectorySettingNew));
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> outputDirectoryResult.setTranslateX(600 - 30 - outputDirectoryResult.getWidth()));

            Platform.runLater(() -> outputDirectoryButton.setTranslateX(600 - 30 - outputDirectoryResult.getWidth()));
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> outputDirectoryButton.setPrefSize(outputDirectoryResult.getWidth(), 25));

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
                    mainWindow.isShowing();
                    break;
                } catch (NullPointerException ignored) {}
            }

            // Keep in background until the window is closed
            while (mainWindow.isShowing()) { }

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
            Platform.runLater(() -> restructureElements(mainWindow.getWidth(), mainWindow.getHeight()));

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

            while (ffmpegVerificationResult.getWidth() == originalWidth) {}
            Platform.runLater(() -> restructureElements(mainWindow.getWidth(), mainWindow.getHeight()));

        }

    }
}