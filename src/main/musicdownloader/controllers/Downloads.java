package musicdownloader.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import musicdownloader.Main;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.ui.Result;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

/*
TODO
 - Check if iTunes is installed and add a right click to open in iTunes option
 - Clearing a history does not check to remove from combobox
 */

public class Downloads {

    @FXML
    private AnchorPane root;
    @FXML
    private VBox viewContainer;

    @FXML
    private Label eventViewTitle;
    @FXML
    private ComboBox<String> eventViewSelector;
    @FXML
    private ListView<BorderPane> eventsViewTable;

    @FXML
    private ImageView albumViewSelector;
    @FXML
    private ImageView songViewSelector;

    @FXML
    private BorderPane albumViewSelectorWrapper;
    @FXML
    private BorderPane songViewSelectorWrapper;

    private final ArrayList<CurrentlyDownloadingResultController> currentlyDownloading = new ArrayList<>();
    private final ArrayList<QueuedResultController> queued = new ArrayList<>();
    private final ArrayList<HistoryResultController> histories = new ArrayList<>();

    @FXML
    private void initialize() {

        try {

            if (Model.getInstance().download.getDownloadObject().has("metadata")) {
                currentlyDownloading.add(new CurrentlyDownloadingResultController(Model.getInstance().download.getDownloadObject()));
                eventViewSelector.getItems().add("Currently Downloading");
            }

            JSONArray queued = Model.getInstance().download.getDownloadQueue();
            if (queued.length() > 0) {
                for (int i = 0; i < (queued.length()); i++)
                    this.queued.add(new QueuedResultController(queued.getJSONObject(i)));
                eventViewSelector.getItems().add("Download Queue");
            }

            JSONArray historiesJson = Model.getInstance().download.getDownloadHistory();
            if (historiesJson.length() > 0) {
                for (int i = 0; i < (historiesJson.length()); i++)
                    this.histories.add(new HistoryResultController(historiesJson.getJSONObject(i)));
                eventViewSelector.getItems().add("Download History");
            }

            switch (eventViewSelector.getItems().size()) {

                case 0:
                    eventViewSelector.setVisible(false);
                    defaultView();
                    break;

                case 1:
                    eventViewSelector.setVisible(false);
                    eventViewTitle.setText(eventViewSelector.getItems().get(0));
                    break;

                default:
                    eventViewSelector.getItems().add(0, "All");
                    eventViewSelector.getSelectionModel().select(0);
                    eventViewTitle.setText("All");

            }

        } catch (JSONException e) {
            Debug.error("Failed to create downloads object from JSON data.", e);
        }

        // Handle switcher & title

        if (Model.getInstance().settings.getSettingBool("dark_theme")) {
            root.getStylesheets().add(String.valueOf(Main.class.getResource("resources/css/dark.css")));
            albumViewSelector.setEffect(new ColorAdjust(0, 0, 1, 0));
            songViewSelector.setEffect(new ColorAdjust(0, 0, 1, 0));
        }

        else root.getStylesheets().add(String.valueOf(Main.class.getResource("resources/css/standard.css")));

        if (Model.getInstance().download.getDownloadObject().has("metadata") || histories.size() > 0) albumsView();
        else defaultView();

        Debug.trace("Initialized downloads view.");
    }

    @FXML
    public void searchView(Event event) {

        Model.getInstance().download.setDownloadsView(null);

        try {
            (
                    ((Node) event.getSource())
                            .getScene()
                            .getWindow()
            )
                    .getScene()
                    .setRoot(
                            FXMLLoader.load(
                                    Main.class.getResource("resources/fxml/search.fxml")
                            )
                    );
        } catch(IOException e) {
            Debug.error("FXML Error: search.fxml", e);
        }

    }

