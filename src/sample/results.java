package sample;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class results {

    @FXML public AnchorPane root;

    @FXML public TableView<Model.resultsSet> results;
    @FXML public TableColumn<String, Model.resultsSet> artColumn;
    @FXML public TableColumn<String, Model.resultsSet> titleColumn;
    @FXML public TableColumn<String, Model.resultsSet> artistColumn;
    @FXML public TableColumn<String, Model.resultsSet> genreColumn;
    @FXML public TableColumn<String, Model.resultsSet> yearColumn;
    @FXML public TableColumn<String, Model.resultsSet> typeColumn;

    @FXML public ProgressIndicator queueAdditionProgress;
    @FXML public Button download;
    @FXML public Button cancel;

    @FXML
    private void initialize() {

        // Declare table properties
        titleColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.2).add(-15));
        artistColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.2).add(-15));
        yearColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.2).add(-15));
        genreColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.2).add(-15));
        typeColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.2).add(-15));

        // Set the table data
        results.getItems().setAll(Model.getInstance().search.getSearchResults());

        Debug.trace(null, "Initialized results view");

    }


    @FXML
    public void download() {

        try {
            // Selected Item -> Selected Item Data -> Select Item Data in correctly positioned array -> JSON Data needed -> Spawn thread with data to generate a queue item
            new generateQueueItem(
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
            Debug.warn(null, "Error generating basic data for queue addition.");
            Platform.runLater(() -> download.setDisable(true));
        }



    }

    @FXML
    public void downloadButtonCheck() {
        Platform.runLater(() -> download.setDisable(results.getSelectionModel().getSelectedIndex() == -1));
    }

    @FXML
    public void searchView(Event event) {

        try {

            Parent searchView = FXMLLoader.load(getClass().getResource("app/fxml/search.fxml"));
            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();

            mainWindow.setScene(new Scene(searchView));

        } catch (IOException e) {
            Debug.error(null, "FXML Error with search.fxml", e.getCause());
        }

    }

    // TODO: Add network error handling
    static class generateQueueItem implements Runnable{

        private final Thread thread;
        private final JSONObject basicData;

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
            for (File cachedArt: Objects.requireNonNull(new File("resources\\cached").listFiles())) {
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
                for (int i = 0; i < downloadItems.length(); i++) {
                    for (int j = 0; j < downloadItems.getJSONArray(i).length(); j++) {

                        if (idExistsInData(downloadItems.getJSONArray(i).getJSONObject(j).getJSONArray("songs"), id)) {
                            return generateNewCacheArtId(downloadItems);
                        }

                    }
                }
            } catch (JSONException ignored) {}

            // Checking in downloads history
            try {
                JSONArray fileData = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());
                if (idExistsInData(fileData, id)) {
                    return generateNewSongId(downloadItems);
                }
            } catch (NoSuchElementException | FileNotFoundException | JSONException ignored) {}

            // Did not match existing records, return generated ID
            return id;
        }

        private String getSource(String query, int targetTime) {

            // [Web Data] -> JavaScript -> String -> Json -> Data
            Document youtubeSearch;
            try {
                youtubeSearch = Jsoup.connect("https://www.youtube.com/results?search_query=" + query).get();
            } catch (IOException e) {
                Debug.warn(thread, "Error connecting to https://www.youtube.com/results?search_query=" + query);
                return "";
                // Await reconnection
            }

            ArrayList<ArrayList<String>> searchDataExtracted = new ArrayList<>();
            ArrayList<String> searchDataTemp;

            if (youtubeSearch.select("script").size() == 17) {
                // Youtube has given us the data we require embedded in the HTML and must be parsed from the HTML

                // Video Times: youtubeSearch.select("ol.item-section").get(0).select("span.video-time").get(selection)
                // Video Link: youtubeSearch.select("ol.item-section").get(0).select("a[href][aria-hidden]").get(selection).attr("href")

                for (int i = 0; i < youtubeSearch.select("ol.item-section").get(0).select("span.video-time").size(); i++) {

                    searchDataTemp = new ArrayList<>();
                    searchDataTemp.add("https://youtube.com" + youtubeSearch.select("ol.item-section").get(0).select("a[href][aria-hidden]").get(i).attr("href"));
                    searchDataTemp.add(Integer.toString(timeConversion(youtubeSearch.select("ol.item-section").get(0).select("span.video-time").get(i).text())));
                    searchDataExtracted.add(searchDataTemp);
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

                    // contents -> twoColumnSearchResultsRenderer -> primaryContents -> sectionListRenderer
                    JSONObject contents = (JSONObject) (
                            (JSONObject) (
                                    (JSONObject) (
                                            (JSONObject) json.get("contents")
                                    ).get("twoColumnSearchResultsRenderer")
                            ).get("primaryContents")
                    ).get("sectionListRenderer");

                    JSONArray contents1 = (JSONArray) contents.get("contents");
                    JSONObject contents2 = (JSONObject) (
                            (JSONObject) contents1.get(0)
                    ).get("itemSectionRenderer");

                    JSONArray contents3 = (JSONArray) contents2.get("contents");

                    //for (Object videoData: contents3)
                    for (int i = 0; i < contents3.length(); i++) {
                        searchDataTemp = new ArrayList<>();
                        JSONObject jsonVideoData = contents3.getJSONObject(i);
                        try {
                            // Extract the playtime and the link to the video
                            String lengthData = (String) ((JSONObject) ((JSONObject) jsonVideoData.get("videoRenderer")).get("lengthText")).get("simpleText");
                            String watchLink = "https://www.youtube.com/watch?v=" + ((JSONObject) jsonVideoData.get("videoRenderer")).get("videoId");

                            searchDataTemp.add(watchLink);
                            searchDataTemp.add(Integer.toString(timeConversion(lengthData)));


                            searchDataExtracted.add(searchDataTemp);
                        } catch (Exception ignored) {
                        }
                    }
                } catch (JSONException e) {
                    Debug.error(thread, "Failed to parse youtube results.", e.getCause());
                }

            }

            int bestTimeDifference = Integer.MAX_VALUE;
            int indexOfBest = -1;

            for (ArrayList<String> video: searchDataExtracted)
            {
                if (Math.abs(Integer.parseInt(video.get(1)) - targetTime) < bestTimeDifference){
                    bestTimeDifference = Math.abs(Integer.parseInt(video.get(1)) - targetTime);
                    indexOfBest = searchDataExtracted.indexOf(video);
                }
            }

            return searchDataExtracted.get(indexOfBest).get(0);


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
                if (basicData.getInt("album") == 0) {

                    // Different output directory
                    metaData.put("directory", Model.getInstance().settings.getSetting("output_directory"));

                    // Requires additional work to get the album data we want
                    Document songDataRequest = Jsoup.connect(basicData.getString("link")).get();

                    Document albumDataRequest = Jsoup.connect(songDataRequest.selectFirst("div.title").selectFirst("a").attr("href")).get();

                    // Get album title
                    metaData.put("album", albumDataRequest.selectFirst("h1.album-title").text());

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
                    if ( (basicData.getInt("album") == 0 && track.selectFirst("div.title").selectFirst("a").text().equals(basicData.getString("title"))) || basicData.getInt("album") != 0 ) {
                        JSONObject newSong = new JSONObject();
                        newSong.put("title", track.select("div.title").text());
                        newSong.put("position", trackResults.indexOf(track) +1);
                        newSong.put("id", generateNewSongId(collectiveDownloadsObjects));
                        newSong.put("source", getSource(
                                        metaData.get("artist") + " " + track.select("div.title").text(),
                                        timeConversion(track.select("td.time").text())
                                )
                        );
                        try {
                            newSong.put("sample", track.selectFirst("a.audio-player").attr("data-sample-url"));
                        } catch (NullPointerException ignored) {}

                        metaData.put("playtime", metaData.getInt("playtime") + timeConversion(track.select("td.time").text()));

                        songs.put(newSong);

                    }
                }


                downloadItem.put("metadata", metaData);
                downloadItem.put("songs", songs);
                Model.getInstance().download.updateDownloadQueue(downloadItem);

            } catch (JSONException e) {
                Debug.error(thread, "Error in JSON processing download item.", e.getCause());
            } catch (IOException e) {
                Debug.warn(thread, "Connection error, attempting to reconnect.");
                // Handle reconnection
            }

        }

    }

}
