package sample;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

// TODO
// Switch settings and downloads to the icons and animate them depending on what is happening
// Error message is not centered and is out of place
// Losing connection mid-search generates a partially completed results table, don't let this happen

public class search {

    @FXML private AnchorPane root;

    @FXML private TextField search;
    @FXML private ProgressIndicator loadingIcon;
    @FXML private Text errorMessage;
    @FXML private ListView<HBox> autocompleteResults;
    @FXML private ImageView downloads;

    // Timer timerRotate;
    Timer hideErrorMessage;

    generateAutocomplete autoCompleteThread;
    allMusicQuery searchThread;

    @FXML
    private void initialize() {

        // Theoretically no way this could change via normal use of the program, but if user starts a download, waits for it to finish and clears file, downloads page needs a check to prevent
        if (Model.getInstance().downloadsAccessible()) {
            downloads.setVisible(true);
        }

        Debug.trace(null, "Initialized search view.");
    }

    @FXML
    private void downloadsView() {
        Debug.trace(null, "Requested to switch to downloads view.");
    }

    @FXML
    private void settingsView(Event event) {

        try {

            Parent settingsView = FXMLLoader.load(getClass().getResource("app/fxml/settings.fxml"));

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
                        if (searchThread.working()) {

                            // Search is already in progress, hence inform user
                            errorMessage.setText("A Search is already in progress, please wait.");
                            errorMessage.setVisible(true);
                            hideErrorMessage();

                        } else {

                            // Search is not in progress, hence we can start a new one
                            throw new NullPointerException();

                        }
                    } catch (NullPointerException ignored) {

                        // Start a new search
                        searchThread = new allMusicQuery(e, search.getText() + e.getText());

                        // Animating the icon
                        loadingIcon.setVisible(true);

                        /*
                        timerRotate = new Timer();
                        timerRotate.schedule(new TimerTask() {
                            @Override
                            public void run() {

                                if (searchThread.working())
                                    loadingIcon.setRotate(loadingIcon.getRotate() + 18);
                                else {
                                    loadingIcon.setVisible(false);
                                    timerRotate.cancel();
                                }
                            }
                        }, 0, 100);

                         */

                    }

                } else {

                    // Warn user search is too short
                    errorMessage.setText("Query is too short, no results.");
                    hideErrorMessage();

                }

            }

        } else if (e.getCode() == KeyCode.BACK_SPACE && search.getText().length() == 1) {

            // Kill all running autocomplete threads
            autoCompleteThread.kill();
            autocompleteResults.getItems().clear();
            autocompleteResults.setVisible(false);

        }
    }

    private void hideErrorMessage() {
        try {
            hideErrorMessage.cancel();
        } catch (NullPointerException ignored) {}

        hideErrorMessage = new Timer();
        hideErrorMessage.schedule(new TimerTask() {
            @Override
            public void run() {
                errorMessage.setVisible(false);
            }
        }, 2000);
    }

    // Generating the full data for the search data
    class allMusicQuery implements Runnable {

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
                        resultData.put("album", result.select("div.cover").size() > 0 ? 1 : 0);

                        // Art (Albums Only)
                        if (result.select("div.cover").size() > 0) {
                            String potentialAlbumArt = result.select("img.lazy").attr("data-original");
                            resultData.put("art", potentialAlbumArt.isBlank() ? new File(getClass().getResource("app/img/album_default.png").getPath()).toURI().toString() : potentialAlbumArt);
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
                // Adding remaining data & preparing data for table
                Model.resultsSet[] tableData = new Model.resultsSet[searchData.length()];
                for (int i = 0; i < searchData.length(); i++) {

                    // Gathering additional data for JSON object
                    try {
                        if (searchData.getJSONObject(i).getInt("album") == 0) {

                            if (!Model.getInstance().settings.getSettingBool("data_saver")) {

                                // Finding: Album Art Link, Year and Genre

                                // Reading information page
                                Document songDataPage = Jsoup.connect(searchData.getJSONObject(i).getString("link")).get();

                                // Album Art
                                String potentialAlbumArt = songDataPage.selectFirst("td.cover").selectFirst("img").attr("src") != null && songDataPage.selectFirst("td.cover").selectFirst("img").attr("src").startsWith("https") && !songDataPage.selectFirst("td.cover").selectFirst("img").attr("src").equals("https://cdn-gce.allmusic.com/images/lazy.gif") ? songDataPage.selectFirst("td.cover").selectFirst("img").attr("src") : new File("resources/song_default.png").toURI().toString();
                                searchData.getJSONObject(i).put("art", potentialAlbumArt.isBlank() ? new File(getClass().getResource("app/img/song_default.png").getPath()).toURI().toString() : potentialAlbumArt);
                                // Year
                                if (songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear").length() == 4)
                                    searchData.getJSONObject(i).put("year", songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear"));

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
                                searchData.getJSONObject(0).put("art", new File("app/img/song_default.png").toURI().toString());
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
                        tableData[i] = new Model.resultsSet(
                                new ImageView(new Image(searchData.getJSONObject(i).getString("art"))),
                                searchData.getJSONObject(i).getString("title"),
                                searchData.getJSONObject(i).getString("artist"),
                                searchData.getJSONObject(i).getString("year"),
                                searchData.getJSONObject(i).getString("genre"),
                                searchData.getJSONObject(i).getInt("album") == 0 ? "Song" : "Album"
                        );
                    } catch (JSONException | IllegalArgumentException e) {
                        try {
                            System.out.println(searchData.getJSONObject(i));
                        } catch (JSONException ignored) {
                            System.out.println("huh");
                        }
                        Debug.error(thread, "Failed to generate table result", e.getCause());
                    }

                }

                // Sending processed data to the model
                Model.getInstance().setSearchResults(tableData);

                // Changing scene to results-view
                try {
                    Parent resultsView = FXMLLoader.load(getClass().getResource("app/fxml/results.fxml"));
                    Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();

                    Platform.runLater(() -> mainWindow.setScene(new Scene(resultsView, mainWindow.getWidth()-16, mainWindow.getHeight()-39)));

                } catch (IOException e) {
                    Debug.error(null, "FXML Error: Settings.fxml", e.getCause());
                }

            } else {

                Debug.trace(thread, "No search results found for query: " + query);
                Platform.runLater(() -> {
                    // timerRotate.cancel();
                    loadingIcon.setVisible(false);

                    errorMessage.setText("No Search Results Found");
                    errorMessage.setVisible(true);
                });

            }

        }

    }

    // Updating the UI with the autocomplete suggestions
    class generateAutocomplete implements Runnable {

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

            try {
                Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + query).get();
                ArrayList<HBox> autocompleteResultsView = new ArrayList<>();
                Elements results = doc.select("ul.search-results").select("li");

                for (Element result: results)
                {

                    // Check that it's either a album or an song, not an artist, the data is a bit odd so the hashcode fixes it
                    if (result.select("h4").text().hashCode() != 1969736551 && result.select("h4").text().hashCode() != 73174740) {

                        autocompleteResultsView.add(
                                new HBox(
                                        10,
                                        new ImageView(
                                                new Image(
                                                        new File(
                                                                result.select("div.cover").size() > 0 ? "resources/album_default.png" : "resources/song_default.png"
                                                        ).toURI().toString(),
                                                        25,
                                                        25,
                                                        true,
                                                        true
                                                )
                                        ),
                                        new Text(result.select("div.title").text().replaceAll("\"", ""))
                                )
                        );

                    }
                }

                // Add generated data to the search query
                if (!killRequest) {
                    Platform.runLater(() -> {
                        autocompleteResults.getItems().setAll(autocompleteResultsView);
                        autocompleteResults.setVisible(true);
                    });
                }

            } catch (IOException ignored) {

                // Failed to connect, handling
                Debug.warn(thread, "Error sending web request: https://www.allmusic.com/search/all/" + query);
                Platform.runLater(() -> {
                    search.setDisable(true);
                    new awaitReconnection();
                });
            }

        }

    }

    // Check that when internet connection is lost, they must reconnect before doing anything else
    public class awaitReconnection implements Runnable {

        public awaitReconnection() {
            Thread thread = new Thread(this, "reconnection");
            thread.start();
        }

        public void run() {

            Timer connectionAttempt = new Timer();
            TimerTask webRequest = new TimerTask() {
                @Override
                public void run() {
                    boolean reconnected = false;
                    try {

                        Platform.runLater(() -> errorMessage.setText("Attempting to reconnect..."));

                        if (InetAddress.getByName("allmusic.com").isReachable(1000)) {
                            // Connection reestablished
                            Platform.runLater(() -> {
                                search.setDisable(false);
                                errorMessage.setVisible(false);
                            });
                            Debug.trace(null, "Connection reestablished.");
                            reconnected = true;
                            connectionAttempt.cancel();
                        }

                    } catch (IOException ignored) {}

                    if (!reconnected) {
                        final int[] countDown = {Model.getInstance().settings.getSettingBool("data_saver") ? 60 : 10};
                        Platform.runLater(() -> errorMessage.setVisible(true));
                        Timer messageDisplay = new Timer();
                        messageDisplay.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (countDown[0] == 0) {
                                    errorMessage.setText("Connection failed, attempting to reconnect...");
                                    messageDisplay.cancel();
                                } else {
                                    errorMessage.setText(String.format("Connection failed, attempting to reconnect in %s second%s...", countDown[0], countDown[0] == 1 ? "" : "s"));
                                    countDown[0]--;
                                }
                            }
                        }, 0, 1000);
                    }

                }
            };
            connectionAttempt.schedule(webRequest, 0, Model.getInstance().settings.getSettingBool("data_saver") ? 60 : 10 * 1000);

        }

    }


}