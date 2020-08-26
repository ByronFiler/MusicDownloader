package musicdownloader.utils.net.db;

import javafx.scene.layout.BorderPane;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public interface Search {

    void query(Boolean useDefaultIcons) throws IOException;

    ArrayList<BorderPane> buildView();

    JSONObject getResults();

    int getAlbumCount();

    int getSongCount();

}
