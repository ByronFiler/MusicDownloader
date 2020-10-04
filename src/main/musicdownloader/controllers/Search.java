package musicdownloader.controllers;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.net.db.sites.Allmusic;
import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

public class Search {

    @FXML
    private AnchorPane root;

    @FXML
    private BorderPane offlineNotification;

    @FXML
    private TextField search;
    @FXML
    private ProgressIndicator loadingIcon;
    @FXML
    private ListView<HBox> autocompleteResults;

    @FXML
    private ImageView downloads;
    @FXML
    private ImageView settings;

    private generateAutocomplete autoCompleteThread;
    private boolean searchQueryActive = false;

    @FXML
    private void initialize() {

        Model.getInstance().connectionWatcher.switchMode(this);
        if (Model.getInstance().connectionWatcher.isOffline()) {
            offlineNotification.setVisible(true);
            offlineNotification.setManaged(true);
            search.setDisable(true);
        }

        // Loading in CSS
        if (Model.getInstance().settings.getSettingBool("dark_theme")) {
            root.getStylesheets().add(String.valueOf(getClass().getClassLoader().getResource("resources/css/dark.css")));

            ColorAdjust invert = new ColorAdjust();
            invert.setBrightness(1);

            downloads.setEffect(invert);
            settings.setEffect(invert);
        } else root.getStylesheets().add(String.valueOf(getClass().getClassLoader().getResource("resources/css/standard.css")));

        Debug.trace("Initialized search view.");
    }

    @FXML
    private void downloadsView(Event event) {
        try {

            FXMLLoader downloadsLoader = new FXMLLoader(
                    getClass().getClassLoader().getResource("resources/fxml/downloads.fxml"),
                    ResourceBundle.getBundle("resources.locale.downloads")
            );
            Parent controllerView = downloadsLoader.load();
            Model.getInstance().download.setDownloadsView(downloadsLoader.getController());

            (
                    ((Node) event.getSource())
                            .getScene()
                            .getWindow()
            )
                    .getScene()
                    .setRoot(
                            controllerView
                    );
        } catch(IOException e) {
            Debug.error("FXML Error: downloads.fxml", e);
        }
    }

    @FXML
    private void settingsView(Event event) {

        try {
            (
                    ((Node) event.getSource())
                            .getScene()
                            .getWindow()
            )
                    .getScene()
                    .setRoot(
                            FXMLLoader.load(
                                    Objects.requireNonNull(getClass().getClassLoader().getResource("resources/fxml/settings.fxml")),
                                    ResourceBundle.getBundle("resources.locale.settings")
                            )
                    );

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
                    if (!searchQueryActive) {
                        loadingIcon.setVisible(true);
                        searchQueryActive = true;
                        Thread queryThread = new Thread(() -> {
                            Allmusic.Search searcher = new Allmusic.Search(search.getText() + e.getText());

                            try {
                                long preQueryTime = Instant.now().toEpochMilli();

                                searcher.query(Model.getInstance().settings.getSettingBool("data_saver"));

                                // Can open each song page and find relevant album to display album art, and other information such as genre and year which isn't given in a default search
                                if (!Model.getInstance().settings.getSettingBool("data_saver")) {
                                    searcher.getSongExternalInformation();
                                }

                                // This causes a lot of time:tm:
                                Model.getInstance().search.setSearchResults(searcher.buildView().toArray(new BorderPane[0]));
                                Model.getInstance().search.setSearchResultsJson(searcher.getResults());

                                long now = Instant.now().toEpochMilli();

                                if (!Model.getInstance().settings.getSettingBool("data_saver") && searcher.getSongCount() > 0) {
                                    Debug.trace(
                                            String.format(
                                                    "Query completed in %.2f seconds, containing %s album(s) %s song(s)",
                                                    (double) (now - preQueryTime) / 1000,
                                                    searcher.getAlbumCount(),
                                                    searcher.getSongCount()
                                            )
                                    );
                                }

                                Platform.runLater(() -> {
                                    try {
                                        (
                                                ((Node) e.getSource())
                                                        .getScene()
                                                        .getWindow()
                                        )
                                                .getScene()
                                                .setRoot(
                                                        FXMLLoader.load(
                                                                Objects.requireNonNull(getClass().getClassLoader().getResource("resources/fxml/results.fxml")),
                                                                ResourceBundle.getBundle("resources.locale.results")
                                                        )
                                                );
                                    } catch (IOException er) {
                                        Debug.error("FXML Error: Settings.fxml", er);
                                    } catch (NullPointerException ignored) {}
                                });


                            } catch (HttpStatusException ignored) {

                                Platform.runLater(() -> {
                                    loadingIcon.setVisible(false);
                                    searchQueryActive = false;
                                });

                            } catch (IOException er) {
                                Platform.runLater(() -> {

                                    loadingIcon.setVisible(false);
                                    searchQueryActive = false;

                                    Debug.warn("Failed to connect to " + Allmusic.baseDomain + Allmusic.Search.subdirectory + search.getText() + e.getText());
                                });
                            }

                        });
                        queryThread.setDaemon(true);
                        queryThread.start();
                    }

                }

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

    public synchronized TextField getSearch() {
        return search;
    }

    public BorderPane getOfflineNotification() {
        return offlineNotification;
    }

    // Updating the UI with the autocomplete suggestions
    private class generateAutocomplete implements Runnable {

        private volatile boolean killRequest = false;
        private final String query;

        generateAutocomplete (String query){
            this.query = query;

            Thread thread = new Thread(this, "autocomplete");
            thread.setDaemon(true);
            thread.start();
        }

        public void kill() {
            killRequest = true;
        }

        public void run() {

            if (Model.getInstance().settings.getSettingBool("data_saver"))
                return;

            try {
                Allmusic.Search autocompleteSearch = new Allmusic.Search(query);
                autocompleteSearch.query(true);
                JSONArray autocompleteProcessedResults = new JSONArray();
                try {
                    autocompleteProcessedResults = autocompleteSearch.getResults().getJSONArray("songs");
                } catch (JSONException e) {
                    Debug.error("Failed to get autocomplete results.", e);
                }
                ArrayList<String> viewResultTitles = new ArrayList<>();
                ArrayList<HBox> autocompleteResultsView = new ArrayList<>();

                try {
                    for (int i = 0; i < autocompleteProcessedResults.length(); i++) {

                        if (!viewResultTitles.contains(autocompleteProcessedResults.getJSONObject(i).getJSONObject("view").getString("title"))) {
                            viewResultTitles.add(autocompleteProcessedResults.getJSONObject(i).getJSONObject("view").getString("title"));

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
                            JSONArray finalAutocompleteProcessedResults = autocompleteProcessedResults;
                            autocompleteResultView.setOnMouseClicked(e -> {
                                try {
                                    search.setText(finalAutocompleteProcessedResults.getJSONObject(finalI).getJSONObject("view").getString("title"));
                                } catch (JSONException er) {
                                    Debug.warn("Failed to set autocomplete result.");
                                }
                            });
                            autocompleteResultView.setCursor(Cursor.HAND);
                            autocompleteResultsView.add(autocompleteResultView);
                        }
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
                Debug.warn("Error sending web request: https://www.allmusic.com/search/all/" + query);
            } catch (HttpStatusException ignored) {
                // Malformed request
            } catch (IOException e) {
                Debug.error("Unknown exception when requesting user search.", e);
            }

        }

    }

}