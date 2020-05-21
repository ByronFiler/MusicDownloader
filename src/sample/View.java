package sample;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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

    public ArrayList<ArrayList<String>> searchResults;

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
                    } catch (IOException | ParseException ioException) {
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

        loading = new ProgressBar();
        loading.setProgress(0);
        loading.setTranslateX(60);
        loading.setPrefWidth(500);
        loading.setPrefHeight(20);
        loading.setTranslateY(510);
        loading.setVisible(false);

        resultsTable = new TableView();
        resultsTable.getSelectionModel().selectedIndexProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                downloadButton.setDisable(false);
            } else {
                downloadButton.setDisable(true);
            }
        });
        resultsTable.setVisible(false);

        TableColumn<String, Utils.resultsSet> albumArtColumn = new TableColumn<>("Album Art");
        albumArtColumn.setCellValueFactory(new PropertyValueFactory<>("albumArtLink"));

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

    public synchronized ArrayList<ArrayList<String>> handleSearch() throws IOException {

        // Load the search data
        ArrayList<ArrayList<String>> searchResults =  utils.allmusicQuery(searchRequest.getText());

        if (searchResults.size() > 0) {

            // Transition to table
            for (ArrayList<String> searchResult: searchResults)
            {
                resultsTable.getItems().add(
                        new Utils.resultsSet(
                            Integer.toString(searchResults.indexOf(searchResult)),
                            "art",
                            searchResult.get(0),
                            searchResult.get(1),
                            searchResult.get(2),
                            searchResult.get(3),
                            searchResult.get(4)
                    )
                );
            }

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


        } else {

            // Inform user invalid search

        }

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

    }

    public synchronized void initializeDownload() throws IOException, ParseException {

        ArrayList<String> request = searchResults.get(resultsTable.getSelectionModel().getSelectedIndex());
        ArrayList<ArrayList<String>> songsData = new ArrayList<>();
        HashMap<String, String> metaData = new HashMap<>();
        String directoryName = "";

        if (request.get(4) == "Album") {
            // Prepare all songs

            // Meta data required is found in the search
            metaData.put("albumTitle", request.get(0));
            metaData.put("artist", request.get(1));
            metaData.put("year", request.get(2));
            metaData.put("genre", request.get(3));
            metaData.put("downloadRequestType", "album");

            // Producing the folder to save the data to
            directoryName = Utils.generateFolder(request.get(0)); // Create new unique directory with album name

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

        // Make Progress Bar Visible
        loading.setVisible(true);

        Double progress = (double)0;
        Double progressIncrement = ((double)15 / (double)songsData.size()) / (double)100;

        for (int i = 0; i < songsData.size(); i++){
            songsData.get(i).add(Utils.evaluateBestLink(Utils.youtubeQuery(songsData.get(i).get(0)), Integer.parseInt(songsData.get(i).get(1))));
            progress += progressIncrement;
            setProgress(progress);
        }

        // 85% of loading bar: downloads

    }

    public synchronized void setProgress(Double percentage){
        loading.setProgress(percentage);
    }

}
