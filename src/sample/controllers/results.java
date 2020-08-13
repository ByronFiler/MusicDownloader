package sample.controllers;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sample.Main;
import sample.utils.debug;
import sample.model.Model;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

public class results {

    @FXML private AnchorPane root;

    @FXML private ListView<BorderPane> results;

    @FXML private ProgressIndicator queueAdditionProgress;
    @FXML private Button download;
    @FXML private Button cancel;

    generateQueueItem queueAdder;

    @FXML
    private void initialize() {

        // Set the table data
        results.getItems().setAll(Model.getInstance().search.getSearchResults());

        // Set theme
        if (Model.getInstance().settings.getSettingBool("dark_theme"))
            root.getStylesheets().add(String.valueOf(Main.class.getResource("app/css/dark.css")));

        else
            root.getStylesheets().add(String.valueOf(Main.class.getResource("app/css/standard.css")));

        debug.trace(null, "Initialized results view");
    }

    @FXML
    public void download() {

        try {

            queueAdditionProgress.setVisible(true);

            download.setText("Adding to queue...");
            download.setDisable(true);

            cancel.setText("Cancel");
            cancel.getStyleClass().set(1, "cancel_button");
            cancel.setOnMouseClicked(e -> queueAdder.kill());

            // Selected Item -> Selected Item Data -> Select Item Data in correctly positioned array -> JSON Data needed -> Spawn thread with data to generate a queue item
            queueAdder = new generateQueueItem(
                    Model
                            .getInstance()
                            .search
                            .getSearchResultsJson()
                            .getJSONObject(
                                    Arrays
                                            .asList(Model.getInstance().search.getSearchResults())
                                            .indexOf(results.getSelectionModel().getSelectedItem())
                            )

            );
        } catch (JSONException e) {
            debug.warn(null, "Error generating basic data for queue addition.");
            Platform.runLater(() -> download.setDisable(true));
        }

    }

    @FXML
    public void downloadButtonCheck() {

        try {
            if (queueAdder.isDead())
                throw new NullPointerException();
        } catch (NullPointerException ignored) {

            for (BorderPane searchResult: results.getItems()) {
                searchResult.setRight(null);
            }
            try {

                HBox tickContainer = new HBox(new ImageView(
                        new Image(
                                Main.class.getResource("app/img/tick.png").toURI().toString(),
                                25,
                                25,
                                true,
                                true
                        )
                ));
                tickContainer.setAlignment(Pos.CENTER);
                tickContainer.setPadding(new Insets(0, 5, 0, 0));

                results.getSelectionModel().getSelectedItems().get(0).setRight(tickContainer);
            } catch (URISyntaxException e) {
                debug.error(null, "Failed to set tick to mark selected element.", e);
            }

            Platform.runLater(() -> download.setDisable(results.getSelectionModel().getSelectedIndex() == -1));
        }
    }