    @FXML
    public void albumsView() {

        String selectedItem = eventViewSelector.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {

            eventsViewTable.getItems().setAll(currentlyDownloading.stream().map(CurrentlyDownloadingResultController::getAlbumView).toArray(BorderPane[]::new));
            eventsViewTable.getItems().addAll(queued.stream().map(QueuedResultController::getAlbumView).toArray(BorderPane[]::new));
            eventsViewTable.getItems().addAll(histories.stream().map(HistoryResultController::getAlbumView).toArray(BorderPane[]::new));

        } else {

            eventViewTitle.setText(selectedItem);

            switch (eventViewSelector.getSelectionModel().getSelectedItem()) {

                case "Currently Downloading":
                    eventsViewTable.getItems().setAll(currentlyDownloading.stream().map(CurrentlyDownloadingResultController::getAlbumView).toArray(BorderPane[]::new));
                    break;

                case "Download Queue":
                    eventsViewTable.getItems().setAll(queued.stream().map(QueuedResultController::getAlbumView).toArray(BorderPane[]::new));
                    break;

                case "Download History":
                    eventsViewTable.getItems().setAll(histories.stream().map(HistoryResultController::getAlbumView).toArray(BorderPane[]::new));
                    break;

                case "All":
                    eventsViewTable.getItems().setAll(currentlyDownloading.stream().map(CurrentlyDownloadingResultController::getAlbumView).toArray(BorderPane[]::new));
                    eventsViewTable.getItems().addAll(queued.stream().map(QueuedResultController::getAlbumView).toArray(BorderPane[]::new));
                    eventsViewTable.getItems().addAll(histories.stream().map(HistoryResultController::getAlbumView).toArray(BorderPane[]::new));
                    break;
            }
        }

        albumViewSelectorWrapper.getStyleClass().setAll("underline2");
        songViewSelectorWrapper.getStyleClass().setAll();
    }

    @FXML
    public void songsView() {
        String selectedItem = eventViewSelector.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {

            eventsViewTable.getItems().clear();
            Arrays.stream(currentlyDownloading.stream().map(CurrentlyDownloadingResultController::getSongsView).toArray(BorderPane[][]::new)).forEach(e -> eventsViewTable.getItems().addAll(e));
            Arrays.stream(queued.stream().map(QueuedResultController::getSongsView).toArray(BorderPane[][]::new)).forEach(e -> eventsViewTable.getItems().addAll(e));
            Arrays.stream(histories.stream().map(HistoryResultController::getSongsView).toArray(BorderPane[][]::new)).forEach(e -> eventsViewTable.getItems().addAll(e));

        } else {

            switch (eventViewSelector.getSelectionModel().getSelectedItem()) {

                case "Currently Downloading":
                    eventsViewTable.getItems().clear();
                    Arrays.stream(currentlyDownloading.stream().map(CurrentlyDownloadingResultController::getSongsView).toArray(BorderPane[][]::new)).forEach(e -> eventsViewTable.getItems().addAll(e));
                    break;

                case "Download Queue":
                    eventsViewTable.getItems().clear();
                    Arrays.stream(queued.stream().map(QueuedResultController::getSongsView).toArray(BorderPane[][]::new)).forEach(e -> eventsViewTable.getItems().addAll(e));
                    break;

                case "Download History":
                    eventsViewTable.getItems().clear();
                    Arrays.stream(histories.stream().map(HistoryResultController::getSongsView).toArray(BorderPane[][]::new)).forEach(e -> eventsViewTable.getItems().addAll(e));
                    break;

                case "All":
                    eventsViewTable.getItems().clear();
                    Arrays.stream(currentlyDownloading.stream().map(CurrentlyDownloadingResultController::getSongsView).toArray(BorderPane[][]::new)).forEach(e -> eventsViewTable.getItems().addAll(e));
                    Arrays.stream(queued.stream().map(QueuedResultController::getSongsView).toArray(BorderPane[][]::new)).forEach(e -> eventsViewTable.getItems().addAll(e));
                    Arrays.stream(histories.stream().map(HistoryResultController::getSongsView).toArray(BorderPane[][]::new)).forEach(e -> eventsViewTable.getItems().addAll(e));
                    break;

                default:
                    eventViewTitle.setText(eventViewSelector.getSelectionModel().getSelectedItem());
            }

        }

        albumViewSelectorWrapper.getStyleClass().setAll();
        songViewSelectorWrapper.getStyleClass().setAll("underline2");

    }

    @FXML
    public void selectionUpdate(ActionEvent event) {

        if (albumViewSelectorWrapper.getStyleClass().contains("underline2")) albumsView();
        else songsView();

        event.consume();

    }

