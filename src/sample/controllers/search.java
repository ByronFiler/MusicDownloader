package sample.controllers;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sample.Debug;
import sample.Model;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

// TODO
// Error message is not centered and is out of place
// Losing connection mid-search generates a partially completed results table, don't let this happen
// Continue testing connection drops: Control Panel\Network and Internet\Network Connections
// Search data could also theoretically load songs since the request is already sent to save sending the requests again decreasing load speeds and web requests
// Internal logic to search isn't great with error handling

public class search {

    @FXML private AnchorPane root;

    @FXML private VBox searchContainer;
    @FXML private TextField search;
    @FXML private ProgressIndicator loadingIcon;
    @FXML private Text errorMessage;
    @FXML private ListView<HBox> autocompleteResults;

    @FXML private ImageView downloads;
    @FXML private ImageView settings;

    // Timer timerRotate;
    private Timer hideErrorMessage;

    private generateAutocomplete autoCompleteThread;
    private allMusicQuery searchThread;

    @FXML
    private void initialize(){

        // Theoretically no way this could change via normal use of the program, but if user starts a download, waits for it to finish and clears file, downloads page needs a check to prevent
        if (Model.getInstance().download.downloadsAccessible())
            downloads.setVisible(true);

        // Loading in CSS
        if (Model.getInstance().settings.getSettingBool("dark_theme")) {

            root.getStylesheets().add(
                    String.valueOf(getClass().getResource("../app/css/dark/search.css"))
            );

            ColorAdjust invert = new ColorAdjust();
            invert.setBrightness(1);

            downloads.setEffect(invert);
            settings.setEffect(invert);
        }
        else
            root.getStylesheets().add(
                    String.valueOf(getClass().getResource("../app/css/standard/search.css"))
            );

        Debug.trace(null, "Initialized search view.");
    }

