package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class search {

    @FXML private TextField search;
    @FXML private ImageView loadingIcon;
    @FXML private Text errorMessage;
    @FXML private ListView<HBox> autocompleteResults;

    Timer timerRotate;
    Timer hideErrorMessage;

    generateAutocomplete autoCompleteThread;

    @FXML
    private void initialize() {
        Debug.trace(null, "Initialized search view.");
    }

    @FXML
    private void downloadsView() {
        Debug.trace(null, "Requested to switch to downloads view.");
    }

    @FXML
    private void settingsView() {
        Debug.trace(null, "Requested to switch to settings view.");
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

                    if (false) {

                        // Animating the icon
                        loadingIcon.setVisible(true);
                        timerRotate = new Timer();
                        timerRotate.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                loadingIcon.setRotate(loadingIcon.getRotate() + 18);
                            }
                        }, 0, 100);

                    } else {

                        errorMessage.setText("A Search is already in progress, please wait.");
                        errorMessage.setVisible(true);
                        hideErrorMessage();
                    }

                } else {

                    // Warn user search is too short


                }

            }

        } else if (e.getCode() == KeyCode.BACK_SPACE && search.getText().length() == 1) {

            // Kill all running autocomplete threads
            System.out.println("Should be killing the autocomplete thread.");

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
    /*
    class allMusicQuery implements Runnable {

        Thread t;
        private volatile boolean working = true;
        private volatile boolean kill = false;
        private volatile String status = "Initializing";
        private final long startTime = Instant.now().toEpochMilli();
        private volatile long endTime = Long.MIN_VALUE;

        public boolean inProgress() {
            return working;
        }

        public void kill() {
            kill = true;
        }

        allMusicQuery (){
            t = new Thread(this, "query");
            t.start();
        }

        public ArrayList<String> getInfo() {
            return new ArrayList<>(
                    Arrays.asList(
                            t.getName(),
                            Long.toString(t.getId()),
                            Long.toString(startTime),
                            Long.toString(endTime),
                            status,
                            Boolean.toString(!working)
                    )
            );
        }

        public void run() {

            try {

                status = "Sending web request to search...";
                Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + searchRequest.getText()).get();
                searchResults = Utils.allmusicQuery(doc);

            } catch (IOException e) {

                Platform.runLater(() -> {
                    timerRotate.cancel();
                    loadingIcon.setVisible(false);
                    searchErrorMessage.setText("Invalid Search");
                    searchErrorMessage.setVisible(true);
                });
                Debug.error(t, "Error connecting to allmusic", e.getStackTrace());
                return;

            }

            if (searchResults.size() > 0) {

                try {

                    status = "Processing search results";

                    // Signal is always sent immediately when running, only want to kill other threads that are in loop, not this one
                    // Needs to check this won't result in future threads being killed
                    resultsData = new ArrayList<>();
                    IntStream.range(0, searchResults.size()).forEach(i -> resultsData.add(null));

                    searchResultFullData = new Utils.resultsSet[searchResults.size()];

                    for (ArrayList<String> searchResult : searchResults) {

                        // Sending a new query requires quitting the old
                        if (kill) {
                            resultsTable.getItems().clear();
                            break;
                        }

                        View.addToTable tableAdder = new View.addToTable();
                        threadManagement.add(new ArrayList<>(Arrays.asList("addToTable", tableAdder)));
                        tableAdder.setSearchResult(searchResult);
                    }

                    while (true) {

                        int completedThreads = 0;
                        int totalThreads = searchResultFullData.length;

                        for (Utils.resultsSet resultPending: searchResultFullData) {

                            // Thread has completed when it is not null
                            if (resultPending != null) {
                                completedThreads++;
                            }

                        }

                        if (completedThreads == totalThreads) {
                            break;
                        }

                        status = "Awaitng detail threads to complete, " + completedThreads + " out of " + totalThreads + " so far.";
                    }

                    for (Utils.resultsSet result: searchResultFullData)
                        resultsTable.getItems().add(result);

                } catch (NullPointerException e) {
                    Debug.error(t, "Invalid search error", e.getStackTrace());
                }

            } else {

                Debug.trace(t, "No search results found for query: " + searchRequest.getText());

                Platform.runLater(() -> {
                    timerRotate.cancel();
                    loadingIcon.setVisible(false);
                    searchErrorMessage.setText("No Search Results Found");
                    searchErrorMessage.setVisible(true);
                });

                return;

            }

            Platform.runLater(() -> {
                timerRotate.cancel();
                loadingIcon.setVisible(false);
                downloadButton.setDisable(true);
                resultsTable.setVisible(true);
                searchResultsTitle.setVisible(true);
                downloadButton.setVisible(true);
                cancelButton.setVisible(true);

                autocompleteResultsTable.setVisible(false);
                autocompleteResultsTable.getItems().clear();
                searchRequest.setVisible(false);
                title.setVisible(false);
                footerMarker.setVisible(false);
                settingsLink.setVisible(false);
                downloadsLink.setVisible(false);
                searchErrorMessage.setVisible(false);
            });

            endTime = Instant.now().toEpochMilli();
            working = false;

        }

    }

     */

    // Updating the UI with the autocomplete suggestions
    class generateAutocomplete implements Runnable {

        private Thread thread;
        private Timer webCheck = new Timer();
        private autoCompleteWeb webThread;
        private volatile boolean killRequest = false;

        generateAutocomplete (String query){
            webThread = new autoCompleteWeb(query);
            thread = new Thread(this, "autocomplete");
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
                                                    new File(queryResult.get(0).equals("Album") ? "resources/album_default.jpg" : "resources/song_default.png").toURI().toString(),
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

                } catch (IOException e) {
                    Debug.error(thread, "Error sending web request: https://www.allmusic.com/search/all/" + searchQuery, e.getStackTrace());
                }

            }

        }

    }

}