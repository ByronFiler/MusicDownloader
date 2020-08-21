package MusicDownloader.controllers;

import MusicDownloader.Main;
import MusicDownloader.model.Model;
import MusicDownloader.utils.app.debug;
import MusicDownloader.utils.app.resources;
import MusicDownloader.utils.net.db.sites.allmusic;
import MusicDownloader.utils.net.source.sites.youtube;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

public class results {

    @FXML private AnchorPane root;

    @FXML private VBox centerContainer;
    @FXML private ListView<BorderPane> results;

    @FXML private HBox downloadButtonContainer;
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
            root.getStylesheets().add(
                    String.valueOf(
                            Main.class.getResource("app/css/dark.css")
                    )
            );

        else
            root.getStylesheets().add(
                String.valueOf(
                        Main.class.getResource("app/css/standard.css")
                )
            );

        try {
            new ProcessBuilder(System.getenv("ProgramFiles(X86)") + "/youtube-dl/youtube-dl.exe", "--version").start();
            new ProcessBuilder(System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.exe", "--version").start();
        } catch (IOException ignored) {
            downloadButtonContainer.getChildren().setAll(
                    download,
                    new ImageView(
                            new Image(
                                    Main.class.getResourceAsStream("app/img/warning.png"),
                                    40,
                                    40,
                                    true,
                                    true
                            )
                    )
            );
            downloadButtonContainer.setSpacing(5);
            Tooltip.install(downloadButtonContainer, new Tooltip("Missing components to download files, check settings for details."));

            results.setOnMouseClicked(null);
        }

        debug.trace("Initialized results view");
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
            debug.warn("Error generating basic data for queue addition.");
            Platform.runLater(() -> download.setDisable(true));
        }

    }

    @FXML
    public void downloadButtonCheck() {

        // Verify executable
        try {
            if (queueAdder.isDead()) throw new NullPointerException();
        } catch (NullPointerException ignored) {

            for (BorderPane searchResult: results.getItems()) searchResult.setRight(null);
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
                debug.error("Failed to set tick to mark selected element.", e);
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
            debug.error("FXML Error with search.fxml", e);
        }

    }

    // TODO: Add network error handling
    class generateQueueItem implements Runnable {

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
            for (File cachedArt: Objects.requireNonNull(new File(resources.getInstance().getApplicationData() + "cached").listFiles())) {
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
                debug.error("Failed to parse temporary data for ID generation.", e);
            }

            // Checking in downloads history
            JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
            if (idExistsInData(downloadHistory, id))
                return generateNewSongId(downloadItems);

            // Did not match existing records, return generated ID
            return id;
        }

        private JSONArray getSource(String query, int targetTime) {

            youtube youtubeParser = new youtube(query, targetTime);

            try {
                youtubeParser.load();
            } catch (IOException e) {
                debug.warn("Error connecting to https://www.youtube.com/results?search_query=" + query);
                return null;
                // TODO: Await reconnection
            }

            JSONArray searchDataExtracted = youtubeParser.getResults();
            try {

                switch (searchDataExtracted.length()) {

                    case 0:
                        // Unable to find a song which matched what we are looking for
                        debug.warn("Youtube does not have the this song, inform user of failure.");
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
                debug.error("Failed to sort songs data with data: " + searchDataExtracted, e);
                return new JSONArray();
            }




        }

        public void kill() {
            kill = true;
        }

        public boolean isDead() {
            return completed;
        }

        @Override
        public void run() {
            JSONObject downloadItem = new JSONObject();

            JSONArray songs = new JSONArray();
            JSONObject metadata = new JSONObject();

            try {

                // Getting other download items for the purpose of ID validation
                JSONArray collectiveDownloadsObjects = new JSONArray();
                if (Model.getInstance().download.getDownloadQueue().length() > 0)
                    collectiveDownloadsObjects.put(Model.getInstance().download.getDownloadQueue());

                if (Model.getInstance().download.getDownloadObject().length() > 0)
                    collectiveDownloadsObjects.put(Model.getInstance().download.getDownloadObject());

                // Metadata: Shared
                metadata.put("artId", generateNewCacheArtId(collectiveDownloadsObjects));
                metadata.put("artist", basicData.getJSONObject("data").getString("artist"));

                // Additional data revealed either by query or additional request already sent
                if (basicData.getJSONObject("data").getBoolean("album") || !Model.getInstance().settings.getSettingBool("data_saver")) {

                    metadata.put("art", basicData.getJSONObject("data").getString("art"));
                    metadata.put("year", basicData.getJSONObject("data").getString("year"));
                    metadata.put("genre", basicData.getJSONObject("data").getString("genre"));

                } else {

                    // Additional connection required to get album id & metadata
                    allmusic.song songTraceLinkLoader = new allmusic.song(basicData.getJSONObject("view").getString("allmusicSongId"));
                    songTraceLinkLoader.load();

                    metadata.put("art", songTraceLinkLoader.getAlbumArt());
                    metadata.put("year", songTraceLinkLoader.getYear());
                    metadata.put("genre", songTraceLinkLoader.getGenre());

                    basicData.getJSONObject("data").put("allmusicAlbumId", songTraceLinkLoader.getAlbumId());
                }

                // Downloading the album art to cache to prevent album art not being loaded if multiple items are in queue
                try {
                    FileUtils.copyURLToFile(
                            new URL(metadata.getString("art")),
                            new File(resources.getInstance().getApplicationData() + String.format("cached\\%s.jpg", metadata.getString("artId")))
                    );
                } catch (IOException e) {
                    debug.error("Failed to download album art.", e);
                    // TODO: Handle connection
                } catch (JSONException e) {
                    debug.error("JSON Error when downloading album art.", e);
                }

                // Extract relevant data from the album
                allmusic.album albumProcessor = new allmusic.album(basicData.getJSONObject("data").getString("allmusicAlbumId"));
                albumProcessor.load();

                StringBuilder outputDirectory = new StringBuilder(Model.getInstance().settings.getSetting("output_directory"));
                if (basicData.getJSONObject("data").getBoolean("album"))
                    outputDirectory.append("\\").append(albumProcessor.getAlbum());

                metadata.put("directory", outputDirectory.toString());

                metadata.put("album", albumProcessor.getAlbum());
                metadata.put("playtime", albumProcessor.getPlaytime());

                for (allmusic.album.song song: albumProcessor.getSongs()) {

                    JSONObject jSong = new JSONObject();

                    jSong.put("position", albumProcessor.getSongs().indexOf(song) + 1);
                    jSong.put("id", generateNewSongId(collectiveDownloadsObjects));
                    jSong.put("completed", JSONObject.NULL);

                    jSong.put(
                            "source",
                            getSource(
                                    metadata.get("artist") + " " + song.getTitle(),
                                    song.getPlaytime()
                            )
                    );

                    jSong.put("playtime", song.getPlaytime());
                    jSong.put("title", song.getTitle());
                    jSong.put("sample", song.getSample() == null ? JSONObject.NULL : song.getSample());

                    songs.put(jSong);

                }



                if (!kill) {
                    downloadItem.put("metadata", metadata);
                    downloadItem.put("songs", songs);

                    Model.getInstance().download.updateDownloadQueue(downloadItem);
                }

            } catch (JSONException e) {
                debug.error("Error in JSON processing download item.", e);
            }
            catch (IOException e) {
                debug.warn("Connection error, attempting to reconnect.");
                // TODO:  Handle reconnection
            }

            // Restore buttons to default
            Platform.runLater(() -> {
                queueAdditionProgress.setVisible(false);

                download.setText("Download");
                downloadButtonCheck();

                Label linkPart0;
                if (Model.getInstance().download.getDownloadQueue().length() > 0)
                    linkPart0 = new Label(String.format("Added to download queue, in position %s, view progress in", Model.getInstance().download.getDownloadObject().length()));

                else linkPart0 = new Label("Download started, view progress in ");

                linkPart0.getStyleClass().add("sub_text");

                Label linkPart1 = new Label("Downloads");
                linkPart1.setUnderline(true);
                linkPart1.setCursor(Cursor.HAND);
                linkPart1.setOnMouseClicked(e -> {
                    try {
                        Parent searchView = FXMLLoader.load(Main.class.getResource("app/fxml/downloads.fxml"));
                        Stage mainWindow = (Stage) ((Node) e.getSource()).getScene().getWindow();

                        mainWindow.setScene(new Scene(searchView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

                    } catch (IOException er) {
                        debug.error("FXML Error with downloads.fxml", er);
                    }
                });
                linkPart1.getStyleClass().add("sub_text");

                Platform.runLater(() -> {
                    centerContainer.setPadding(new Insets(0, 0, 20, 0));
                    centerContainer.setSpacing(20);
                    centerContainer.getChildren().setAll(results, new HBox(linkPart0, linkPart1));
                });

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