    @FXML
    public void searchView(Event event) {

        try {

            Parent searchView = FXMLLoader.load(Main.class.getResource("app/fxml/search.fxml"));
            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();

            mainWindow.setScene(new Scene(searchView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

        } catch (IOException e) {
            debug.error(null, "FXML Error with search.fxml", e);
        }

    }

    // TODO: Add network error handling
    class generateQueueItem implements Runnable{

        private final Thread thread;
        private final JSONObject basicData;
        private volatile boolean kill;
        private volatile boolean completed = false;

        public generateQueueItem(JSONObject basicData) {
            this.basicData = basicData;

            thread = new Thread(this, "generate-queue-item");
            thread.setDaemon(true);
            thread.start();
        }

        private synchronized String generateNewCacheArtId(JSONArray downloadItems) {

            // Generating the potential ID
            String id = Double.toString(Math.random()).split("\\.")[1];

            // Checking if it exists in the downloads queue
            try {
                for (int i = 0; i < downloadItems.length(); i++) {
                    for (int j = 0; j < downloadItems.getJSONObject(i).length(); j++) {
                        if (downloadItems.getJSONArray(i).getJSONObject(j).getJSONObject("meta").get("artId").equals(id)) {
                            // Our generated ID already exists in the queue, generate a new one
                            return generateNewCacheArtId(downloadItems);
                        }

                    }
                }
            } catch (JSONException ignored) {}

            // Checking if it exists in existing cached arts
            for (File cachedArt: Objects.requireNonNull(new File(System.getenv("APPDATA") + "\\MusicDownloader\\cached").listFiles())) {
                if (cachedArt.isFile() && cachedArt.getName().split("\\.")[1].equals("jpg") && cachedArt.getName().split("\\.")[0].equals(id)) {
                    // Our generated ID already exists in the files, generate a new one
                    return generateNewCacheArtId(downloadItems);
                }
            }

            // Generated ID was not found to match any existing record, hence use this ID
            return id;

        }

        private synchronized boolean idExistsInData(JSONArray songs, String id) throws NoSuchElementException{

            try {
                for (int i = 0; i < songs.length(); i++)
                {
                    if ((songs.getJSONObject(0)).get("id").equals(id)) {
                        return true;
                    }
                }
            } catch (JSONException | NoSuchElementException ignored) {}
            return false;

        }

        private synchronized String generateNewSongId(JSONArray downloadItems) {
            String id = Double.toString(Math.random()).split("\\.")[1];

            // Checking in temporary data
            try {
                for (int i = 0; i < downloadItems.length(); i++)
                    if (idExistsInData(downloadItems.getJSONObject(i).getJSONArray("songs"), id))
                        return generateNewCacheArtId(downloadItems);

            } catch (JSONException e) {
                debug.error(null, "Failed to parse temporary data for ID generation.", e);
            }

            // Checking in downloads history
            JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
            if (idExistsInData(downloadHistory, id))
                return generateNewSongId(downloadItems);

            // Did not match existing records, return generated ID
            return id;
        }

        private JSONArray getSource(String query, int targetTime) {

            // [Web Data] -> JavaScript -> String -> Json -> Data
            Document youtubeSearch;
            try {
                youtubeSearch = Jsoup.connect("https://www.youtube.com/results?search_query=" + query).get();
            } catch (IOException e) {
                debug.warn(thread, "Error connecting to https://www.youtube.com/results?search_query=" + query);
                return new JSONArray();
                // Await reconnection
            }

            JSONArray searchDataExtracted = new JSONArray();
            JSONObject searchDataTemp;

            if (youtubeSearch.select("script").size() == 17) {
                // Youtube has given us the data we require embedded in the HTML and must be parsed from the HTML

                // Video Times: youtubeSearch.select("ol.item-section").get(0).select("span.video-time").get(selection)
                // Video Link: youtubeSearch.select("ol.item-section").get(0).select("a[href][aria-hidden]").get(selection).attr("href")

                for (int i = 0; i < youtubeSearch.select("ol.item-section").get(0).select("span.video-time").size(); i++) {

                    try {
                        searchDataTemp = new JSONObject();
                        searchDataTemp.put(
                                "watch_id",
                                youtubeSearch
                                        .select("ol.item-section")
                                        .get(0)
                                        .select("a[href][aria-hidden]")
                                        .get(i).attr("href")
                                        .substring(9) // Removes /watch?v= from the source
                        );
                        searchDataTemp.put(
                                "difference",
                                Math.abs(
                                    timeConversion(
                                            youtubeSearch
                                                    .select("ol.item-section")
                                                    .get(0)
                                                    .select("span.video-time")
                                                    .get(i)
                                                    .text()
                                    ) - targetTime
                                )
                        );
                        searchDataExtracted.put(searchDataTemp);
                    } catch (JSONException e) {
                        debug.warn(null, "Failed to generate search data from query, from html response.");
                    }
                }

            } else {

                // YouTube has given us the data stored in json stored script tags which be parsed
                try {
                    // Web Data -> [JavaScript] -> String -> Json -> Data
                    Element jsData = youtubeSearch.select("script").get(24);

                    // Web Data -> JavaScript -> [String] -> Json -> Data
                    String jsonConversion = jsData.toString();
                    jsonConversion = jsonConversion.substring(39, jsonConversion.length() - 119);

                    // Web Data -> JavaScript -> String -> [Json] -> Data
                    JSONObject json = new JSONObject(jsonConversion);

                    // Parsing deep JSON to get relevant data
                    JSONArray contents = json
                            .getJSONObject("contents")
                            .getJSONObject("twoColumnSearchResultsRenderer")
                            .getJSONObject("primaryContents")
                            .getJSONObject("sectionListRenderer")
                            .getJSONArray("contents")
                            .getJSONObject(0)
                            .getJSONObject("itemSectionRenderer")
                            .getJSONArray("contents");

                    // If youtube gives a bad response, just retry
                    if (contents.length() < 10)
                        return getSource(query, targetTime);

                    for (int i = 0; i < contents.length(); i++) {

                        try {

                            int length = timeConversion(
                                    contents
                                            .getJSONObject(i)
                                            .getJSONObject("videoRenderer")
                                            .getJSONObject("lengthText")
                                            .getString("simpleText")
                            );

                            // Checks that the length is within 15% either way of the target time, otherwise definitely not relevant.
                            if (length < targetTime * 1.15 && length > targetTime / 1.15) {

                                searchDataTemp = new JSONObject();

                                // Extract the playtime and the link to the video
                                searchDataTemp.put(
                                        "watch_id",
                                        contents
                                                .getJSONObject(i)
                                                .getJSONObject("videoRenderer")
                                                .getString("videoId")
                                );

                                searchDataTemp.put("difference", Math.abs(length - targetTime));

                                searchDataExtracted.put(searchDataTemp);
                            }

                        } catch (JSONException ignored) {

                        } // Youtube adds random elements that are tricky to handle and are best ignored
                    }

                } catch (JSONException e) {
                    debug.error(thread, "Failed to parse youtube results.", e);
                }

            }

            try {

                switch (searchDataExtracted.length()) {

                    case 0:
                        // Unable to find a song which matched what we are looking for
                        debug.warn(thread, "Youtube does not have the this song, inform user of failure.");
                        return new JSONArray();

                    case 1:
                        // Only one element, hence doesn't need to be sorted
                        return new JSONArray("[" + searchDataExtracted.getJSONObject(0).getString("watch_id") + "]");

                    default:
                        // Quick-sort remaining and return
                        JSONArray sortedData = new QuickSort(searchDataExtracted, 0, searchDataExtracted.length() - 1).getSorted();
                        JSONArray reducedData = new JSONArray();
                        for (int i = 0; i < sortedData.length(); i++) {
                            reducedData.put(sortedData.getJSONObject(i).getString("watch_id"));
                        }
                        return reducedData;
                }
            } catch (JSONException e) {
                debug.error(thread, "Failed to sort songs data with data: " + searchDataExtracted, e);
                return new JSONArray();
            }


        }

        private int timeConversion(String stringTime) {

            String[] songDataBreak = stringTime.split(":");

            int songLenSec = 0;

            for (int i = songDataBreak.length-1; i >= 0; i--) {
                // Time * 60^^Index ie
                // 01:27 -> 27:01 -> ((27)*60^^0) + ((1)*60^^1) -> 87
                songLenSec += (Double.parseDouble(songDataBreak[Math.abs(i-songDataBreak.length+1)]) * Math.pow(60, i));
            }

            return songLenSec;
        }

        public void kill() {
            kill = true;
        }

        public boolean isDead() {
            return completed;
        }

        public void run() {
            JSONObject downloadItem = new JSONObject();
            JSONArray songs = new JSONArray();
            JSONObject metaData = new JSONObject();

            try {
                // All existing downloads object for reference in creating unique IDs
                JSONArray collectiveDownloadsObjects = new JSONArray();
                if (Model.getInstance().download.getDownloadQueue().length() > 0)
                    collectiveDownloadsObjects.put(Model.getInstance().download.getDownloadQueue());

                if (Model.getInstance().download.getDownloadObject().length() > 0)
                    collectiveDownloadsObjects.put(Model.getInstance().download.getDownloadObject());

                metaData.put("art", basicData.getString("art"));
                metaData.put("artist", basicData.get("artist"));
                metaData.put("artId", generateNewCacheArtId(collectiveDownloadsObjects));
                metaData.put("year", basicData.getString("year"));
                metaData.put("genre", basicData.getString("genre"));
                metaData.put("playtime", 0);

                Elements trackResults;
                if (!basicData.getBoolean("album")) {

                    Document songDataRequest = null;
                    Document albumDataRequest = null;

                    // Different output directory
                    metaData.put("directory", Model.getInstance().settings.getSetting("output_directory"));

                    // Requires additional work to get the album data we want, takes time so we check twice, otherwise excessive wait
                    if (!kill)
                        songDataRequest = Jsoup.connect(basicData.getString("link")).get();

                    if (!kill)
                        albumDataRequest = Jsoup.connect(Objects.requireNonNull(songDataRequest).selectFirst("div.title").selectFirst("a").attr("href")).get();

                    // Get album title
                    metaData.put("album", Objects.requireNonNull(albumDataRequest).selectFirst("h1.album-title").text());
                    trackResults = albumDataRequest.select("tr.track");

                } else {

                    // Directory created is the name of the album
                    metaData.put("directory", Model.getInstance().settings.getSetting("output_directory") + "\\" + basicData.get("title"));

                    // Add the album title we want
                    metaData.put("album", basicData.getString("title"));

                    // Contains the direct link to the album we want
                    trackResults = Jsoup.connect(basicData.getString("link")).get().select("tr.track");
                }

                for (Element track: trackResults) {

                    if (kill)
                        break;

                    if ( (!basicData.getBoolean("album") && track.selectFirst("div.title").selectFirst("a").text().equals(basicData.getString("title"))) || basicData.getBoolean("album")) {
                        JSONObject newSong = new JSONObject();
                        newSong.put("title", track.select("div.title").text());
                        newSong.put("position", trackResults.indexOf(track) +1);
                        newSong.put("id", generateNewSongId(collectiveDownloadsObjects));
                        newSong.put("source", getSource(
                                        metaData.get("artist") + " " + track.select("div.title").text(),
                                        timeConversion(track.select("td.time").text())
                                )
                        );
                        newSong.put("completed", JSONObject.NULL);

                        try {
                            String sampleSource = track.selectFirst("a.audio-player").attr("data-sample-url");
                            newSong.put("sample", sampleSource.substring(46, sampleSource.length()-5));
                        } catch (NullPointerException ignored) {}

                        metaData.put("playtime", metaData.getInt("playtime") + timeConversion(track.select("td.time").text()));
                        newSong.put("playtime", timeConversion(track.select("td.time").text()));

                        songs.put(newSong);

                    }
                }

                if (!kill) {
                    downloadItem.put("metadata", metaData);
                    downloadItem.put("songs", songs);

                    Model.getInstance().download.updateDownloadQueue(downloadItem);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                debug.error(thread, "Error in JSON processing download item.", e);
            } catch (IOException e) {
                debug.warn(thread, "Connection error, attempting to reconnect.");
                // Handle reconnection
            }

            // Restore buttons to default
            Platform.runLater(() -> {
                queueAdditionProgress.setVisible(false);

                download.setText("Download");
                downloadButtonCheck();

                cancel.setText("Back");
                cancel.getStyleClass().set(1, "back_button");
                cancel.setOnMouseClicked(results.this::searchView);
            });
            completed = true;

        }

        private class QuickSort {

            private final JSONArray searchDataExtracted;

            public QuickSort(JSONArray searchDataExtracted, int low, int high) throws JSONException {
                this.searchDataExtracted = searchDataExtracted;

                sort(this.searchDataExtracted, low, high);
            }

            int partition(JSONArray searchData, int low, int high) throws JSONException{
                int pivot = searchData.getJSONObject(high).getInt("difference");
                int i = low-1; // index of smaller element
                for (int j=low; j<high; j++)
                {
                    // If current element is smaller than the pivot
                    if (searchData.getJSONObject(j).getInt("difference") < pivot)
                    {
                        i++;

                        // swap arr[i] and arr[j]
                        //int temp = arr[i];
                        int temp = searchData.getJSONObject(i).getInt("difference");
                        String temp0 = searchData.getJSONObject(i).getString("watch_id");

                        searchData.getJSONObject(i).put("difference", searchData.getJSONObject(j).getInt("difference"));
                        searchData.getJSONObject(i).put("watch_id", searchData.getJSONObject(j).getString("watch_id"));

                        searchData.getJSONObject(j).put("difference", temp);
                        searchData.getJSONObject(j).put("watch_id", temp0);
                    }
                }

                // swap arr[i+1] and arr[high] (or pivot)
                int temp = searchData.getJSONObject(i+1).getInt("difference");
                String temp0 = searchData.getJSONObject(i+1).getString("watch_id");

                searchData.getJSONObject(i+1).put("difference", searchData.getJSONObject(high).getInt("difference"));
                searchData.getJSONObject(i+1).put("watch_id", searchData.getJSONObject(high).getString("watch_id"));

                searchData.getJSONObject(high).put("difference", temp);
                searchData.getJSONObject(high).put("watch_id", temp0);

                return i+1;
            }

            void sort(JSONArray searchData, int low, int high) throws JSONException {
                if (low < high)
                {
                    int pi = partition(searchData, low, high);

                    // Recursively sort elements before
                    // partition and after partition
                    sort(searchData, low, pi-1);
                    sort(searchData, pi+1, high);
                }
            }

            JSONArray getSorted() {
                return searchDataExtracted;
            }
        }

    }

}
