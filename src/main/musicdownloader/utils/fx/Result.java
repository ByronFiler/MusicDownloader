package musicdownloader.utils.fx;

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
import musicdownloader.Main;
import musicdownloader.utils.app.Debug;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Result {

    protected final BorderPane view = new BorderPane();

    protected final HBox left = new HBox();
    protected final ImageView albumArt = new ImageView();
    protected final BorderPane leftTextContainer = new BorderPane();
    protected final BorderPane imageContainer = new BorderPane();
    protected final Label title = new Label();

    protected final HBox right = new HBox();
    protected final ContextMenu menu = new ContextMenu();

    public Result(
            String localArtResource,
            String remoteArtResource,
            boolean forceLoadRemote,
            String title,
            String artist
    ) {
        sharedInitialisation(title, artist);
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
            if (forceLoadRemote) fetchRemoteResource(remoteArtResource);

            else
                new Thread(() -> {
                    useFallbackAlbumArt();

                    try {
                        Thread.sleep((long) (Math.random() * 1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    fetchRemoteResource(remoteArtResource);
                }, "album-art-loader").start();

        }

        imageContainer.getChildren().add(albumArt);
        left.getChildren().setAll(imageContainer, leftTextContainer);
        view.setLeft(left);
    }

    // TODO: Fallback use for downloads when album art doesn't exist
    protected Result(Image albumArt, String title, String artist) {

        sharedInitialisation(title, artist);

        this.albumArt.setImage(albumArt);
        imageContainer.getChildren().add(this.albumArt);

        left.getChildren().setAll(imageContainer, leftTextContainer);
        view.setLeft(left);
    }

    private void sharedInitialisation(String title, String artist) {

        this.title.setText(title);
        this.title.getStyleClass().setAll("sub_title1");

        Label artistLabel = new Label(artist);
        artistLabel.getStyleClass().setAll("sub_title2");

        VBox songArtistContainer = new VBox(this.title, artistLabel);
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
