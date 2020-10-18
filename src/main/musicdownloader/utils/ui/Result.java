package musicdownloader.utils.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import musicdownloader.utils.app.Debug;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Result {

    protected final BorderPane view = new BorderPane();

    protected final HBox left = new HBox();
    protected final ImageView albumArt = new ImageView();
    protected final BorderPane leftTextContainer = new BorderPane();
    protected final BorderPane imageContainer = new BorderPane();
    protected final HBox titleContainer = new HBox();
    protected final Label title = new Label();

    protected final HBox right = new HBox();
    protected final ContextMenu menu = new ContextMenu();

    protected boolean albumArtRendered = false;
    protected boolean threadRunning = false;
    protected final String remoteArtResource;

    public Result(
            String localArtResource,
            String remoteArtResource,
            boolean forceLoadRemote,
            String title,
            String artist
    ) {

        this.title.setText(title);
        this.title.getStyleClass().setAll("sub_title1");

        this.remoteArtResource = remoteArtResource;

        Label artistLabel = new Label(artist);
        artistLabel.getStyleClass().setAll("sub_title2");

        titleContainer.getChildren().add(this.title);

        VBox songArtistContainer = new VBox(titleContainer, artistLabel);
        songArtistContainer.setAlignment(Pos.TOP_LEFT);

        leftTextContainer.setTop(songArtistContainer);
        leftTextContainer.setPadding(new Insets(0, 0, 0, 5));

        imageContainer.setMinSize(85, 85);
        imageContainer.setPrefSize(85, 85);

        albumArt.setFitHeight(85);
        albumArt.setFitWidth(85);

        right.setPadding(new Insets(0, 10, 0, 0));
        right.setAlignment(Pos.CENTER);
        right.setMaxWidth(40);

        view.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            menu.show(
                    view,
                    event.getScreenX(),
                    event.getScreenY()
            );
            event.consume();
        });
        view.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> menu.hide());
        view.getStyleClass().add("result");

        if (localArtResource != null && Files.exists(Paths.get(localArtResource))) {

            albumArt.setImage(
                    new Image(
                            new File(localArtResource).toURI().toString(),
                            85,
                            85,
                            true,
                            true
                    )
            );

        } else {

            useFallbackAlbumArt();

            if (forceLoadRemote) {
                new loadAlbumArt(remoteArtResource).run();
            } else {
                Thread albumArtLoader = new Thread(new loadAlbumArt(remoteArtResource), "album-art-loader");
                albumArtLoader.setDaemon(true);
                albumArtLoader.start();
            }
        }

        imageContainer.getChildren().add(albumArt);
        left.getChildren().setAll(imageContainer, leftTextContainer);
        view.setLeft(left);
    }

    public synchronized BorderPane getView() {
        return view;
    }

    public void setSubtext(String warningMessage) {

        Label warning = new Label(warningMessage);
        warning.getStyleClass().add("sub_text3");

        leftTextContainer.setBottom(warning);
        view.setLeft(new HBox(imageContainer, leftTextContainer));

    }

    public synchronized void setAlbumArt(Image art) {
        albumArt.setImage(art);
    }

    protected void fetchRemoteResource(String remoteArtResource) {
        Image albumArtImage = new Image(
                remoteArtResource,
                85,
                85,
                true,
                true
        );

        if (albumArtImage.getHeight() == 0) useFallbackAlbumArt();
        else albumArt.setImage(albumArtImage);

        albumArtRendered = true;
    }

    private void useFallbackAlbumArt() {

        try {
            albumArt.setImage(
                    new Image(
                            Objects.requireNonNull(getClass().getClassLoader().getResource("img/misc/song_default.png")).toURI().toString(),
                            85,
                            85,
                            true,
                            true
                    )
            );
        } catch (URISyntaxException er) {
            Debug.error("Failed to set default album art.", er);
        }

    }

    protected class loadAlbumArt implements Runnable {

        private final String remoteArtResource;

        public loadAlbumArt(String remoteArtResource) {
            threadRunning = true;
            this.remoteArtResource = remoteArtResource;
        }

        @Override
        public void run() {

            if (remoteArtResource == null) useFallbackAlbumArt();
            else {
                Image albumArtImage = new Image(
                        remoteArtResource,
                        85,
                        85,
                        true,
                        true
                );

                if (albumArtImage.getHeight() == 0) useFallbackAlbumArt();
                else {
                    albumArt.setImage(albumArtImage);
                    albumArtRendered = true;
                }
            }

            threadRunning = false;
        }

    }
}
