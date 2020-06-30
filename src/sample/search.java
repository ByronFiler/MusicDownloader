package sample;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

// TODO
// Fix title isn't centered with the search bar due to the invisible loading icon
public class search {

    @FXML private AnchorPane root;

    @FXML private TextField search;
    @FXML private ImageView loadingIcon;
    @FXML private Text errorMessage;
    @FXML private ListView<HBox> autocompleteResults;
    @FXML private Label downloads;

    Timer timerRotate;
    Timer hideErrorMessage;
    Timer networkCheck;

    generateAutocomplete autoCompleteThread;
    allMusicQuery searchThread;

    @FXML
    private void initialize(Event e) {

        // Theoretically no way this could change via normal use of the program, but if user starts a download, waits for it to finish and clears file, downloads page needs a check to prevent
        if (Model.getInstance().downloadsAccessible()) {
            downloads.setDisable(false);
            downloads.setTextFill(Color.BLACK);
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
            mainWindow.setScene(new Scene(settingsView));

        } catch(IOException e) {
            Debug.error(null, "Missing FXML File: Settings.fxml", e.getStackTrace());
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

            // Raw & Basic Data
            ArrayList<ArrayList<String>> searchResults;

            try {

                Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + query).get();
                searchResults = Utils.allmusicQuery(doc);

            } catch (IOException e) {

                Platform.runLater(() -> {
                    timerRotate.cancel();
                    loadingIcon.setVisible(false);

                    errorMessage.setText("Invalid Search");
                    errorMessage.setVisible(true);
                });
                Debug.warn(thread, "Failed to connect to allmusic");
                return;

            }

            // Populated & Table-Ready
            Model.resultsSet[] tableData = new Model.resultsSet[searchResults.size()];

            boolean dataSaver = Model.getInstance().settings.getSettingBool("data_saver");

            if (searchResults.size() > 0) {

                for (ArrayList<String> searchResult: searchResults) {

                    /*
                    [
                        0: Title,
                        1: Artist,
                        2: Year,
                        3: Genre,
                        4: Type,
                        5: Album Art URL,
                        6: Information Page,
                        7: ???
                    ]
                     */

                    // Data is missing and must be acquired
                    if (searchResult.get(4).equals("Song") && !dataSaver) {

                        try {
                            // Establishing connection and reading response
                            Document songDataPage = Jsoup.connect(searchResult.get(6)).get();

                            // Locating the album art
                            if (songDataPage.selectFirst("td.cover").selectFirst("img").attr("src") != null && songDataPage.selectFirst("td.cover").selectFirst("img").attr("src").startsWith("https") && !songDataPage.selectFirst("td.cover").selectFirst("img").attr("src").equals("https://cdn-gce.allmusic.com/images/lazy.gif")) {
                                searchResult.set(5, songDataPage.selectFirst("td.cover").selectFirst("img").attr("src"));
                            } else {
                                searchResult.set(5, new File("resources/song_default.png").toURI().toString());
                            }

                            // Locating the year
                            if (songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear").length() == 4)
                                searchResult.set(2, songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear"));

                            // Locating the genre
                            try {
                                searchResult.set(
                                        3,
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


                        } catch (IOException ignored) {
                            Debug.warn(thread, "Failed to connect to: " + searchResult.get(6));
                        }

                    } else {

                        if (searchResult.get(5).isEmpty())
                            searchResult.set(5, new File("resources/album_default.png").toURI().toString());

                    }

                    try {
                        tableData[searchResults.indexOf(searchResult)] = new Model.resultsSet(
                                new ImageView(new Image(searchResult.get(5))),
                                searchResult.get(0),
                                searchResult.get(1),
                                searchResult.get(2),
                                searchResult.get(3),
                                searchResult.get(4)
                        );
                    } catch (IllegalArgumentException ignored) {
                        Debug.warn(thread, "Error processing search result: " + searchResult.toString());
                    }
                }

                // Updating the model with the data
                Model.getInstance().setSearchResults(tableData);

                // Transitioning to results to show search results
                System.out.println("Switching to search view");
                try {

                    Parent resultsView = FXMLLoader.load(new File("resources/fxml/results.fxml").toURI().toURL());
                    Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    Platform.runLater(() -> mainWindow.setScene(new Scene(resultsView)));

                } catch(IOException e) {
                    e.printStackTrace();
                    // Debug.error(null, "Missing FXML File: Settings.fxml", e.getStackTrace());
                }

            } else {

                Debug.warn(thread, "No search results found for query: " + query);
                Platform.runLater(() -> {
                    timerRotate.cancel();
                    loadingIcon.setVisible(false);

                    errorMessage.setText("No Search Results Found");
                    errorMessage.setVisible(true);
                });

            }

        }

    }

    // Updating the UI with the autocomplete suggestions
    class generateAutocomplete implements Runnable {

        private final Timer webCheck = new Timer();
        private final autoCompleteWeb webThread;
        private volatile boolean killRequest = false;

        generateAutocomplete (String query){
            webThread = new autoCompleteWeb(query);
            Thread thread = new Thread(this, "autocomplete");
            thread.start();
        }

        public void kill() {
            killRequest = true;
        }

        public void run() {

            webCheck.schedule(new TimerTask() {
                @Override
                public void run() {

                    if (killRequest)
                        return;

                    if (!webThread.getAutocompleteResults().isEmpty()) {

                        // Add all the data at once to try and minimize excessive calls
                        HBox[] autocompleteResultsDisplay = new HBox[webThread.getAutocompleteResults().size()];

                        // Add the data as a viewable element
                        for (ArrayList<String> queryResult : webThread.getAutocompleteResults()) {
                            autocompleteResultsDisplay[webThread.getAutocompleteResults().indexOf(queryResult)] = new HBox(
                                    10,
                                    new ImageView(
                                            new Image(
                                                    new File(queryResult.get(0).equals("Album") ? "resources/album_default.png" : "resources/song_default.png").toURI().toString(),
                                                    25,
                                                    25,
                                                    true,
                                                    true
                                            )
                                    ),
                                    new Text(queryResult.get(1))
                            );
                        }

                        // Add generated data to the search query
                        Platform.runLater(() -> {
                            autocompleteResults.getItems().setAll(autocompleteResultsDisplay);
                            autocompleteResults.setVisible(true);
                        });

                    }

                }
            }, 0, 50);

        }

        // The web requests for the auto-complete functionality
        class autoCompleteWeb implements Runnable {
            private final Thread thread;
            private final String searchQuery;
            private final ArrayList<ArrayList<String>> autocompleteResults = new ArrayList<>();

            autoCompleteWeb (String queryRequest){
                searchQuery = queryRequest;
                thread = new Thread(this, "autocomplete-web");
                thread.start();
            }

            public ArrayList<ArrayList<String>> getAutocompleteResults() {
                return autocompleteResults;
            }

            public void run() {

                try {

                    Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + searchQuery).get();

                    for (ArrayList<String> searchResult: Utils.allmusicQuery(doc)) {

                        ArrayList<String> resultsData = new ArrayList<>();
                        resultsData.add(searchResult.get(4));
                        resultsData.add(searchResult.get(0));

                        // Prevents duplicated results
                        if (!autocompleteResults.contains(resultsData)) {

                            autocompleteResults.add(resultsData); // Song or Album and Name

                        }
                    }

                } catch (IOException ignored) {
                    Debug.warn(thread, "Error sending web request: https://www.allmusic.com/search/all/" + searchQuery);
                    // Now we want to await for a connection to be reestablished

                    Platform.runLater(() -> {
                        search.setDisable(true);
                        new awaitReconnection();
                    });


                }

            }

        }

    }

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
                    try {

                        Platform.runLater(() -> errorMessage.setText("Attempting to reconnect..."));

                        if (InetAddress.getByName("https://www.allmusic.com/").isReachable(1000)) {
                            // Connection reestablished
                            Platform.runLater(() -> {
                                search.setDisable(false);
                                errorMessage.setVisible(false);
                            });
                        } else {
                            System.out.println("We reached the else statement.");
                        }

                    } catch (IOException ignored) {
                        // Connection still down
                        System.out.println("We reached the error.");

                    }
                }
            };
            connectionAttempt.schedule(webRequest, 0, Model.getInstance().settings.getSettingBool("data_saver") ? 60 : 10 * 1000);

            connectionAttempt.schedule(new TimerTask() {
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

        }

    }



}