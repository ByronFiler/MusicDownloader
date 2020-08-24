package musicdownloader.controllers;

import musicdownloader.Main;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.net.db.sites.Allmusic;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/*
TODO
 - Losing connection mid-search generates a partially completed results table, don't let this happen
 - Continue testing connection drops: Control Panel\Network and Internet\Network Connections
 - Search data could also theoretically load songs since the request is already sent to save sending the requests again decreasing load speeds and web requests
 - For quicker general usability try and finish and let the youtube backend threads just complete the search results instead of making the user wait
 - Stop allowing duplicates in the autocomplete
 */
public class Search {

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
    private boolean searchQueryActive = false;

    @FXML
    private void initialize() {

        // Loading in CSS
        if (Model.getInstance().settings.getSettingBool("dark_theme")) {

            root.getStylesheets().add(
                    String.valueOf(Main.class.getResource("app/css/dark.css"))
            );

            ColorAdjust invert = new ColorAdjust();
            invert.setBrightness(1);

            downloads.setEffect(invert);
            settings.setEffect(invert);
        }
        else
            root.getStylesheets().add(
                    String.valueOf(Main.class.getResource("app/css/standard.css"))
            );

        Debug.trace("Initialized search view.");
    }

    @FXML
    private void downloadsView(Event event) {
        try {
            Parent settingsView = FXMLLoader.load(Main.class.getResource("app/fxml/downloads.fxml"));
            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(settingsView, mainWindow.getWidth() - Resources.getInstance().getWindowResizeWidth(), mainWindow.getHeight() - Resources.getInstance().getWindowResizeHeight()));

        } catch(IOException e) {
            Debug.error("FXML Error: downloads.fxml", e);
        }
    }

    @FXML
    private void settingsView(Event event) {

        try {
            Parent settingsView = FXMLLoader.load(Main.class.getResource("app/fxml/settings.fxml"));

            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(settingsView, mainWindow.getWidth() - Resources.getInstance().getWindowResizeWidth(), mainWindow.getHeight() - Resources.getInstance().getWindowResizeHeight()));

        } catch(IOException e) {
            Debug.error("Missing FXML File: Settings.fxml", e);
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
                    if (searchQueryActive) error("A Search is already in progress, please wait.");
                    else {
                        loadingIcon.setVisible(true);
                        searchQueryActive = true;

                        Thread queryThread = new Thread(() -> {

                            Allmusic.search searcher = new Allmusic.search(search.getText() + e.getText());

                            try {
                                long preQueryTime = Instant.now().toEpochMilli();
                                long preBuilderTime = 0;

                                searcher.query(Model.getInstance().settings.getSettingBool("data_saver"));

                                // Can open each song page and find relevant album to display album art, and other information such as genre and year which isn't given in a default search
                                if (!Model.getInstance().settings.getSettingBool("data_saver")) {
                                    preBuilderTime = Instant.now().toEpochMilli();
                                    searcher.getSongExternalInformation();
                                }


                                Model.getInstance().search.setSearchResults(searcher.buildView().toArray(new BorderPane[0]));
                                Model.getInstance().search.setSearchResultsJson(searcher.getSearchResultsData());

                                long now = Instant.now().toEpochMilli();

                                if (!Model.getInstance().settings.getSettingBool("data_saver"))
                                    Debug.trace(
                                            String.format(
                                                    "Query results completed in %.2f seconds, containing %s album%s %s song%s and (%sms per song average)",
                                                    (double) (now - preQueryTime) / 1000,
                                                    searcher.getAlbumCount(),
                                                    searcher.getAlbumCount() == 1 ? "" : "s",
                                                    searcher.getSongCount(),
                                                    searcher.getSongCount() == 1 ? "" : "s",
                                                    Math.round((double) (now - preBuilderTime) / searcher.getSongCount())
                                            )
                                    );

                                try {
                                    Parent resultsView = FXMLLoader.load(Main.class.getResource("app/fxml/results.fxml"));
                                    Stage mainWindow = (Stage) ((Node) e.getSource()).getScene().getWindow();

                                    Platform.runLater(() -> mainWindow.setScene(new Scene(resultsView, mainWindow.getWidth() - Resources.getInstance().getWindowResizeWidth(), mainWindow.getHeight() - Resources.getInstance().getWindowResizeHeight())));

                                } catch (IOException er) {
                                    Debug.error("FXML Error: Settings.fxml", er);
                                }

                            } catch (HttpStatusException ignored) {

                                Platform.runLater(() -> {
                                    loadingIcon.setVisible(false);
                                    searchQueryActive = false;

                                    error("No results found.");
                                });

                            } catch (IOException er) {
                                Platform.runLater(() -> {

                                    loadingIcon.setVisible(false);
                                    searchQueryActive = false;

                                    Debug.warn("Failed to connect to " + Allmusic.baseDomain + Allmusic.search.subdirectory + search.getText() + e.getText());
                                    new awaitReconnection();
                                });
                            }

                        });
                        queryThread.setDaemon(true);
                        queryThread.start();
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
                Allmusic.search autocompleteSearch = new Allmusic.search(query);
                autocompleteSearch.query(true);
                JSONArray autocompleteProcessedResults = autocompleteSearch.getSearchResultsData();
                ArrayList<HBox> autocompleteResultsView = new ArrayList<>();

                try {
                    for (int i = 0; i < autocompleteProcessedResults.length(); i++) {
                        Label resultTitle = new Label(autocompleteProcessedResults.getJSONObject(i).getJSONObject("view").getString("title"));
                        resultTitle.getStyleClass().add("sub_text2");

                        ImageView resultIcon = new ImageView(
                                new Image(
                                        autocompleteProcessedResults.getJSONObject(i).getJSONObject("view").getString("art"),
                                        25,
                                        25,
                                        true,
                                        true
                                )
                        );

                        if (Model.getInstance().settings.getSettingBool("dark_theme"))
                            resultIcon.setEffect(new ColorAdjust(0, 0, 1, 0));

                        HBox autocompleteResultView = new HBox(10, resultIcon, resultTitle);
                        int finalI = i;
                        autocompleteResultView.setOnMouseClicked(e -> {
                            try {
                                search.setText(autocompleteProcessedResults.getJSONObject(finalI).getString("title"));
                            } catch (JSONException er) {
                                Debug.error("Failed to set autocomplete result.", er);
                            }
                        });
                        autocompleteResultView.setCursor(Cursor.HAND);

                        autocompleteResultsView.add(autocompleteResultView);
                    }
                } catch (JSONException e) {
                    Debug.error("Error processing JSON to generate autocomplete results.", e);
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
                Debug.trace("Error sending web request: https://www.allmusic.com/search/all/" + query);
                Platform.runLater(() -> {
                    search.setDisable(true);
                    new awaitReconnection();
                });
            } catch (HttpStatusException ignored) {
                // Malformed request
            } catch (IOException e) {
                Debug.error("Unknown exception when requesting user search.", e);
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
                            Debug.trace("Connection reestablished.");
                            this.cancel();

                        }

                    } catch (IOException ignored) {}

                }

            }, 0, 1000);
        }

    }
}