    private void defaultView() {
        albumViewSelectorWrapper.setVisible(false);
        songViewSelectorWrapper.setVisible(false);

        viewContainer.getChildren().clear();
        viewContainer.setAlignment(Pos.CENTER);

        Label defaultMessage = new Label("Files you download appear here");
        defaultMessage.getStyleClass().add("sub_title1");

        ImageView iconImage = new ImageView(
                new Image(
                        Main.class.getResourceAsStream("resources/img/icon.png"),
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
        defaultInfoContainer.setPadding(new javafx.geometry.Insets(0, 0, 40, 0));

        viewContainer.getChildren().setAll(defaultInfoContainer);

        Debug.trace("Finished default view.");

    }

    public void markSongCompleted(int songIndex) {

        CurrentlyDownloadingResultController activeDownloader = currentlyDownloading.get(currentlyDownloading.size() -1);

        Platform.runLater(() -> {
            activeDownloader.album.setProgress((double) (songIndex + 1) / (activeDownloader.songs.size()));
            activeDownloader.songs.get(songIndex).setProgress(1);
        });

    }

    public void markDownloadCompleted() {

        Platform.runLater(() -> {
            currentlyDownloading.get(currentlyDownloading.size() - 1).markCompleted();

            if (queued.size() > 0) {

                queued.remove(0);

                try {
                    currentlyDownloading.add(new CurrentlyDownloadingResultController(Model.getInstance().download.getDownloadObject()));
                } catch (JSONException e) {
                    Debug.error("Failed to create new currently downloading object.", e);
                }

                if (albumViewSelectorWrapper.getStyleClass().contains("underline2")) albumsView();
                else songsView();

                if (queued.size() == 0) {
                    eventViewSelector.getItems().remove("Download Queue");

                    if (eventViewSelector.getItems().size() == 2) {

                        eventViewSelector.setVisible(false);
                        eventViewTitle.setText("Currently Downloading");

                    }

                }

            }
        });
    }

    public static class CurrentlyDownloadingResultController {

        private final CurrentlyDownloadingResult album;
        private final ArrayList<CurrentlyDownloadingResult> songs = new ArrayList<>();

        private final JSONObject downloadObject;

        public CurrentlyDownloadingResultController(JSONObject downloadObject) throws JSONException {

            this.downloadObject = downloadObject;

            this.album = new CurrentlyDownloadingResult(downloadObject.getJSONObject("metadata").getString("album"));
            for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++)
                songs.add(
                        new CurrentlyDownloadingResult(downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"))
                );

            int completed = 0;
            for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {
                if (downloadObject.getJSONArray("songs").getJSONObject(i).getBoolean("completed")) {
                    this.songs.get(i).setProgress(1);
                    completed++;

                } else this.songs.get(i).setProgress(0);
            }
            this.album.setProgress((double) completed / downloadObject.getJSONArray("songs").length());
        }

        public BorderPane[] getSongsView() {
            return songs.stream().map(Result::getView).toArray(BorderPane[]::new);
        }

        public BorderPane getAlbumView() {
            return album.getView();
        }

        public void markCompleted() {

            ColorAdjust markFinished = new ColorAdjust(
                    0, 0, Model.getInstance().settings.getSettingBool("dark_theme") ? 1 : 0, 0
            );

            album.getView().getRight().setEffect(markFinished);
            songs.forEach(e -> e.getView().getRight().setEffect(markFinished));

        }

        private class CurrentlyDownloadingResult extends Result {

            public CurrentlyDownloadingResult(String title) throws JSONException {

                super(
                        String.format(
                                "%scached/%s.jpg",
                                Resources.getInstance().getApplicationData(),
                                downloadObject.getJSONObject("metadata").getString("artId")
                        ),
                        downloadObject.getJSONObject("metadata").getString("art"),
                        false,
                        title,
                        downloadObject.getJSONObject("metadata").getString("artist")
                );

            }

            private void setProgress(double progress) {

                try {
                    switch ((int) (progress * 10)) {

                        case 0:
                            right.getChildren().setAll(new ProgressIndicator(-1));
                            break;

                        case 10:
                            right.getChildren().setAll(
                                    new ImageView(
                                            new Image(
                                                    Main.class.getResource("resources/img/tick.png").toURI().toString(),
                                                    25,
                                                    25,
                                                    true,
                                                    true
                                            )
                                    )
                            );
                            break;

                        default:
                            ProgressIndicator partialCompletion = new ProgressIndicator(progress);
                            partialCompletion.getStyleClass().add("progress-indicator-percentage");
                            right.getChildren().setAll(partialCompletion);
                            break;

                    }
                } catch (URISyntaxException e) {
                    Debug.error("URI Syntax exception loading tick.", e);
                }
                view.setRight(right);

            }

        }

    }

    public class QueuedResultController {

        private final QueuedResult album;
        private final ArrayList<QueuedResult> songs = new ArrayList<>();
        private final JSONObject queuedObject;

        public QueuedResultController(JSONObject queuedObject) throws JSONException {

            this.queuedObject = queuedObject;
            this.album = new QueuedResult(queuedObject.getJSONObject("metadata").getString("album"), true);
            for (int i = 0; i < queuedObject.getJSONArray("songs").length(); i++) songs.add(new QueuedResult(queuedObject.getJSONArray("songs").getJSONObject(i).getString("title"), false));

        }

        private class QueuedResult extends Result {

            public QueuedResult(String title, boolean isAlbum) throws JSONException {

                super(
                        String.format(
                                "%scached/%s.jpg",
                                Resources.getInstance().getApplicationData(),
                                queuedObject.getJSONObject("metadata").getString("artId")
                        ),
                        queuedObject.getJSONObject("metadata").getString("art"),
                        false,
                        title,
                        queuedObject.getJSONObject("metadata").getString("artist")
                );

                try {
                    ImageView scheduledIcon = new ImageView(
                            new Image(
                                    Main.class.getResource("resources/img/scheduled.png").toURI().toString(),
                                    25,
                                    25,
                                    true,
                                    true
                            )
                    );
                    scheduledIcon.setEffect(
                            new ColorAdjust(
                                    0,
                                    0,
                                    Model.getInstance().settings.getSettingBool("dark_theme") ? 1 : -1,
                                    0
                            )
                    );
                    right.getChildren().setAll(scheduledIcon);
                    view.setRight(right);

                    MenuItem cancelSong = new MenuItem("Cancel Song");
                    cancelSong.setOnAction(this::cancelSong);

                    MenuItem cancel = new MenuItem("Cancel");
                    cancel.setOnAction(this::cancel);

                    if (isAlbum) menu.getItems().addAll(cancel);
                    else menu.getItems().addAll(cancelSong);

                } catch (URISyntaxException e) {
                    Debug.error("Failed to load path to for scheduled icon.", e);
                }

            }

            private void cancelSong(ActionEvent event) {

                if (songs.size() == 1) cancel(event);
                else {

                    JSONArray newSongs = new JSONArray();
                    JSONObject backup = new JSONObject(queuedObject);
                    JSONArray newModelData = new JSONArray();

                    try {
                        for (int i = 0; i < queuedObject.getJSONArray("songs").length(); i++)
                            if (i != songs.indexOf(this))
                                newSongs.put(queuedObject.getJSONArray("songs").getJSONObject(i));

                        queuedObject.put("songs", newSongs);

                        for (int i = 0; i < Model.getInstance().download.getDownloadQueue().length(); i++)
                            newModelData.put(!Model.getInstance().download.getDownloadQueue().getJSONObject(i).toString().equals(backup.toString()) ? Model.getInstance().download.getDownloadQueue().getJSONObject(i) : queuedObject);

                    } catch (JSONException e) {
                        Debug.error("Failed to parse data to remove song from model.", e);
                    }

                    Model.getInstance().download.setDownloadQueue(newModelData);

                    eventsViewTable.getItems().remove(this.getView());
                    songs.remove(this);
                }

                event.consume();

            }

            private void cancel(ActionEvent event) {

                eventsViewTable.getItems().remove(this.getView());
                queued.remove(QueuedResultController.this);

                JSONArray queued = Model.getInstance().download.getDownloadQueue();
                JSONArray newQueued = new JSONArray();

                try {
                    for (int i = 0; i < queued.length(); i++)
                        if (!queued.getJSONObject(i).toString().equals(queuedObject.toString()))
                            newQueued.put(queued.getJSONObject(i));
                } catch (JSONException e) {
                    Debug.error("Failed to parse queue to remove item.", e);
                }

                Model.getInstance().download.setDownloadQueue(newQueued);
                event.consume();
            }

        }

        public BorderPane[] getSongsView() {
            return songs.stream().map(Result::getView).toArray(BorderPane[]::new);
        }

        public BorderPane getAlbumView() {
            return album.getView();
        }

    }

    public class HistoryResultController {

        private final HistoryResult album;
        private final ArrayList<HistoryResult> songs = new ArrayList<>();
        private final JSONObject historyItem;

        public HistoryResultController(JSONObject historyItem) throws JSONException {

            this.historyItem = historyItem;

            int existingFiles = 0;
            for (int i = 0; i < historyItem.getJSONArray("songs").length(); i++) {

                String title = historyItem.getJSONArray("songs").getJSONObject(i).getString("title");
                int checkedFile = Files.exists(
                        Paths.get(
                                String.format(
                                        "%s/%s.%s",
                                        historyItem.getJSONObject("metadata").getString("directory"),
                                        title.replaceAll("[\u0000-\u001f<>:\"/\\\\|?*\u007f]+", "_"),
                                        historyItem.getJSONObject("metadata").getString("format")
                                )
                        )
                ) ? 1 : 0;

                // TODO: TESTING, DELETE
                // checkedFile = Math.random() > 0.5 ? 1 : 0;

                songs.add(new HistoryResult(title, false, checkedFile));

                existingFiles += checkedFile;

            }
            this.album = new HistoryResult(historyItem.getJSONObject("metadata").getString("album"), true, existingFiles);

        }

        public BorderPane[] getSongsView() {
            return songs.stream().map(Result::getView).toArray(BorderPane[]::new);
        }

        public BorderPane getAlbumView() {
            return album.getView();
        }

        protected class HistoryResult extends Result {

            private final Line crossLine0 = new Line(20, 0, 0, 20);
            private final Line crossLine1 = new Line(20, 20, 0, 0);

            private final boolean isAlbum;
            private final String resultTitle;
            private int filesExist;

            private final MenuItem deleteLocal;
            private final MenuItem deleteBoth = new MenuItem("Delete Both");

            private final MenuItem reacquireFiles = new MenuItem("Download Missing Files");

            protected int missingFiles;

            public HistoryResult(String resultTitle, boolean isAlbum, int filesExist) throws JSONException {

                super(
                        String.format(
                                "%scached/%s.jpg",
                                Resources.getInstance().getApplicationData(),
                                historyItem.getJSONObject("metadata").getString("artId")
                        ),
                        historyItem.getJSONObject("metadata").getString("art"),
                        false,
                        resultTitle,
                        historyItem.getJSONObject("metadata").getString("artist")
                );

                this.isAlbum = isAlbum;
                this.resultTitle = resultTitle;
                this.filesExist = filesExist;

                this.deleteLocal = new MenuItem("Delete File" + (isAlbum ? "s" : ""));

                // Generate Cross
                crossLine0.getStyleClass().add("cross-line");
                crossLine1.getStyleClass().add("cross-line");

                HBox crossView = new HBox();
                crossView.setAlignment(Pos.CENTER);
                crossView.setCursor(Cursor.HAND);
                crossView.setPadding(new Insets(0, 10, 0, 0));
                crossView.setOnMouseEntered(this::selectCross);

                if (isAlbum) crossView.setOnMouseClicked(e -> {deleteAlbumHistory(); e.consume();});
                else crossView.setOnMouseClicked(e -> {deleteSongHistory(); e.consume();});

                crossView.setOnMouseExited(this::unselectCross);
                crossView.getChildren().setAll(new Group(crossLine0, crossLine1));

                view.setRight(crossView);

                // Generate Warnings
                if (filesExist == 0) markUnOpenable();
                else {

                    view.setOnMouseEntered(this::selectTitle);
                    view.setOnMouseClicked(this::openFiles);
                    view.setOnMouseExited(this::unselectTitle);
                    view.setCursor(Cursor.HAND);

                    if (isAlbum && filesExist < historyItem.getJSONArray("songs").length()) {
                        setSubtext(
                                String.format(
                                        "%s File%s Moved or Deleted.",
                                        (missingFiles = historyItem.getJSONArray("songs").length() - filesExist),
                                        missingFiles == 1 ? "" : "s"
                                )
                        );
                    }

                }

                // Generate Context Menu
                MenuItem deleteHistory = new MenuItem("Delete from History");
                if (isAlbum) deleteHistory.setOnAction(e -> deleteAlbumHistory());
                else deleteHistory.setOnAction(e -> deleteSongHistory());

                if (filesExist > 0) {

                    deleteLocal.setOnAction(this::deleteLocalFiles);

                    if (isAlbum) {
                        deleteBoth.setOnAction(e -> {
                            deleteAlbumHistory();
                            deleteLocalFiles(e);
                        });
                    } else {
                        deleteBoth.setOnAction(e -> {
                            deleteSongHistory();
                            deleteLocalFiles(e);
                        });
                    }

                    menu.getItems().addAll(deleteLocal, deleteBoth);

                    if (filesExist < historyItem.getJSONArray("songs").length()) {
                        reacquireFiles.setOnAction(this::reacquireFiles);
                    }

                }

                menu.getItems().add(deleteHistory);

            }

            private synchronized void deleteSongHistory() {

                eventsViewTable.getItems().remove(this.view);

                try {
                    Model.getInstance().download.deleteHistory(historyItem.getJSONArray("songs").getJSONObject(songs.indexOf(this)));
                } catch (JSONException e) {
                    Debug.error("Failed to parse JSON to delete history.", e);
                }

                songs.remove(this);

                if (songs.size() == 0) deleteAlbumHistory();


            }

            private synchronized void deleteAlbumHistory() {

                eventsViewTable.getItems().remove(this.view);
                histories.remove(HistoryResultController.this);

                try {
                    for (int i = 0; i < historyItem.getJSONArray("songs").length(); i++)
                        Model.getInstance().download.deleteHistory(historyItem.getJSONArray("songs").getJSONObject(i));
                } catch (JSONException e) {
                    Debug.error("Failed to parse JSON to delete history.", e);
                }

                if (histories.size() == 0 && currentlyDownloading.size() == 0) defaultView();

            }

            private synchronized void selectCross(MouseEvent e) {
                crossLine0.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.RED : Color.BLACK);
                crossLine1.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.RED : Color.BLACK);
                e.consume();
            }

            private synchronized void unselectCross(MouseEvent e) {
                crossLine0.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.rgb(193, 199, 201) : Color.GRAY);
                crossLine1.setStroke(Model.getInstance().settings.getSettingBool("dark_theme") ? Color.rgb(193, 199, 201) : Color.GRAY);
                e.consume();
            }

            private synchronized void selectTitle(MouseEvent e) {
                title.getStyleClass().add("sub_title1_selected");
            }

            private synchronized void unselectTitle(MouseEvent e) {
                title.getStyleClass().remove("sub_title1_selected");
            }

            private synchronized void openFiles(MouseEvent event) {

                if (event.getButton() == MouseButton.PRIMARY) {
                    try {

                        File openFile = new File(
                                isAlbum ?
                                        historyItem.getJSONObject("metadata").getString("directory") :
                                        historyItem.getJSONObject("metadata").getString("directory") + "/" + resultTitle + "." + historyItem.getJSONObject("metadata").getString("format")
                        );

                        if (openFile.exists()) Desktop.getDesktop().open(openFile);
                        else markUnOpenable();

                        if (!isAlbum) {

                            album.setFilesExist(album.getFilesExist() - 1);
                            album.setSubtext(
                                    String.format(
                                            "%s File%s Moved or Deleted.",
                                            (album.missingFiles = historyItem.getJSONArray("songs").length() - album.filesExist),
                                            album.missingFiles == 1 ? "" : "s"
                                    )
                            );
                        }

                    } catch (IOException | IllegalArgumentException ignored) {

                        Debug.warn("File was detected as existing but failed to be opened.");
                        markUnOpenable();

                        // Rebuild song download histories
                        // Internal call to redrawn songs and call method to set subtext to inform they don't exist

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        event.consume();
                    }
                }
            }

            private synchronized void markUnOpenable() {

                view.setCursor(Cursor.DEFAULT);
                view.setOnMouseClicked(null);
                albumArt.setEffect(new ColorAdjust(0, -1, 0, 0));
                setSubtext( (isAlbum ? "All Files" : "File") + " Moved or Deleted.");
                title.getStyleClass().add("sub_title1_strikethrough");

                if (isAlbum) songs.forEach(HistoryResult::markUnOpenable);

                menu.getItems().remove(deleteLocal);
                menu.getItems().remove(deleteBoth);

            }

            private void deleteLocalFiles(ActionEvent event) {

                try {
                    if (isAlbum) {

                        try {
                            FileUtils.deleteDirectory(
                                    new File(historyItem.getJSONObject("metadata").getString("directory"))
                            );
                            markUnOpenable();
                        } catch (IOException e) {
                            Debug.warn("Failed to delete album directory.");
                        }

                    } else {

                        if (
                                !new File(
                                        historyItem.getJSONObject("metadata").getString("directory")
                                                + "/"
                                                + resultTitle
                                                + "."
                                                + historyItem.getJSONObject("metadata").getString("format")
                                ).delete()
                        ) Debug.warn("Failed to delete song.");
                        else markUnOpenable();

                    }
                } catch (JSONException e) {
                    Debug.error("Failed to parse JSON to delete local item.", e);
                }

            }

            private void reacquireFiles(ActionEvent e) {

                menu.getItems().remove(reacquireFiles);

            }

            protected int getFilesExist() {
                return filesExist;
            }

            protected void setFilesExist(int filesExist) {
                this.filesExist = filesExist;
            }
        }
    }
}
