package sample;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;
import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLRequest;
import javafx.concurrent.Task;
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
import javafx.stage.Stage;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class View implements EventHandler<KeyEvent>
{
    public Controller controller;
    public Utils utils;

    public int width;
    public int height;

    public Pane pane;
    public Canvas canvas;

    public TextField searchRequest;
    public TableView resultsTable;
    public Label searchResultsTitle;
    public Label title;
    public Button downloadButton;
    public Button cancelButton;
    public ProgressBar loading;
    public Label searchesProgressText;

    public ArrayList<ArrayList<String>> searchResults;
    public ArrayList<ArrayList<String>> songsData;
    public HashMap<String, String> metaData;

    public boolean quitQueryThread = false;

    public View(int w, int h)
    {
        Debug.trace("View::<constructor>");
        width = w;
        height = h;
    }

    public void start(Stage window)
    {
        pane = new Pane();
        pane.setId("initial");

        canvas = new Canvas(width, height);
        pane.getChildren().add(canvas);

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
        loading.progressProperty().bind(
                youtubeRequestsHandler.progressProperty()
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

        pane.getChildren().add(title);
        pane.getChildren().add(searchRequest);
        pane.getChildren().add(searchResultsTitle);
        pane.getChildren().add(resultsTable);
        pane.getChildren().add(downloadButton);
        pane.getChildren().add(cancelButton);
        pane.getChildren().add(loading);
        pane.getChildren().add(searchesProgressText);

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

        quitQueryThread = true;
        MyThread tableHandler = new MyThread();

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

        loading = new ProgressBar();
        loading.setProgress(0);
        loading.progressProperty().unbind();
        loading.setVisible(false);

        searchesProgressText.setVisible(false);
        searchesProgressText.setText("Search Songs: 0%");

    }

    public synchronized void initializeDownload() throws IOException {

        ArrayList<String> request = searchResults.get(resultsTable.getSelectionModel().getSelectedIndex());

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

            String genre = songDataRequest.selectFirst("div.song_genres").selectFirst("div.middle").select("a").text();
            genre = (genre.split("\\(")[0]);
            genre = genre.substring(0, genre.length()-1);

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
        loading.setVisible(true);
        searchesProgressText.setVisible(true);

        // Make Progress Bar Visible
        Thread thread = new Thread(youtubeRequestsHandler, "task-thread");
        thread.setDaemon(true);
        thread.start();


    }

    public synchronized ArrayList<ArrayList<String>> getSongsData() {
        return songsData;
    }

    class MyThread implements Runnable {

        Thread t;
        MyThread (){
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

                // Signal is always sent immediately when running, only want to kill other threads that are in loop, not this one
                quitQueryThread = false;

                for (ArrayList<String> searchResult : searchResults) {

                    // Sending a new query requires quitting the old
                    if (quitQueryThread) {
                        quitQueryThread = false;
                        resultsTable.getItems().clear();
                        break;
                    }


                    // Reference for web requests
                    Document songDataPage = null;

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

                        try {
                            songDataPage = Jsoup.connect(searchResult.get(6)).get();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (songDataPage.selectFirst("td.cover").selectFirst("img").attr("src") != null && songDataPage.selectFirst("td.cover").selectFirst("img").attr("src").startsWith("https")) {
                            selectedImage = new ImageView(new Image(songDataPage.selectFirst("td.cover").selectFirst("img").attr("src")));
                        } else {
                            selectedImage = new ImageView(new Image(new File("resources/song_default.png").toURI().toString()));
                        }


                        if (songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear").length() == 4 && year.length() == 0) {
                            year = songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear");
                        }

                        try {
                            genre = songDataPage.selectFirst("div.song_genres").selectFirst("div.middle").selectFirst("a").text().split("\\(")[0].substring(0, songDataPage.selectFirst("div.song_genres").selectFirst("div.middle").selectFirst("a").text().split("\\(")[0].length()-1);
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
            } else {
                System.out.println("Invalid Search");
            }
        }

    }

    Task<Void> youtubeRequestsHandler = new Task<>() {
        @Override
        protected Void call() throws Exception {

            ArrayList<ArrayList<String>> songsData = getSongsData();

            double loadingPercent = 0;
            double percentIncrease = ((double)1 / (double)songsData.size()) * 0.15;

            for (ArrayList<String> songsDatum : songsData) {

                updateProgress(loadingPercent, 1);
                songsDatum.add(Utils.evaluateBestLink(Utils.youtubeQuery(metaData.get("artist") + " " + songsDatum.get(0)), Integer.parseInt(songsDatum.get(1))));

                loadingPercent += percentIncrease;

            }

            loadingPercent = 0.15;
            percentIncrease = ((double)1 / (double)songsData.size()) * 0.85;
            updateProgress(0.15, 1);

            for (ArrayList<String> song: songsData)
            {
                YoutubeDLRequest request = new YoutubeDLRequest(song.get(2), metaData.get("directory"));
                request.setOption("extract-audio");
                request.setOption("audio-format mp3");
                request.setOption("ignore-errors");
                request.setOption("retries", 10);
                YoutubeDL.execute(request);

                loadingPercent += percentIncrease;
                updateProgress(loadingPercent, 1);

                // We want the name of the file which is output to the current working directory in the format [YOUTUBE-TITLE]-[YOUTUBE-WATCH-ID]
                File folder = new File(metaData.get("directory"));
                File[] folderContents = folder.listFiles();
                String targetFileName = "";

                for (File file: Objects.requireNonNull(folderContents)) {
                    if (file.isFile()) {
                        if (file.getName().endsWith("-" + song.get(2).substring(32) + ".mp3")) {
                            targetFileName = file.getName();
                        }
                    }
                }

                // Now to apply the metadata
                Mp3File mp3Applicator = new Mp3File(metaData.get("directory") + targetFileName);
                ID3v2 id3v2tag = new ID3v24Tag();
                mp3Applicator.setId3v2Tag(id3v2tag);

                // Album Art Application
                RandomAccessFile albumArtImg = new RandomAccessFile(metaData.get("directory") + "art.jpg", "r");
                // Could break this up into mb loads
                byte[] bytes = new byte[(int) albumArtImg.length()];
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

                mp3Applicator.save(metaData.get("directory") + song.get(0) + ".mp3");

                File deletion = new File(metaData.get("directory") + targetFileName);
                if (!deletion.delete()) {
                    Debug.trace("Failed to delete file: " + metaData.get("directory") + targetFileName);
                }
            }

            File deletion = new File(metaData.get("directory") + "art.jpg");
            if (!deletion.delete()) {
                Debug.trace("Failed to delete file: " + metaData.get("directory") + "art.jpg");
            }

            updateProgress(1, 1);

            return null;
        }
    };

}