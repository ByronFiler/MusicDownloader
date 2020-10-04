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
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.net.db.sites.Allmusic;
import musicdownloader.utils.net.source.sites.Vimeo;
import musicdownloader.utils.net.source.sites.YouTube;
import musicdownloader.utils.ui.Result;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

// TODO: Switching view should pause music playing

public class Results {

    @FXML
    private AnchorPane root;
    @FXML
    private BorderPane mainContainer;

    @FXML
    private BorderPane offlineNotification;

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
    private boolean queueAdditionInProgress = false;
    private String loadedQuery;
    private boolean modifiedResults;
    private JSONObject viewData;

    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("resources.locale.results");
    private final ArrayList<searchResult.MediaController> mediaPlayers = new ArrayList<>();

    @FXML
    private void initialize() {

        Model.getInstance().connectionWatcher.switchMode(this);

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
                        getClass().getClassLoader().getResource(
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
                                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("resources/img/warning.png")),
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
            queueAdditionInProgress = true;
            queueAdditionProgress.setVisible(true);

            download.setText(resourceBundle.getString("queueingMessage"));
            download.setDisable(true);

            cancel.setText(resourceBundle.getString("cancelButton"));
            cancel.setOnMouseClicked(e -> {
                queueAdder.kill();
                restoreView(false);
            });

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
            (
                    ((Node) event.getSource())
                            .getScene()
                            .getWindow()
            )
                    .getScene()
                    .setRoot(
                            FXMLLoader.load(
                                    Objects.requireNonNull(getClass().getClassLoader().getResource("resources/fxml/search.fxml")),
                                    ResourceBundle.getBundle("resources.locale.search")
                            )
                    );
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
                Allmusic.Search search = new Allmusic.Search(tempQuery);
                loadingIndicator.setVisible(true);
                Thread resultsSearch = new Thread(() -> {
                    try {
                        search.query(Model.getInstance().settings.getSettingBool("data_saver"));

                        if (!Model.getInstance().settings.getSettingBool("data_saver"))
                            search.getSongExternalInformation();

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
                                        download.setDisable(true);
                                    } else defaultView("No Search Results Found");

                                    downloadButtonCheckInternal();
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

    public void setDisabled(boolean disable) {

        results.setDisable(disable);
        searchField.setDisable(disable);
        download.setDisable(disable);

    }

    public BorderPane getOfflineNotification() {
        return offlineNotification;
    }

    private void defaultView(String message) {

        Label defaultMessage = new Label(message);
        defaultMessage.getStyleClass().add("sub_title1");

        ImageView iconImage = new ImageView(
                new Image(
                        Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("resources/img/song_default.png")),
                        50,
                        50,
                        true,
                        true
                )
        );


        if (Model.getInstance().settings.getSettingBool("dark_theme")) {
            iconImage.setEffect(new ColorAdjust(0, 0, 1, 0));
        }

        VBox defaultInfoContainer = new VBox(defaultMessage, iconImage);
        defaultInfoContainer.setAlignment(Pos.CENTER);
        defaultInfoContainer.setPadding(new Insets(0, 0, 40, 0));

        mainContainer.setCenter(defaultInfoContainer);

    }

    private void downloadButtonCheckInternal() {
        if (!queueAdditionInProgress) {

            if (results.getSelectionModel().getSelectedItems().size() > 0) {
                for (BorderPane searchResult : results.getItems()) searchResult.getStyleClass().setAll("result");
                results.getSelectionModel().getSelectedItems().get(0).getStyleClass().setAll("result_selected");
            }

            Platform.runLater(() -> download.setDisable(results.getSelectionModel().getSelectedIndex() == -1));
        }
    }

    private void restoreView(boolean displayLink) {

        queueAdditionProgress.setVisible(false);

        download.setText(resourceBundle.getString("downloadButton"));
        downloadButtonCheckInternal();

        if (displayLink) {
            Label linkPart0;
            if (Model.getInstance().download.getDownloadQueue().length() > 0) {
                linkPart0 = new Label(String.format(resourceBundle.getString("addedToQueueMessage"), Model.getInstance().download.getDownloadQueue().length()));
            } else linkPart0 = new Label(resourceBundle.getString("downloadStartedMessage"));

            linkPart0.getStyleClass().add("sub_text");
            Label linkPart1 = new Label(resourceBundle.getString("downloadsTitlePart"));
            linkPart1.setUnderline(true);
            linkPart1.setCursor(Cursor.HAND);
            linkPart1.setOnMouseClicked(e -> {
                try {
                    FXMLLoader downloadsLoader = new FXMLLoader(
                            getClass().getClassLoader().getResource("resources/fxml/downloads.fxml"),
                            ResourceBundle.getBundle("resources.locale.downloads")
                    );
                    Parent controllerView = downloadsLoader.load();
                    Model.getInstance().download.setDownloadsView(downloadsLoader.getController());

                    (
                            ((Node) e.getSource())
                                    .getScene()
                                    .getWindow()
                    )
                            .getScene()
                            .setRoot(
                                    controllerView
                            );
                } catch (IOException er) {
                    Debug.error("FXML Error with downloads.fxml", er);
                }
            });
            linkPart1.getStyleClass().add("sub_text");

            HBox downloadsLinkContainer = new HBox(linkPart0, linkPart1);
            downloadsLinkContainer.setSpacing(5);

            footer.setTop(downloadsLinkContainer);
        }

        cancel.setText(resourceBundle.getString("backButton"));
        cancel.setOnMouseClicked(Results.this::searchView);

        results.setDisable(false);
        download.setDisable(false);

        queueAdditionInProgress = false;

    }

    class generateQueueItem implements Runnable {

        private final JSONObject basicData;
        private volatile boolean kill;

        private int completedThreads = 0;
        private int requiredThreadsCompleted = -1;

        private final JSONObject downloadItem = new JSONObject();
        private final JSONObject metadata = new JSONObject();
        private final JSONArray songs = new JSONArray();

        public generateQueueItem(JSONObject basicData) {
            this.basicData = basicData;

            Thread thread = new Thread(this, "generate-queue-item");
            thread.setDaemon(true);
            thread.start();
        }

        public void kill() {
            kill = true;
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

        private JSONObject parseJsonFromSong(JSONArray collectiveDownloadsObjects, Allmusic.Album albumProcessor, Allmusic.Album.song song) throws JSONException {
            JSONObject jSong = new JSONObject();

            jSong.put("position", albumProcessor.getSongs().indexOf(song) + 1);
            jSong.put("id", generateNewSongId(collectiveDownloadsObjects));
            jSong.put("completed", JSONObject.NULL);
            jSong.put("playtime", song.getPlaytime());
            jSong.put("title", song.getTitle());
            jSong.put("sample", song.getSample() == null ? JSONObject.NULL : song.getSample());

            return jSong;
        }

        private synchronized void markThreadCompleted(int position, JSONArray results) throws JSONException {

            completedThreads++;
            songs.getJSONObject(position).put("source", results);

            if (completedThreads == requiredThreadsCompleted) {

                if (!kill) {

                    downloadItem.put("metadata", metadata);
                    downloadItem.put("songs", songs);

                    Model.getInstance().download.updateDownloadQueue(downloadItem);

                }

                Platform.runLater(() -> restoreView(!kill));
            }

        }

        @Override
        public void run() {
            try {

                // Getting other download items for the purpose of ID validation
                JSONArray collectiveDownloadsObjects = new JSONArray();
                if (Model.getInstance().download.getDownloadQueue().length() > 0)
                    for (int i = 0; i < Model.getInstance().download.getDownloadQueue().length(); i++)
                        collectiveDownloadsObjects.put(Model.getInstance().download.getDownloadQueue().getJSONObject(i));

                if (Model.getInstance().download.getDownloadObject().length() > 0)
                    collectiveDownloadsObjects.put(Model.getInstance().download.getDownloadObject());

                // Metadata: Shared
                metadata.put("artId", generateNewCacheArtId(collectiveDownloadsObjects));
                metadata.put("artist", basicData.getJSONObject("data").getString("artist"));

                // Additional data revealed either by query or additional request already sent
                if (basicData.getJSONObject("data").getBoolean("album") || !Model.getInstance().settings.getSettingBool("data_saver") || !basicData.getJSONObject("view").has("allmusicSongId")) {

                    metadata.put("art", basicData.getJSONObject("data").getString("art"));
                    metadata.put("year", basicData.getJSONObject("data").getString("year"));
                    metadata.put("genre", basicData.getJSONObject("data").getString("genre"));

                } else {

                    // Additional connection required to get album id & metadata
                    Allmusic.Song songTraceLinkLoader = new Allmusic.Song(basicData.getJSONObject("view").getString("allmusicSongId"));
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
                } catch (UnknownHostException | SocketException e) {
                    Debug.warn("Detected connection failure when downloading album art, awaiting reconnection...");
                    restoreView(false);
                    setDisabled(true);
                    return;

                } catch (IOException e) {
                    Debug.error("Failed to download album art.", e);
                } catch (JSONException e) {
                    Debug.error("JSON Error when downloading album art.", e);
                }

                // Extract relevant data from the album
                Allmusic.Album albumProcessor = new Allmusic.Album(basicData.getJSONObject("data").getString("allmusicAlbumId"));
                albumProcessor.load();

                requiredThreadsCompleted = albumProcessor.getSongs().size();
                StringBuilder outputDirectory = new StringBuilder(Model.getInstance().settings.getSetting("output_directory"));
                if (basicData.getJSONObject("data").getBoolean("album"))
                    outputDirectory.append("/").append(albumProcessor.getAlbum());

                metadata.put("directory", outputDirectory.toString());

                metadata.put("album", albumProcessor.getAlbum());
                metadata.put("playtime", albumProcessor.getPlaytime());
                metadata.put("is_album", basicData.getJSONObject("data").getBoolean("album"));
                metadata.put("format", Resources.songReferences.get(Model.getInstance().settings.getSettingInt("music_format")));

                if (basicData.getJSONObject("data").getBoolean("album")) {
                    for (Allmusic.Album.song song : albumProcessor.getSongs()) {
                        songs.put(
                                parseJsonFromSong(
                                        collectiveDownloadsObjects,
                                        albumProcessor,
                                        song
                                )
                        );
                        new GetSources(
                                albumProcessor.getSongs().indexOf(song),
                                metadata.get("artist") + " " + song.getTitle(),
                                song.getPlaytime()
                        );
                    }

                } else {
                    Allmusic.Album.song song = albumProcessor
                            .getSongs()
                            .get(
                                    Arrays.asList(
                                            albumProcessor
                                                    .getSongs()
                                                    .stream()
                                                    .map(
                                                            Allmusic.Album.song::getTitle
                                                    ).toArray()
                                    ).indexOf(
                                            basicData
                                                    .getJSONObject("view")
                                                    .getString("title")
                                    )
                            );
                    songs.put(
                            parseJsonFromSong(
                                    collectiveDownloadsObjects,
                                    albumProcessor,
                                    song
                            )
                    );
                    new GetSources(
                            0,
                            metadata.get("artist") + " " + song.getTitle(),
                            song.getPlaytime()
                    );
                }

            } catch (JSONException e) {
                Debug.error("Error in JSON processing download item.", e);
            }
            catch (IOException e) {
                Debug.warn("Connection error, attempting to reconnect.");
            }
        }

        private class GetSources implements Runnable {

            private final int position;
            private final String query;
            private final int targetTime;

            private JSONObject youtubeResponses = null;
            private JSONObject vimeoResponses = null;

            public GetSources(int position, String query, int targetTime) {

                this.position = position;
                this.query = query;
                this.targetTime = targetTime;

                Thread executor = new Thread(this, "source-getter");
                executor.setDaemon(true);
                executor.start();

            }

            @Override
            public void run() {

                new Thread(() -> {
                    try {
                        YouTube youtubeParser = new YouTube(query, targetTime);
                        youtubeParser.load();

                        youtubeResponses = youtubeParser.getResults();
                        compile();

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }

                }, "youtube-parser").start();

                new Thread(() -> {
                    try {
                        Vimeo vimeoParser = new Vimeo(query, targetTime);
                        vimeoParser.load();

                        vimeoResponses = vimeoParser.getResults();
                        compile();

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }

                }, "vimeo-parser").start();

            }

            private synchronized void compile() {

                if (youtubeResponses != null && vimeoResponses != null) {

                    JSONArray sources = new JSONArray();

                    try {
                        for (int i = 0; i < youtubeResponses.getJSONArray("primary").length(); i++)
                            sources.put(Resources.youtubeVideoSource + youtubeResponses.getJSONArray("primary").getString(i));
                        for (int i = 0; i < vimeoResponses.getJSONArray("primary").length(); i++)
                            sources.put(Resources.vimeoVideoSource + vimeoResponses.getJSONArray("primary").getString(i));
                        for (int i = 0; i < youtubeResponses.getJSONArray("secondary").length(); i++)
                            sources.put(Resources.youtubeVideoSource + youtubeResponses.getJSONArray("secondary").getString(i));
                        for (int i = 0; i < vimeoResponses.getJSONArray("secondary").length(); i++)
                            sources.put(Resources.vimeoVideoSource + vimeoResponses.getJSONArray("secondary").getString(i));

                        generateQueueItem.this.markThreadCompleted(position, sources);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }

        }

    }

    class searchResult extends Result {

        private MenuItem getAlbumSongs;
        private MenuItem hideAlbumSongs;

        private Allmusic.Album albumParser;
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

            MenuItem hide = new MenuItem(resourceBundle.getString("hideItem"));
            hide.setOnAction(e -> {
                Results.this.results.getItems().remove(view);
                Results.this.modifiedResults = true;
            });

            menu.getItems().setAll(hide);

            if (Model.getInstance().settings.getSettingBool("data_saver")) {
                MenuItem informationRetrieval = new MenuItem(resourceBundle.getString("retrieveAdditionalInformationItem"));
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

                                Allmusic.Song songParser = new Allmusic.Song(data.getJSONObject("data").getString(
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
                            Platform.runLater(() -> menu.getItems().remove(informationRetrieval));
                        } catch (JSONException er) {
                            Debug.error("Failed to get data to extract additional information.", er);
                        } catch (IOException er) {
                            Debug.warn("Connection issue.");
                        }

                    }, "external-information-retriever");
                    externalInformationRetriever.setDaemon(true);
                    externalInformationRetriever.start();
                });
                menu.getItems().add(informationRetrieval);
            }

            if (data.getJSONObject("data").getBoolean("album")) {

                albumParser = new Allmusic.Album(data.getJSONObject("data").getString("allmusicAlbumId"));

                hideAlbumSongs = new MenuItem(resourceBundle.getString("hideSongsItem"));
                getAlbumSongs = new MenuItem(resourceBundle.getString("showSongsItem"));

                hideAlbumSongs.setOnAction(this::hideResult);
                getAlbumSongs.setOnAction(this::getAlbumSongs);

                menu.getItems().add(getAlbumSongs);
            }
        }

        private void hideResult(ActionEvent event) {

            view.setBottom(null);
            menu.getItems().remove(hideAlbumSongs);
            menu.getItems().add(getAlbumSongs);
            internalMediaControllers.forEach(Results.this.mediaPlayers::remove);

        }

        private void getAlbumSongs(ActionEvent event) {

            VBox songResults = new VBox();

            if (albumParser.getIsLoaded()) generateMediaControllers(songResults);
            else {
                Thread albumLoader = new Thread(() -> {
                    try {
                        albumParser.load();
                        generateMediaControllers(songResults);

                    } catch (IOException er) {
                        Debug.warn("Connection failure detected.");
                    }
                }, "album-loader");
                albumLoader.setDaemon(true);
                albumLoader.start();
            }


        }

        private void generateMediaControllers(VBox songResults) {
            for (Allmusic.Album.song song: albumParser.getSongs()) {
                MediaController songController = new MediaController(song);
                mediaPlayers.add(songController);
                internalMediaControllers.add(songController);
                Platform.runLater(() -> songResults.getChildren().add(songController.getView()));
            }

            Platform.runLater(() -> {
                view.setBottom(songResults);
                menu.getItems().remove(getAlbumSongs);
                menu.getItems().add(hideAlbumSongs);
            });
        }

        protected class MediaController {

            private BorderPane view;
            private final Allmusic.Album.song song;
            private final MediaPlayer mp;

            private final HBox playIconContainer = new HBox();
            private final ImageView playIcon = new ImageView(
                    new Image(
                            Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("resources/img/play.png")),
                            20,
                            20,
                            true,
                            true
                    )
            );

            public MediaController(Allmusic.Album.song song) {
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
                                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("resources/img/paused.png")),
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
                                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("resources/img/play.png")),
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
                if (song.getSample() != null) {

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