    @FXML
    private void downloadsView(Event event) {
        try {
            Parent settingsView = FXMLLoader.load(getClass().getResource("../app/fxml/downloads.fxml"));

            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(settingsView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

        } catch(IOException e) {
            Debug.error(null, "FXML Error: downloads.fxml", e.getCause());
        }
    }

    @FXML
    private void settingsView(Event event) {

        try {
            Parent settingsView = FXMLLoader.load(getClass().getResource("../app/fxml/settings.fxml"));

            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(settingsView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

        } catch(IOException e) {
            Debug.error(null, "Missing FXML File: Settings.fxml", e.getCause());
        }

    }

    @FXML
    private void searchRequest(KeyEvent e) {

        char[] newCharacter = e.getText().toCharArray();

        if (newCharacter.length > 0) {

            if (Character.isLetterOrDigit(newCharacter[0])) {

                if (search.getText().length() > 0) {
                    // Kill previous & spawn a new autocomplete thread
                    try {autoCompleteThread.kill();} catch (NullPointerException ignored) {}
                    autoCompleteThread = new generateAutocomplete(search.getText() + e.getText());
                }

            } else if (e.getCode() == KeyCode.ENTER) {

                if (search.getText().length() > 1) {

                    // Starting the new search thread
                    try {
                        if (searchThread.working())
                            error("A Search is already in progress, please wait.");
                        else
                            throw new NullPointerException();
                    } catch (NullPointerException ignored) {

                        // Start a new search
                        searchThread = new allMusicQuery(e, search.getText() + e.getText());
                        loadingIcon.setVisible(true);

                    }

                } else
                    error("Query is too short, no results found.");

            }

        } else if (search.getText().length() < 3) {

            // Kill all running autocomplete threads
            try {
                autoCompleteThread.kill();
                autocompleteResults.getItems().clear();
                autocompleteResults.setVisible(false);
            } catch (NullPointerException ignored) {}

        }
    }

    private void error(String message) {

        autocompleteResults.getItems().clear();
        autocompleteResults.setVisible(false);

        // Repositioning directly below search bar, taking autocomplete space
        searchContainer.getChildren().remove(errorMessage);
        searchContainer.getChildren().add(2, errorMessage);

        errorMessage.setText(message);
        errorMessage.setVisible(true);

        try {
            hideErrorMessage.cancel();
        } catch (NullPointerException ignored) {}

        hideErrorMessage = new Timer();
        hideErrorMessage.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    errorMessage.setVisible(false);
                    searchContainer.getChildren().remove(errorMessage);
                    searchContainer.getChildren().add(errorMessage);
                });
            }
        }, 2000);

    }

    // Generating the full data for the search data
    private class allMusicQuery implements Runnable {

        private final Thread thread;
        private final String query;
        private final KeyEvent event;

        allMusicQuery (KeyEvent event, String query){
            this.query = query;
            this.event = event;

            thread = new Thread(this, "query");
            thread.start();
        }

        public boolean working() {
            return thread.isAlive();
        }

        public void run() {
            Document doc;

            // Attempting connection
            try {
                doc = Jsoup.connect("https://www.allmusic.com/search/all/" + query).get();
            } catch (IOException e) {

                Platform.runLater(() -> {
                    // timerRotate.cancel();
                    loadingIcon.setVisible(false);

                    Debug.warn(thread, "Failed to connect to https://www.allmusic.com/search/all/" + query);
                    new awaitReconnection();
                });
                return;
            }

            // Extracting basic data from search
            JSONArray searchData = new JSONArray();

            for (Element result: doc.select("ul.search-results").select("li"))
            {

                // Check that it's either a album or an song, not an artist, the data is a bit odd so the hashcode fixes it
                if (result.select("h4").text().hashCode() != 1969736551 && result.select("h4").text().hashCode() != 73174740) {
                    JSONObject resultData = new JSONObject();

                    // Handling The Title, If it's a song it has "" surround it, which it to be removed
                    try {
                        // Title
                        resultData.put("title", result.select("div.title").text().replaceAll("\"", ""));

                        // Artist
                        resultData.put("artist", result.select("h4").text().hashCode() == 2582837 ? result.select("div.performers").select("a").text() : result.select("div.artist").text());

                        // Year (Albums Only)
                        resultData.put("year", result.select("div.year").text());

                        // Genre (Albums Only)
                        resultData.put("genre", result.select("div.genres").text());

                        // Type
                        resultData.put("album", result.select("div.cover").size() > 0);

                        // Art (Albums Only)
                        if (result.select("div.cover").size() > 0) {
                            String potentialAlbumArt = result.select("img.lazy").attr("data-original");
                            resultData.put("art", potentialAlbumArt.isEmpty() ? new File(getClass().getResource("../app/img/album_default.png").getPath()).toURI().toString() : potentialAlbumArt);
                        }

                        // Link
                        resultData.put("link", result.select("div.title").select("a").attr("href"));

                        searchData.put(resultData);

                    } catch (JSONException e) {
                        Debug.error(thread, "Failed to process search request JSON for https://www.allmusic.com/search/all/" + query, e.getCause());
                    }
                }
            }

            if (searchData.length() > 0) {

                BorderPane[] tableData = new BorderPane[searchData.length()];

                // Adding remaining data & preparing data for table
                for (int i = 0; i < searchData.length(); i++) {

                    // Gathering additional data for JSON object
                    try {
                        if (!searchData.getJSONObject(i).getBoolean("album")) {

                            if (!Model.getInstance().settings.getSettingBool("data_saver")) {

                                // Finding: Album Art Link, Year and Genre

                                // Reading information page
                                Document songDataPage = Jsoup.connect(searchData.getJSONObject(i).getString("link")).get();

                                // Album Art
                                try {
                                    String potentialAlbumArt = songDataPage.selectFirst("td.cover").selectFirst("img").attr("src") != null && songDataPage.selectFirst("td.cover").selectFirst("img").attr("src").startsWith("https") && !songDataPage.selectFirst("td.cover").selectFirst("img").attr("src").equals("https://cdn-gce.allmusic.com/images/lazy.gif") ? songDataPage.selectFirst("td.cover").selectFirst("img").attr("src") : getClass().getResource("../app/img/song_default.png").toURI().toString();
                                    searchData.getJSONObject(i).put("art", potentialAlbumArt.isEmpty() ? new File(getClass().getResource("../app/img/song_default.png").getPath()).toURI().toString() : potentialAlbumArt);
                                } catch (NullPointerException ignored) {
                                    searchData.getJSONObject(i).put("art", new File(getClass().getResource("../app/img/song_default.png").getPath()).toURI().toString());
                                } catch (URISyntaxException e) {
                                    Debug.error(null, "URI Formation exception loading song default image.", e.getCause());
                                }

                                // Year
                                try {
                                    if (songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear").length() == 4)
                                        searchData.getJSONObject(i).put("year", songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear"));
                                } catch (NullPointerException ignored) {}

                                // Genre
                                try {
                                    searchData.getJSONObject(i).put(
                                            "genre",
                                            songDataPage
                                                    .selectFirst("div.song_genres")
                                                    .selectFirst("div.middle")
                                                    .selectFirst("a")
                                                    .text()
                                                    .split("\\(")[0]
                                                    .substring(
                                                            0,
                                                            songDataPage
                                                                    .selectFirst("div.song_genres")
                                                                    .selectFirst("div.middle")
                                                                    .selectFirst("a")
                                                                    .text()
                                                                    .split("\\(")[0]
                                                                    .length() - 1
                                                    )
                                    );
                                } catch (NullPointerException ignored) {}
                            } else {
                                searchData.getJSONObject(i).put("art", getClass().getResource("../app/img/song_default.png"));
                            }

                        }
                    } catch (JSONException e) {
                        Debug.error(thread, "Failed to process found search results.", e.getCause());
                    } catch (IOException e) {
                        try {
                            Debug.warn(thread, "Connection error on connecting to: " + searchData.getJSONObject(i).getString("link"));
                        } catch (JSONException er) {
                            Debug.error(thread, "Failed to process found search results.", e.getCause());
                        }
                    }

                    // Add as processed element to table data
                    try {

                        BorderPane searchResult = new BorderPane();
                        HBox left = new HBox();

                        ImageView albumArt = new ImageView(
                                new Image(
                                        searchData.getJSONObject(i).getString("art"),
                                        75,
                                        75,
                                        true,
                                        true
                                )
                        );

                        BorderPane textInfo = new BorderPane();

                        Label title = new Label(searchData.getJSONObject(i).getString("title"));
                        title.getStyleClass().add("resultTitle");

                        Label artist = new Label(searchData.getJSONObject(i).getString("artist"));
                        artist.getStyleClass().add("resultArtist");

                        VBox songArtistContainer = new VBox(title, artist);

                        StringBuilder metaInfoRaw = new StringBuilder(searchData.getJSONObject(i).getBoolean("album") ? "Album" : "Song");

                        if (!searchData.getJSONObject(i).getString("year").isEmpty())
                            metaInfoRaw.append(" | ").append(searchData.getJSONObject(i).getString("year"));

                        if (!searchData.getJSONObject(i).getString("genre").isEmpty())
                            metaInfoRaw.append(" | ").append(searchData.getJSONObject(i).getString("genre"));

                        Label metaInfo = new Label(metaInfoRaw.toString());
                        metaInfo.getStyleClass().add("resultMeta");

                        textInfo.setTop(songArtistContainer);
                        textInfo.setBottom(metaInfo);

                        textInfo.setPadding(new Insets(0, 0, 0, 5));

                        left.getChildren().setAll(albumArt, textInfo);

                        searchResult.setLeft(left);
                        searchResult.getStyleClass().add("resultContainer");

                        tableData[i] = searchResult;

                    } catch (JSONException | IllegalArgumentException e) {
                        e.printStackTrace();
                        Debug.error(thread, "Failed to generate table result", e.getCause());
                    }
                }

                // Sending processed data to the model
                Model.getInstance().search.setSearchResults(tableData);
                Model.getInstance().search.setSearchResultsJson(searchData);

                // Changing scene to results-view
                try {
                    Parent resultsView = FXMLLoader.load(getClass().getResource("../app/fxml/results.fxml"));
                    Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();

                    Platform.runLater(() -> mainWindow.setScene(new Scene(resultsView, mainWindow.getWidth()-16, mainWindow.getHeight()-39)));

                } catch (IOException e) {
                    Debug.error(null, "FXML Error: Settings.fxml", e.getCause());
                }

            } else {

                Debug.trace(thread, "No search results found for query: " + query);
                Platform.runLater(() -> {
                    loadingIcon.setVisible(false);
                    error("No Search Results Found");
                });

            }

        }

    }

    // Updating the UI with the autocomplete suggestions
    private class generateAutocomplete implements Runnable {

        private final Thread thread;
        private volatile boolean killRequest = false;
        private final String query;

        generateAutocomplete (String query){
            this.query = query;

            thread = new Thread(this, "autocomplete");
            thread.start();
        }

        public void kill() {
            killRequest = true;
        }

        public void run() {

            if (Model.getInstance().settings.getSettingBool("data_saver"))
                return;

            try {
                Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + query).get();
                ArrayList<HBox> autocompleteResultsView = new ArrayList<>();
                Elements results = doc.select("ul.search-results").select("li");
                JSONArray autocompleteDataRaw = new JSONArray();

                for (Element result: results)
                {

                    try {
                        JSONObject searchResultRaw = new JSONObject(String.format("{\"album\": %s, \"title\": \"%s\"}", result.select("div.cover").size() > 0, result.select("div.title").text().replaceAll("\"", "")));

                        // Check that it's either a album or an song, not an artist, the data is a bit odd so the hashcode fixes it
                        if (result.select("h4").text().hashCode() != 1969736551 && result.select("h4").text().hashCode() != 73174740 && !autocompleteDataRaw.toString().contains(searchResultRaw.toString())) {
                            autocompleteDataRaw.put(searchResultRaw);

                            Label resultTitle = new Label(result.select("div.title").text().replaceAll("\"", ""));
                            resultTitle.getStyleClass().add("autocompleteResult");

                            ImageView resultIcon = new ImageView(
                                    new Image(
                                            getClass().getResource(result.select("div.cover").size() > 0 ? "../app/img/album_default.png" : "../app/img/song_default.png").toURI().toString(),
                                            25,
                                            25,
                                            true,
                                            true
                                    )
                            );

                            if (Model.getInstance().settings.getSettingBool("dark_theme"))
                                resultIcon.setEffect(new ColorAdjust(0, 0, 1, 0));

                            HBox autocompleteResultView = new HBox(10, resultIcon, resultTitle);
                            autocompleteResultView.setOnMouseClicked(e -> search.setText( ((Label) (autocompleteResultView.getChildren().get(1))) .getText()));
                            autocompleteResultView.setCursor(Cursor.HAND);

                            autocompleteResultsView.add(autocompleteResultView);

                        }
                    } catch (JSONException e) {
                        Debug.warn(Thread.currentThread(), "Failed to parse JSON: " + String.format("{\"album\": %s, \"title\": \"%s\"}", result.select("div.cover").size() > 0, result.select("div.title").text().replaceAll("\"", "")));
                    }
                }

                // Add generated data to the search query
                if (!killRequest && !search.getText().isEmpty())
                    Platform.runLater(() -> {
                        autocompleteResults.getItems().setAll(autocompleteResultsView);
                        autocompleteResults.setVisible(true);
                    });

                if (search.getText().isEmpty())
                    Platform.runLater(() -> {
                        autocompleteResults.getItems().clear();
                        autocompleteResults.setVisible(false);
                    });

            } catch (SocketException | UnknownHostException ignored) {

                // Failed to connect, handling
                Debug.warn(thread, "Error sending web request: https://www.allmusic.com/search/all/" + query);
                Platform.runLater(() -> {
                    search.setDisable(true);
                    new awaitReconnection();
                });

            } catch (HttpStatusException ignored) {
            } catch (URISyntaxException | IOException e) {
                Debug.error(Thread.currentThread(), "Unknown exception when requesting user search.", e.getCause());
            }

        }

    }

    // Check that when internet connection is lost, they must reconnect before doing anything else
    private class awaitReconnection implements Runnable {

        public awaitReconnection() {
            Thread thread = new Thread(this, "reconnection");
            thread.start();
        }

        public void run() {

            new Timer().schedule(new TimerTask() {

                @Override
                public void run() {

                    Platform.runLater(() -> {
                        autocompleteResults.getItems().clear();
                        autocompleteResults.setVisible(false);

                        searchContainer.getChildren().remove(errorMessage);
                        searchContainer.getChildren().add(2, errorMessage);

                        errorMessage.setText("Failed to connect, check internet access, awaiting reconnection...");
                        errorMessage.setVisible(true);
                    });

                    try {

                        if (InetAddress.getByName("allmusic.com").isReachable(1000)) {

                            Platform.runLater(() -> {
                                search.setDisable(false);
                                errorMessage.setVisible(false);

                                searchContainer.getChildren().remove(errorMessage);
                                searchContainer.getChildren().add(errorMessage);
                            });
                            Debug.trace(null, "Connection reestablished.");
                            this.cancel();

                        }

                    } catch (IOException ignored) {}

                }

            }, 0, 1000);
        }

    }

}