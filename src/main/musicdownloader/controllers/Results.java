package musicdownloader.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
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
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;
import musicdownloader.Main;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.fx.Result;
import musicdownloader.utils.net.db.sites.Allmusic;
import musicdownloader.utils.net.source.sites.Youtube;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Objects;

/*
TODO
 - Remove the title and replace it with a new search box
 - Consider thread loading the album art

- Loaded the additional data about the album should be saved and used to save a web request when building data
 - Searching should be disallowed when adding to queue (Use class wide bools, no more NullPointerExceptions)

 */

public class Results {

    @FXML
    private AnchorPane root;
    @FXML
    private BorderPane mainContainer;

    @FXML
    private TextField searchField;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private ListView<BorderPane> results;

    @FXML
    private BorderPane footer;
    @FXML
    private HBox downloadButtonContainer;
    @FXML
    private ProgressIndicator queueAdditionProgress;
    @FXML
    private Button download;
    @FXML
    private Button cancel;

    private generateQueueItem queueAdder;
    private String loadedQuery;
    private boolean modifiedResults;
    private JSONObject viewData;

    private final ArrayList<searchResult.MediaController> mediaPlayers = new ArrayList<>();

    @FXML
    private void initialize() {

        // Set the table data
        try {
            viewData = Model.getInstance().search.getSearchResultsJson();
            JSONArray songs = Model.getInstance().search.getSearchResultsJson().getJSONArray("songs");
            if (songs.length() > 0) {
                for (int i = 0; i < songs.length(); i++) {
                    results.getItems().add(new searchResult(songs.getJSONObject(i)).getView());
                }
            } else defaultView("No Search Results Found");
        } catch (JSONException e) {
            Debug.error("Failed to load search results.", e);
        }

        // Set the search box data
        try {
            loadedQuery = Model
                    .getInstance()
                    .search
                    .getSearchResultsJson()
                    .getJSONObject("metadata")
                    .getString("query")
                    .replace(
                            "\r",
                            ""
                    );
            searchField.setText(loadedQuery);
        } catch (JSONException e) {
            Debug.error("Failed to get query for results.", e);
        }

        // Set theme
        root.getStylesheets().add(
                String.valueOf(
                        Main.class.getResource(
                                "resources/css/" + (Model.getInstance().settings.getSettingBool("dark_theme") ? "dark" : "standard") + ".css"
                        )
                )
        );

        try {
            new ProcessBuilder(Resources.getInstance().getYoutubeDlExecutable(), "--version").start();
            new ProcessBuilder(Resources.getInstance().getFfmpegExecutable(), "--version").start();
        } catch (IOException ignored) {

            downloadButtonContainer.getChildren().setAll(
                    download,
                    new ImageView(
                            new Image(
                                    Main.class.getResourceAsStream("resources/img/warning.png"),
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

        Debug.trace("Initialized results view");
    }

    @FXML
    public void download() {

        try {
            queueAdditionProgress.setVisible(true);

            download.setText("Queueing");
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
                            .getJSONArray("songs")
                            .getJSONObject(
                                    results.getItems().indexOf(results.getSelectionModel().getSelectedItem())
                            )

            );
        } catch (JSONException e) {
            Debug.warn("Error generating basic data for queue addition.");
            e.printStackTrace();
            Platform.runLater(() -> download.setDisable(true));
        }

    }

    @FXML
    public void downloadButtonCheck(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) downloadButtonCheckInternal();
    }

    @FXML
    public void searchView(Event event) {

        try {

            Parent searchView = FXMLLoader.load(Main.class.getResource("resources/fxml/search.fxml"));
            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();

            mainWindow.setScene(new Scene(searchView, mainWindow.getWidth() - Resources.getInstance().getWindowResizeWidth(), mainWindow.getHeight() - Resources.getInstance().getWindowResizeHeight()));

        } catch (IOException e) {
            Debug.error("FXML Error with search.fxml", e);
        }

    }

    @FXML
    public void newQuery(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER && (!searchField.getText().equals(loadedQuery) || modifiedResults)) {

            if (searchField.getText().length() >= 3) {

                // Search is valid to attempt
                String tempQuery = searchField.getText();
                Allmusic.search search = new Allmusic.search(tempQuery);
                loadingIndicator.setVisible(true);
                Thread resultsSearch = new Thread(() -> {
                    try {
                        search.query(Model.getInstance().settings.getSettingBool("data_saver"));
                    } catch (IOException er) {
                        defaultView("Connection issue detected, please verify connection and retry.");
                    }
                    try {
                        if (search.getResults().getJSONArray("songs").length() > 0) {
                            Platform.runLater(() -> {
                                loadingIndicator.setVisible(false);
                                loadedQuery = tempQuery;

                                Model.getInstance().search.setSearchResultsJson(search.getResults());
                                try {
                                    JSONArray songs = search.getResults().getJSONArray("songs");
                                    this.viewData = search.getResults();
                                    mainContainer.setCenter(results);
                                    results.getItems().clear();
                                    if (songs.length() > 0) {
                                        for (int i = 0; i < songs.length(); i++) {
                                            results.getItems().add(new searchResult(songs.getJSONObject(i)).getView());
                                        }
                                    } else defaultView("No Search Results Found");
                                } catch (JSONException er) {
                                    Debug.error("Failed to get search results.", er);
                                }
                            });
                        } else {
                            Platform.runLater(() -> {
                                defaultView("No Search Results Found");
                                loadingIndicator.setVisible(false);
                            });
                        }
                    } catch (JSONException er) {
                        Platform.runLater(() -> {
                            defaultView("Connection issue detected, please verify connection and retry.");
                            loadingIndicator.setVisible(false);
                        });
                    }
                }, "results-search");
                resultsSearch.setDaemon(true);
                resultsSearch.start();

            } else {
                // Inform user no results
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    defaultView("No Search Results Found");
                });
            }
        }

    }

    private void defaultView(String message) {

        Label defaultMessage = new Label(message);
        defaultMessage.getStyleClass().add("sub_title1");

        ImageView iconImage = new ImageView(
                new Image(
                        Main.class.getResourceAsStream("resources/img/song_default.png"),
                        50,
                        50,
                        true,
                        true
                )
        );


        if (Model.getInstance().settings.getSettingBool("dark_theme"))
            iconImage.setEffect(new ColorAdjust(0, 0, 1, 0));

        VBox defaultInfoContainer = new VBox(defaultMessage, iconImage);
        defaultInfoContainer.setAlignment(Pos.CENTER);
        defaultInfoContainer.setPadding(new Insets(0, 0, 40, 0));

        mainContainer.setCenter(defaultInfoContainer);

    }

    private void downloadButtonCheckInternal() {
        try {
            if (queueAdder.isDead()) throw new NullPointerException();
        } catch (NullPointerException ignored) {

            for (BorderPane searchResult: results.getItems()) searchResult.getStyleClass().setAll("result");
            results.getSelectionModel().getSelectedItems().get(0).getStyleClass().setAll("result_selected");

            Platform.runLater(() -> download.setDisable(results.getSelectionModel().getSelectedIndex() == -1));
        }
    }

    // TODO: Add network error handling
    class generateQueueItem implements Runnable {

        private final JSONObject basicData;
        private volatile boolean kill;
        private volatile boolean completed = false;

        public generateQueueItem(JSONObject basicData) {
            this.basicData = basicData;

            Thread thread = new Thread(this, "generate-queue-item");
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
            if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData())))
                if (!new File(Resources.getInstance().getApplicationData() + "cached").mkdirs())
                    Debug.error("Failed to create cache folder in app data.", new IOException());

            for (File cachedArt: Objects.requireNonNull(new File(Resources.getInstance().getApplicationData() + "cached").listFiles())) {
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
                Debug.error("Failed to parse temporary data for ID generation.", e);
            }

            // Checking in downloads history
            JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
            if (idExistsInData(downloadHistory, id))
                return generateNewSongId(downloadItems);

            // Did not match existing records, return generated ID
            return id;
        }

        private JSONArray getSource(String query, int targetTime) {

            Youtube youtubeParser = new Youtube(query, targetTime);

            try {
                youtubeParser.load();
            } catch (IOException e) {
                Debug.warn("Error connecting to https://www.youtube.com/results?search_query=" + query);
                return null;
                // TODO: Await reconnection
            }

            JSONArray searchDataExtracted = youtubeParser.getResults();
            try {

                switch (searchDataExtracted.length()) {

                    case 0:
                        // Unable to find a song which matched what we are looking for
                        Debug.warn("Youtube does not have the this song, inform user of failure.");
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
                Debug.error("Failed to sort songs data with data: " + searchDataExtracted, e);
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
                    Allmusic.song songTraceLinkLoader = new Allmusic.song(basicData.getJSONObject("view").getString("allmusicSongId"));
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
                            new File(Resources.getInstance().getApplicationData() + String.format("cached/%s.jpg", metadata.getString("artId")))
                    );
                } catch (IOException e) {
                    Debug.error("Failed to download album art.", e);
                    // TODO: Handle connection
                } catch (JSONException e) {
                    Debug.error("JSON Error when downloading album art.", e);
                }

                // Extract relevant data from the album
                Allmusic.album albumProcessor = new Allmusic.album(basicData.getJSONObject("data").getString("allmusicAlbumId"));
                albumProcessor.load();

                StringBuilder outputDirectory = new StringBuilder(Model.getInstance().settings.getSetting("output_directory"));
                if (basicData.getJSONObject("data").getBoolean("album"))
                    outputDirectory.append("/").append(albumProcessor.getAlbum());

                metadata.put("directory", outputDirectory.toString());

                metadata.put("album", albumProcessor.getAlbum());
                metadata.put("playtime", albumProcessor.getPlaytime());

                for (Allmusic.album.song song: albumProcessor.getSongs()) {

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
                Debug.error("Error in JSON processing download item.", e);
            }
            catch (IOException e) {
                Debug.warn("Connection error, attempting to reconnect.");
                // TODO:  Handle reconnection
            }

            // Restore buttons to default
            Platform.runLater(() -> {
                queueAdditionProgress.setVisible(false);

                download.setText("Download");
                downloadButtonCheckInternal();

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
                        Parent searchView = FXMLLoader.load(Main.class.getResource("resources/fxml/downloads.fxml"));
                        Stage mainWindow = (Stage) ((Node) e.getSource()).getScene().getWindow();

                        mainWindow.setScene(new Scene(searchView, mainWindow.getWidth() - Resources.getInstance().getWindowResizeWidth(), mainWindow.getHeight() - Resources.getInstance().getWindowResizeHeight()));

                    } catch (IOException er) {
                        Debug.error("FXML Error with downloads.fxml", er);
                    }
                });
                linkPart1.getStyleClass().add("sub_text");

                Platform.runLater(() -> footer.setTop(new HBox(linkPart0, linkPart1)));

                cancel.setText("Back");
                cancel.getStyleClass().set(1, "back_button");
                cancel.setOnMouseClicked(Results.this::searchView);
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

    class searchResult extends Result {

        private final JSONObject data;

        private final ContextMenu contextMenu;

        private MenuItem getAlbumSongs;
        private MenuItem hideAlbumSongs;

        private final ArrayList<MediaController> internalMediaControllers = new ArrayList<>();

        public searchResult (JSONObject data) throws JSONException {
            super(
                    null,
                    data.getJSONObject("view").getString("art"),
                    false,
                    data.getJSONObject("view").getString("title"),
                    data.getJSONObject("view").getString("artist")
            );
            setSubtext(data.getJSONObject("view").getString("meta"));

            this.data = data;
            contextMenu = new ContextMenu();

            MenuItem hide = new MenuItem("Hide");
            hide.setOnAction(e -> {
                Results.this.results.getItems().remove(view);
                Results.this.modifiedResults = true;
            });

            contextMenu.getItems().setAll(hide);

            view.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> { contextMenu.show(view, event.getScreenX(), event.getScreenY());event.consume(); });
            view.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> contextMenu.hide());

            if (Model.getInstance().settings.getSettingBool("data_saver")) {
                MenuItem informationRetrieval = new MenuItem("Retrieve Additional Information");
                informationRetrieval.setOnAction(e -> {
                    Thread externalInformationRetriever = new Thread(() -> {

                        try {
                            if (data.getJSONObject("data").getBoolean("album")) {

                                // Data is already stored
                                fetchRemoteResource(
                                        viewData
                                                .getJSONArray("songs")
                                                .getJSONObject(
                                                        Results
                                                                .this
                                                                .results
                                                                .getItems()
                                                                .indexOf(view)
                                                )
                                                .getJSONObject("data")
                                                .getString("art")
                                );

                            } else {

                                Allmusic.song songParser = new Allmusic.song(data.getJSONObject("data").getString(
                                        data.getJSONObject("data").getBoolean("album") ?
                                                "allmusicAlbumId" : "allmusicSongId"
                                ));
                                songParser.load();

                                StringBuilder subtext = new StringBuilder("Song");
                                if (!songParser.getYear().isEmpty()) {
                                    subtext.append(" | ").append(songParser.getYear());
                                }
                                if (!songParser.getGenre().isEmpty()) {
                                    subtext.append(" | ").append(songParser.getGenre());
                                }

                                Platform.runLater(() -> {
                                    fetchRemoteResource(songParser.getAlbumArt());
                                    setSubtext(subtext.toString());
                                });
                            }
                            Platform.runLater(() -> contextMenu.getItems().remove(informationRetrieval));
                        } catch (JSONException er) {
                            Debug.error("Failed to get data to extract additional information.", er);
                        } catch (IOException er) {
                            Debug.warn("Connection issue.");
                        }

                    }, "external-information-retriever");
                    externalInformationRetriever.setDaemon(true);
                    externalInformationRetriever.start();
                });
                contextMenu.getItems().add(informationRetrieval);
            }

            if (data.getJSONObject("data").getBoolean("album")) {
                hideAlbumSongs = new MenuItem("Hide Songs");
                getAlbumSongs = new MenuItem("View Songs");

                hideAlbumSongs.setOnAction(this::hideResult);
                getAlbumSongs.setOnAction(this::getAlbumSongs);

                contextMenu.getItems().add(getAlbumSongs);
            }
        }

        private void hideResult(ActionEvent event) {

            view.setBottom(null);
            contextMenu.getItems().remove(hideAlbumSongs);
            contextMenu.getItems().add(getAlbumSongs);
            internalMediaControllers.forEach(Results.this.mediaPlayers::remove);

        }

        private void getAlbumSongs(ActionEvent event) {

            try {
                Allmusic.album albumParser = new Allmusic.album(data.getJSONObject("data").getString("allmusicAlbumId"));
                Thread albumLoader = new Thread(() -> {
                    try {
                        albumParser.load();

                        VBox songResults = new VBox();

                        for (Allmusic.album.song song: albumParser.getSongs()) {

                            MediaController songController = new MediaController(song);
                            mediaPlayers.add(songController);
                            internalMediaControllers.add(songController);
                            songResults.getChildren().add(songController.getView());

                        }

                        Platform.runLater(() -> view.setBottom(songResults));
                        contextMenu.getItems().remove(getAlbumSongs);
                        contextMenu.getItems().add(hideAlbumSongs);

                    } catch (IOException er) {
                        Debug.warn("Connection failure detected.");
                    }
                }, "album-loader");
                albumLoader.setDaemon(true);
                albumLoader.start();

            } catch (JSONException er) {
                Debug.error("lol", er);
            }

        }

        protected class MediaController {

            private BorderPane view;
            private final Allmusic.album.song song;
            private final MediaPlayer mp;

            private final HBox playIconContainer = new HBox();
            private final ImageView playIcon = new ImageView(
                    new Image(
                            Main.class.getResourceAsStream("resources/img/play.png"),
                            20,
                            20,
                            true,
                            true
                    )
            );

            public MediaController(Allmusic.album.song song) {
                this.song = song;
                this.mp = new MediaPlayer(
                        new Media(
                                String.format(Resources.mp3Source, song.getSample())
                        )
                );
                mp.setOnEndOfMedia(() -> {
                    Debug.trace(String.format("Finished Playing '%s'", song.getTitle()));
                    mp.seek(Duration.millis(0));
                    pause();
                });
            }

            public BorderPane getView() {
                buildView();
                return view;
            }

            private void play(MouseEvent event) {
                Debug.trace(
                        String.format(
                                "Playing '%s' from %.2fs of 30s (%.2f%%)",
                                song.getTitle(),
                                mp.getCurrentTime().toSeconds(),
                                (mp.getCurrentTime().toSeconds() / 30) * 100
                        )
                );
                if (event.getButton().equals(MouseButton.PRIMARY)) play();
                event.consume();
            }

            private void pause(MouseEvent event) {
                Debug.trace(
                        String.format(
                                "Paused '%s' at %.2fs of 30s (%.2f%%)",
                                song.getTitle(),
                                mp.getCurrentTime().toSeconds(),
                                (mp.getCurrentTime().toSeconds() / 30) * 100
                        )
                );
                if (event.getButton().equals(MouseButton.PRIMARY)) pause();
                event.consume();
            }

            private void play() {
                Results.this.mediaPlayers.forEach(MediaController::pause);
                playIcon.setImage(
                        new Image(
                                Main.class.getResourceAsStream("resources/img/paused.png"),
                                20,
                                20,
                                true,
                                true
                        )
                );
                mp.play();
                playIconContainer.setOnMouseClicked(this::pause);
            }

            private void pause() {
                playIcon.setImage(
                        new Image(
                                Main.class.getResourceAsStream("resources/img/play.png"),
                                20,
                                20,
                                true,
                                true
                        )
                );
                mp.pause();
                playIconContainer.setOnMouseClicked(this::play);

            }

            private void buildView() {

                view = new BorderPane();
                view.setPadding(new Insets(5, 0, 5, 89));

                HBox songResult = new HBox();
                songResult.setSpacing(5);

                Label songTitleView = new Label(song.getTitle());
                songTitleView.getStyleClass().add("sub_text");

                // This would be much easier to work into a class, will need to switch back and forth, consider a thin progress bar or something
                if (!song.getSample().isEmpty()) {

                    if (Model.getInstance().settings.getSettingBool("dark_theme"))
                        playIcon.setEffect(new ColorAdjust(0, 0, 1, 0));

                    playIconContainer.getChildren().setAll(playIcon);
                    playIconContainer.setOnMouseClicked(this::play);
                    playIconContainer.setCursor(Cursor.HAND);

                    songResult.getChildren().add(playIconContainer);

                }
                songResult.getChildren().add(songTitleView);
                view.setLeft(songResult);

            }
        }
    }
}
