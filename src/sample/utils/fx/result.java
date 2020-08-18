package sample.utils.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import sample.Main;
import sample.utils.app.debug;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class result {

    protected final BorderPane view;

    protected final HBox left;
    protected final ImageView albumArt;
    protected final BorderPane leftTextContainer;
    protected final BorderPane imageContainer;

    protected HBox right;

    public result(
            String localArtResource,
            String remoteArtResource,
            boolean forceLoadRemote,
            String title,
            String artist
    ) {
        view = new BorderPane();

        imageContainer = new BorderPane();
        imageContainer.setMinSize(85, 85);
        imageContainer.setPrefSize(85, 85);

        albumArt = new ImageView();
        leftTextContainer = new BorderPane();

        right = new HBox();

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
            if (forceLoadRemote)
                fetchRemoteResource(remoteArtResource);

            else
                new Thread(() -> {
                    useFallbackAlbumArt();
                    fetchRemoteResource(remoteArtResource);
                }, "album-art-loader").start();

        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().setAll("sub_title1");

        Label artistLabel = new Label(artist);
        artistLabel.getStyleClass().setAll("sub_title2");

        VBox songArtistContainer = new VBox(titleLabel, artistLabel);
        songArtistContainer.setAlignment(Pos.TOP_LEFT);

        leftTextContainer.setTop(songArtistContainer);
        leftTextContainer.setPadding(new Insets(0, 0, 0, 5));

        right.setPadding(new Insets(0, 10, 0, 0));
        right.setAlignment(Pos.CENTER);
        right.setMaxWidth(40);

        imageContainer.getChildren().add(albumArt);

        left = new HBox(imageContainer, leftTextContainer);

        view.setLeft(left);
        view.getStyleClass().add("result");
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

    private void fetchRemoteResource(String remoteArtResource) {

        try {

            if (InetAddress.getByName("allmusic.com").isReachable(1000)) {
                albumArt.setImage(
                        new Image(
                                remoteArtResource,
                                85,
                                85,
                                true,
                                true
                        )
                );

            } else
                throw new IOException();

        } catch (IOException e) {

            debug.warn(null, "Failed to connect to allmusic to get album art, using default.");

            if (albumArt.getImage() == null)
                useFallbackAlbumArt();
        }

    }

    private void useFallbackAlbumArt() {

        try {
            albumArt.setImage(
                    new Image(
                            Main.class.getResource("app/img/song_default.png").toURI().toString(),
                            85,
                            85,
                            true,
                            true
                    )
            );
        } catch (URISyntaxException er) {
            debug.error(null, "Failed to set default album art.", er);
        }

    }
}
