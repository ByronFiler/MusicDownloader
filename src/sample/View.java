package sample;

import com.mpatric.mp3agic.*;
import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
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
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;

public class View implements EventHandler<KeyEvent>
{
    public Controller controller;
    public Settings settings;

    public int width;
    public int height;

    public Pane pane;
    public Canvas canvas;

    public String outputDirectorySetting;
    public String OutputDirectorySettingNew;
    public int musicFormatSetting;
    public int saveAlbumArtSetting;

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
    public Label settingsTitle;

    // Program
    public Label programSettingsTitle;
    public Label version;
    public Label versionResult;
    public Label latestVersion;
    public Label latestVersionResult;
    public Label youtubeDlVerification;
    public Label youtubeDlVerificationResult;

    // File
    public Label fileSettingsTitle;
    public Label outputDirectory;
    public Label outputDirectoryResult;
    public Button outputDirectoryButton;
    public Label songDownloadFormat;
    public ComboBox songDownloadFormatResult;
    public Label saveAlbumArt;
    public ComboBox saveAlbumArtResult;

    // Buttons
    public Button confirmChanges;
    public Button cancelBackButton;

    // Settings Lines
    public Line settingTitleSubline;
    public Line programSettingsTitleSubline;
    public Line fileSettingsTitleSubline;

    public ArrayList<ArrayList<String>> searchResults;
    public ArrayList<ArrayList<String>> songsData;
    public HashMap<String, String> metaData;

    public ArrayList<String> formatReferences = new ArrayList<>(Arrays.asList("mp3", "wav", "ogg", "aac"));

    public boolean quitQueryThread = false;

    public View(int w, int h) {
        Debug.trace("View::<constructor>");
        width = w;
        height = h;
    }

