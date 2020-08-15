package sample.controllers;

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
import sample.Main;
import sample.model.Model;
import sample.utils.app.debug;
import sample.utils.net.db.allmusic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/*
TODO
 - Losing connection mid-search generates a partially completed results table, don't let this happen
 - Continue testing connection drops: Control Panel\Network and Internet\Network Connections
 - Search data could also theoretically load songs since the request is already sent to save sending the requests again decreasing load speeds and web requests
 - For quicker general usability try and finish and let the youtube backend threads just complete the search results instead of making the user wait
 */
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
    private boolean searchQueryActive = false;

    @FXML
    private void initialize() {
        // Theoretically no way this could change via normal use of the program, but if user starts a download, waits for it to finish and clears file, downloads page needs a check to prevent
        if (Model.getInstance().download.downloadsAccessible())
            downloads.setVisible(true);

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

        debug.trace(null, "Initialized search view.");
    }

    @FXML
    private void downloadsView(Event event) {
        try {
            Parent settingsView = FXMLLoader.load(Main.class.getResource("app/fxml/downloads.fxml"));

            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(settingsView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

        } catch(IOException e) {
            debug.error(null, "FXML Error: downloads.fxml", e);
        }
    }

    @FXML
    private void settingsView(Event event) {

        try {
            Parent settingsView = FXMLLoader.load(Main.class.getResource("app/fxml/settings.fxml"));

            Stage mainWindow = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainWindow.setScene(new Scene(settingsView, mainWindow.getWidth()-16, mainWindow.getHeight()-39));

        } catch(IOException e) {
            debug.error(null, "Missing FXML File: Settings.fxml", e);
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
                            allmusic.search searcher = new allmusic.search(search.getText() + e.getText());

                            try {
                                searcher.query(Model.getInstance().settings.getSettingBool("data_saver"));
                                if (!Model.getInstance().settings.getSettingBool("data_saver"))
                                    searcher.getSongExternalInformation();

                                Model.getInstance().search.setSearchResults(searcher.buildView().toArray(new BorderPane[0]));
                                Model.getInstance().search.setSearchResultsJson(searcher.getSearchResultsData());

                                try {
                                    Parent resultsView = FXMLLoader.load(Main.class.getResource("app/fxml/results.fxml"));
                                    Stage mainWindow = (Stage) ((Node) e.getSource()).getScene().getWindow();

                                    Platform.runLater(() -> mainWindow.setScene(new Scene(resultsView, mainWindow.getWidth()-16, mainWindow.getHeight()-39)));

                                } catch (IOException er) {
                                    debug.error(null, "FXML Error: Settings.fxml", er);
                                }

                            } catch (IOException er) {
                                Platform.runLater(() -> {

                                    loadingIcon.setVisible(false);
                                    searchQueryActive = false;

                                    debug.warn(Thread.currentThread(), "Failed to connect to " + allmusic.baseDomain + allmusic.search.subdirectory + search.getText() + e.getText());
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
                allmusic.search autocompleteSearch = new allmusic.search(query);
                autocompleteSearch.query(true);
                JSONArray autocompleteProcessedResults = autocompleteSearch.getSearchResultsData();
                ArrayList<HBox> autocompleteResultsView = new ArrayList<>();

                try {
                    for (int i = 0; i < autocompleteProcessedResults.length(); i++) {
                        Label resultTitle = new Label(autocompleteProcessedResults.getJSONObject(i).getString("title"));
                        resultTitle.getStyleClass().add("sub_text2");

                        ImageView resultIcon = new ImageView(
                                new Image(
                                        autocompleteProcessedResults.getJSONObject(i).getString("art"),
                                        25,
                                        25,
                                        true,
                                        true
                                )
                        );

                        if (Model.getInstance().settings.getSettingBool("dark_theme"))
                            resultIcon.setEffect(new ColorAdjust(0, 0, 1, 0));

                        HBox autocompleteResultView = new HBox(10, resultIcon, resultTitle);
                        autocompleteResultView.setOnMouseClicked(e -> search.setText(((Label) (autocompleteResultView.getChildren().get(1))).getText()));
                        autocompleteResultView.setCursor(Cursor.HAND);

                        autocompleteResultsView.add(autocompleteResultView);
                    }
                } catch (JSONException e) {
                    debug.error(Thread.currentThread(), "Error processing JSON to generate autocomplete results.", e);
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
                debug.warn(thread, "Error sending web request: https://www.allmusic.com/search/all/" + query);
                Platform.runLater(() -> {
                    search.setDisable(true);
                    new awaitReconnection();
                });
            } catch (IOException e) {
                debug.error(Thread.currentThread(), "Unknown exception when requesting user search.", e);
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
                            debug.trace(null, "Connection reestablished.");
                            this.cancel();

                        }

                    } catch (IOException ignored) {}

                }

            }, 0, 1000);
        }

    }

}