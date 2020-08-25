package musicdownloader.utils.net.db;

import javafx.scene.layout.BorderPane;
import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;

public interface Search {

    void query(Boolean useDefaultIcons) throws IOException;

    ArrayList<BorderPane> buildView();

    JSONArray getSearchResultsData();

    int getAlbumCount();

    int getSongCount();

}