    public void start(Stage window) {
        pane = new Pane();
        pane.setId("initial");

        canvas = new Canvas(width, height);
        pane.getChildren().add(canvas);

        settings = new Settings();
        JSONObject config = settings.getSettings();

        settings.checkYouTubeDl();

        outputDirectorySetting = (String) config.get("output_directory");
        OutputDirectorySettingNew = outputDirectorySetting;
        musicFormatSetting = Integer.parseInt(Long.toString((Long) config.get("music_format")));
        saveAlbumArtSetting = Integer.parseInt(Long.toString((Long) config.get("save_album_art")));

        programVersion = settings.getVersion();

        title = new Label("Music Downloader");
        title.setTextFill(Color.BLACK);
        title.setStyle("-fx-font: 32 arial;");
        title.setTranslateY(300);
        title.setTranslateX(170);

        searchResultsTitle = new Label("Search Results");
        searchResultsTitle.setTextFill(Color.BLACK);
        searchResultsTitle.setStyle("-fx-font: 28 arial;");
        searchResultsTitle.setVisible(false);

        searchRequest = new TextField();
        searchRequest.setTranslateX(220);
        searchRequest.setTranslateY(340);

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

        cancelButton = new Button("Cancel");
        cancelButton.setPrefSize(120, 40);
        cancelButton.setOnAction(
                e -> cancel()
        );
        cancelButton.setVisible(false);

        searchesProgressText = new Label("Locating Songs: 0%");
        searchesProgressText.setTranslateY(510);
        searchesProgressText.setTranslateX(60);
        searchesProgressText.setVisible(false);

        loading = new ProgressBar();
        loading.setProgress(0);
        loading.setTranslateX(60);
        loading.setPrefWidth(480);
        loading.setPrefHeight(20);
        loading.setTranslateY(530);
        loading.setVisible(false);

        footerMarker = new Line(0, 800-50, 600, 800-50);

        // Create a box here surrounding it, invisible to handle clicks in a wider area
        settingsLink = new Label("Settings");
        settingsLink.setStyle("-fx-font: 24 arial;");
        settingsLink.setTranslateY(800 - 40);
        settingsLink.setTranslateX(10);

        settingsLinkButton = new Button();
        settingsLinkButton.setPrefSize(100, 25);
        settingsLinkButton.setTranslateX(10);
        settingsLinkButton.setTranslateY(800 - 35);
        settingsLinkButton.setStyle("-fx-cursor: hand;");
        settingsLinkButton.setOpacity(0);
        settingsLinkButton.setOnAction(
                e -> settingsMode()
        );

        resultsTable = new TableView();
        resultsTable.getSelectionModel().selectedIndexProperty().addListener((obs, oldSelection, newSelection) -> downloadButton.setDisable(newSelection == null));
        resultsTable.setVisible(false);

        TableColumn<String, Utils.resultsSet> albumArtColumn = new TableColumn<>("Album Art");
        albumArtColumn.setCellValueFactory(new PropertyValueFactory<>("albumArt"));

        TableColumn<String, Utils.resultsSet> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<String, Utils.resultsSet> artistColumn = new TableColumn<>("Artist");
        artistColumn.setCellValueFactory(new PropertyValueFactory<>("artist"));

        TableColumn<String, Utils.resultsSet> yearColumn = new TableColumn<>("Year");
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));

        TableColumn<String, Utils.resultsSet> genreColumn = new TableColumn<>("Genre");
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));

        TableColumn<String, Utils.resultsSet> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

        resultsTable.getColumns().add(albumArtColumn);
        resultsTable.getColumns().add(titleColumn);
        resultsTable.getColumns().add(artistColumn);
        resultsTable.getColumns().add(yearColumn);
        resultsTable.getColumns().add(genreColumn);
        resultsTable.getColumns().add(typeColumn);

        // Settings
        settingsTitle = new Label("Settings");
        settingsTitle.setStyle("-fx-font: 28 arial;");
        settingsTitle.setTranslateX(30);
        settingsTitle.setTranslateY(10);
        settingsTitle.setVisible(false);

        programSettingsTitle = new Label("Information");
        programSettingsTitle.setStyle("-fx-font: 22 arial;");
        programSettingsTitle.setTranslateX(30);
        programSettingsTitle.setTranslateY(80);
        programSettingsTitle.setVisible(false);

        version = new Label("Version: ");
        version.setStyle("-fx-font: 16 arial;");
        version.setTranslateX(30);
        version.setTranslateY(115);
        version.setVisible(false);

        versionResult = new Label(programVersion);
        versionResult.setStyle("-fx-font: 16 arial;");
        versionResult.setTranslateY(115);
        versionResult.setVisible(false);

        latestVersion = new Label("Latest Version: ");
        latestVersion.setStyle("-fx-font: 16 arial;");
        latestVersion.setTranslateX(30);
        latestVersion.setTranslateY(135);
        latestVersion.setVisible(false);

        latestVersionResult = new Label("Locating...");
        latestVersionResult.setStyle("-fx-font: 16 arial;");
        latestVersionResult.setTranslateY(135);
        latestVersionResult.setVisible(false);

        youtubeDlVerification = new Label("YouTube-DL Status: ");
        youtubeDlVerification.setStyle("-fx-font: 16 arial;");
        youtubeDlVerification.setTranslateX(30);
        youtubeDlVerification.setTranslateY(155);
        youtubeDlVerification.setVisible(false);

        youtubeDlVerificationResult = new Label(settings.checkYouTubeDl());
        youtubeDlVerificationResult.setStyle("-fx-font: 16 arial;");
        youtubeDlVerificationResult.setTranslateY(155);
        youtubeDlVerificationResult.setVisible(false);

        fileSettingsTitle = new Label("Files");
        fileSettingsTitle.setStyle("-fx-font: 22 arial;");
        fileSettingsTitle.setTranslateX(30);
        fileSettingsTitle.setTranslateY(195);
        fileSettingsTitle.setVisible(false);

        outputDirectory = new Label("Save music to: ");
        outputDirectory.setStyle("-fx-font: 16 arial;");
        outputDirectory.setTranslateX(30);
        outputDirectory.setTranslateY(230);
        outputDirectory.setVisible(false);

        outputDirectoryResult = new Label(outputDirectorySetting.equals("") ? System.getProperty("user.dir") : outputDirectorySetting);
        outputDirectoryResult.setStyle("-fx-font: 16 arial;");
        outputDirectoryResult.setTranslateY(230);
        outputDirectoryResult.setVisible(false);

        outputDirectoryButton = new Button();
        outputDirectoryButton.setTranslateY(230);
        outputDirectoryButton.setStyle("-fx-cursor: hand;");
        outputDirectoryButton.setOpacity(0);
        outputDirectoryButton.setOnAction(e -> new selectFolder());
        outputDirectoryButton.setVisible(false);

        songDownloadFormat = new Label("Music format: ");
        songDownloadFormat.setStyle("-fx-font: 16 arial;");
        songDownloadFormat.setTranslateX(30);
        songDownloadFormat.setTranslateY(255);
        songDownloadFormat.setVisible(false);

        songDownloadFormatResult = new ComboBox(FXCollections.observableArrayList(
                "mp3",
                "wav",
                "ogg",
                "aac"
        ));
        songDownloadFormatResult.setTranslateX(600 - 30);
        songDownloadFormatResult.setTranslateY(255);
        songDownloadFormatResult.setVisible(false);

        saveAlbumArt = new Label("Save Album Art: ");
        saveAlbumArt.setStyle("-fx-font: 16 arial;");
        saveAlbumArt.setTranslateX(30);
        saveAlbumArt.setTranslateY(280);
        saveAlbumArt.setVisible(false);

        saveAlbumArtResult = new ComboBox(FXCollections.observableArrayList(
                "No",
                "Albums Only",
                "Songs Only",
                "All"
        ));
        saveAlbumArtResult.setTranslateY(280);
        saveAlbumArtResult.setVisible(false);

        confirmChanges = new Button("Confirm");
        confirmChanges.setTranslateY(800-50);
        confirmChanges.setTranslateX(30);
        confirmChanges.setOnAction(e -> submit());
        confirmChanges.setVisible(false);

        cancelBackButton = new Button("Cancel");
        cancelBackButton.setTranslateY(800-50);
        cancelBackButton.setOnAction(e -> searchMode());
        cancelBackButton.setVisible(false);

        // Settings Lines
        settingTitleSubline = new Line(30, 45, 600-30, 45);
        settingTitleSubline.setVisible(false);

        programSettingsTitleSubline = new Line(30, 105, 600-30, 105);
        programSettingsTitleSubline.setVisible(false);

        fileSettingsTitleSubline = new Line(30, 220, 600-30, 220);
        fileSettingsTitleSubline.setVisible(false);

        // Search
        pane.getChildren().add(title);
        pane.getChildren().add(searchRequest);
        pane.getChildren().add(searchResultsTitle);
        pane.getChildren().add(resultsTable);
        pane.getChildren().add(downloadButton);
        pane.getChildren().add(cancelButton);
        pane.getChildren().add(loading);
        pane.getChildren().add(searchesProgressText);
        pane.getChildren().add(footerMarker);
        pane.getChildren().add(settingsLink);
        pane.getChildren().add(settingsLinkButton);

        // Settings
        pane.getChildren().add(settingsTitle);
        pane.getChildren().add(programSettingsTitle);
        pane.getChildren().add(version);
        pane.getChildren().add(versionResult);
        pane.getChildren().add(latestVersion);
        pane.getChildren().add(latestVersionResult);
        pane.getChildren().add(youtubeDlVerification);
        pane.getChildren().add(youtubeDlVerificationResult);
        pane.getChildren().add(fileSettingsTitle);
        pane.getChildren().add(outputDirectory);
        pane.getChildren().add(outputDirectoryResult);
        pane.getChildren().add(outputDirectoryButton);
        pane.getChildren().add(songDownloadFormat);
        pane.getChildren().add(songDownloadFormatResult);
        pane.getChildren().add(saveAlbumArt);
        pane.getChildren().add(saveAlbumArtResult);
        pane.getChildren().add(confirmChanges);
        pane.getChildren().add(cancelBackButton);

        // Settings Lines
        pane.getChildren().add(settingTitleSubline);
        pane.getChildren().add(programSettingsTitleSubline);
        pane.getChildren().add(fileSettingsTitleSubline);

        Scene scene = new Scene(pane);
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

        resultsTable.setTranslateX((600 - resultsTable.getWidth()) / 2);
        resultsTable.setTranslateY(50);

        searchResultsTitle.setTranslateX((600 - resultsTable.getWidth()) / 2);
        searchResultsTitle.setTranslateY(10);

        downloadButton.setTranslateX((600 - resultsTable.getWidth()) / 2);
        downloadButton.setTranslateY(460);
        downloadButton.setDisable(true);

        cancelButton.setTranslateX(480 - (600 - resultsTable.getWidth()) / 2);
        cancelButton.setTranslateY(460);

        resultsTable.setVisible(true);
        searchResultsTitle.setVisible(true);
        downloadButton.setVisible(true);
        cancelButton.setVisible(true);

        return searchResults;

    }

    public synchronized void setSearchData(ArrayList<ArrayList<String>> newData) {
        searchResults = newData;
    }

    public synchronized void cancel() {

        // Stops the search thread from running
        quitQueryThread = true;

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

    }

    public synchronized void initializeDownload() throws IOException {

        ArrayList<String> request = searchResults.get(resultsTable.getSelectionModel().getSelectedIndex());

        loading.setVisible(true);

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

        // 15% of loading bar: Youtube Searches
        //loading.setVisible(true);
        searchesProgressText.setVisible(true);

        // Make Progress Bar Visible
        new downloadHandler();


    }

    public synchronized ArrayList<ArrayList<String>> getSongsData() {
        return songsData;
    }

    public synchronized void selectNewFolder() {

        try {
            JFileChooser newFolder = new JFileChooser();
            newFolder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            newFolder.showSaveDialog(null);

            OutputDirectorySettingNew = newFolder.getSelectedFile().getPath();

        } catch (NullPointerException ignored) {} // User cancels the operation
    }

    public synchronized void settingsMode() {

        // Set search to invisible
        searchRequest.setVisible(false);
        title.setVisible(false);

        // Setting settings link footer to invisible
        footerMarker.setVisible(false);
        settingsLink.setVisible(false);
        settingsLinkButton.setVisible(false);

        // Set setting elements visible
        settingsTitle.setVisible(true);

        // Program
        programSettingsTitle.setVisible(true);
        latestVersion.setVisible(true);
        latestVersionResult.setVisible(true);
        version.setVisible(true);
        versionResult.setVisible(true);
        youtubeDlVerification.setVisible(true);
        youtubeDlVerificationResult.setVisible(true);

        // File
        fileSettingsTitle.setVisible(true);
        outputDirectory.setVisible(true);
        outputDirectoryResult.setVisible(true);
        songDownloadFormat.setVisible(true);
        songDownloadFormatResult.setVisible(true);
        saveAlbumArt.setVisible(true);
        saveAlbumArtResult.setVisible(true);

        // Buttons
        outputDirectoryButton.setVisible(true);
        confirmChanges.setVisible(true);
        cancelBackButton.setVisible(true);
        cancelBackButton.setTranslateX(600 - 30 - cancelBackButton.getWidth());

        // Lines
        settingTitleSubline.setVisible(true);
        programSettingsTitleSubline.setVisible(true);
        fileSettingsTitleSubline.setVisible(true);

        // Setting the labels for information
        outputDirectoryResult.setTranslateX(600 - 30 - outputDirectoryResult.getWidth());
        songDownloadFormatResult.setTranslateX(600 - 30 - songDownloadFormatResult.getWidth());
        saveAlbumArtResult.setTranslateX(600 - 30 - saveAlbumArtResult.getWidth());
        versionResult.setTranslateX(600 - 30 - versionResult.getWidth());
        latestVersionResult.setTranslateX(600 - 30 - latestVersionResult.getWidth());
        youtubeDlVerificationResult.setTranslateX(600 - 30 - youtubeDlVerificationResult.getWidth());

        // Invisilbe button for selection
        outputDirectoryButton.setTranslateX(600 - 30 - outputDirectoryResult.getWidth());
        outputDirectoryButton.setPrefSize(outputDirectoryResult.getWidth(), 25);

        // Additional selection
        songDownloadFormatResult.getSelectionModel().select(musicFormatSetting);
        saveAlbumArtResult.getSelectionModel().select(saveAlbumArtSetting);

        // Scheduling getting latest version
        new getLatestVersion();

    }

    public synchronized void searchMode() {
        // Set search to visible
        searchRequest.setVisible(true);
        title.setVisible(true);

        // Setting settings link footer to visible
        footerMarker.setVisible(true);
        settingsLink.setVisible(true);
        settingsLinkButton.setVisible(true);

        // Set setting elements invisible
        settingsTitle.setVisible(false);

        // Program
        programSettingsTitle.setVisible(false);
        latestVersion.setVisible(false);
        latestVersionResult.setVisible(false);
        version.setVisible(false);
        versionResult.setVisible(false);
        youtubeDlVerification.setVisible(false);
        youtubeDlVerificationResult.setVisible(false);

        // File
        fileSettingsTitle.setVisible(false);
        outputDirectory.setVisible(false);
        outputDirectoryResult.setVisible(false);
        songDownloadFormat.setVisible(false);
        songDownloadFormatResult.setVisible(false);
        saveAlbumArt.setVisible(false);
        saveAlbumArtResult.setVisible(false);

        // Buttons
        confirmChanges.setVisible(false);
        cancelBackButton.setVisible(false);

        // Lines
        settingTitleSubline.setVisible(false);
        programSettingsTitleSubline.setVisible(false);
        fileSettingsTitleSubline.setVisible(false);
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
                        .getValue()
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

                            // This is an album request, year must be known or won't be found and hence doesn't require a check
                            if (searchResult.get(5).equals("")) {
                                selectedImage = new ImageView(new Image(new File("resources/album_default.jpg").toURI().toString()));
                            } else {
                                selectedImage = new ImageView(new Image(searchResult.get(5)));
                            }
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
                            } catch (NullPointerException ignored) {}

                        }

                        resultsTable.getItems().add(
                                new Utils.resultsSet(
                                        selectedImage,
                                        searchResult.get(0),
                                        searchResult.get(1),
                                        year,
                                        genre,
                                        searchResult.get(4)
                                )
                        );
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

            ArrayList<ArrayList<String>> songsData = getSongsData();

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

                try {

                    YoutubeDLRequest request = new YoutubeDLRequest(song.get(2),outputDirectorySetting.equals("") ? metaData.get("directory") : outputDirectorySetting + "\\" + metaData.get("directory"));
                    request.setOption("extract-audio");
                    request.setOption("audio-format " + formatReferences.get(musicFormatSetting));
                    request.setOption("ignore-errors");
                    request.setOption("retries", 10);

                    YoutubeDLResponse response = YoutubeDL.execute(request);
                    
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
                    if (formatReferences.get(musicFormatSetting) == "mp3") {
                        Mp3File mp3Applicator = new Mp3File(metaData.get("directory") + targetFileName);

                        ID3v2 id3v2tag = new ID3v24Tag();
                        mp3Applicator.setId3v2Tag(id3v2tag);

                        // Album Art Application
                        RandomAccessFile albumArtImg = new RandomAccessFile(metaData.get("directory") + "art.jpg", "r");

                        // Could break this up into mb loads
                        byte[] bytes;
                        bytes = new byte[(int) albumArtImg.length()];
                        albumArtImg.read(bytes);
                        albumArtImg.close();

                        id3v2tag.setAlbumImage(bytes, "image/jpg");

                        // Applying remaining data
                        id3v2tag.setTitle(song.get(0));
                        id3v2tag.setAlbum(metaData.get("albumTitle"));
                        id3v2tag.setArtist(metaData.get("artist"));
                        id3v2tag.setAlbumArtist(metaData.get("artist"));
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
                        File file = new File(metaData.get("directory") + targetFileName);
                        file.renameTo(new File(metaData.get("directory") + song.get(0) + "." + formatReferences.get(musicFormatSetting)));
                    }

                    percentIncrease = ((double)Integer.parseInt(song.get(1)) / (double)totalPlayTime) * (totalPlayTime * 0.02313 / (0.5518 * songsData.size() + totalPlayTime * 0.02313));
                    loadingPercent += percentIncrease;
                    double tempLoadingPercent = loadingPercent;

                    Platform.runLater(() -> loading.setProgress(tempLoadingPercent));
                    Platform.runLater(() -> searchesProgressText.setText(Math.round(tempLoadingPercent*10000)/100 + "% Complete"));

                } catch (IOException | YoutubeDLException| InvalidDataException | UnsupportedTagException  e) {
                    e.printStackTrace();
                }

                //Platform.runLater(() -> loading.setProgress(1));
            }

            if (saveAlbumArtSetting == 0 || (saveAlbumArtSetting == 1 && metaData.containsKey("positionInAlbum")) || (saveAlbumArtSetting == 2 && !metaData.containsKey("positionInAlbum"))) {
                File deletion = new File(metaData.get("directory") + "art.jpg");
                if (!deletion.delete()) {
                    Debug.trace("Failed to delete file: " + metaData.get("directory") + "art.jpg");
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
            Platform.runLater(() -> latestVersionResult.setText(settings.getLatestVersion()));
            // Come back when I know how to do this properly
            // It works incredibly well and it's shaving literally less than 100th of a second off but could be nicer looking
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> latestVersionResult.setTranslateX(600 - 30 - latestVersionResult.getWidth()));



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

}