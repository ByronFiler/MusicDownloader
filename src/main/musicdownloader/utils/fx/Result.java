package musicdownloader.utils.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import musicdownloader.Main;
import musicdownloader.utils.app.Debug;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Result {

    protected BorderPane view;

    protected final HBox left;
    protected final ImageView albumArt;
    protected final BorderPane leftTextContainer;
    protected final BorderPane imageContainer;
    protected final Label title;

    protected HBox right;

    public Result(
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
            albumArt.setFitHeight(85);
            albumArt.setFitWidth(85);

        } else {
            if (forceLoadRemote) fetchRemoteResource(remoteArtResource);

            else
                new Thread(() -> {
                    useFallbackAlbumArt();
                    fetchRemoteResource(remoteArtResource);
                }, "album-art-loader").start();

        }

        this.title = new Label(title);
        this.title.getStyleClass().setAll("sub_title1");

        Label artistLabel = new Label(artist);
        artistLabel.getStyleClass().setAll("sub_title2");

        VBox songArtistContainer = new VBox(this.title, artistLabel);
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
    }

    private void useFallbackAlbumArt() {

        try {
            albumArt.setImage(
                    new Image(
                            Main.class.getResource("resources/img/song_default.png").toURI().toString(),
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
}
