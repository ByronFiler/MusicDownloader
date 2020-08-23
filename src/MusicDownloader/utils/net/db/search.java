package MusicDownloader.utils.net.db;

import javafx.scene.layout.BorderPane;
import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;

public interface search {

    void query(Boolean useDefaultIcons) throws IOException;

    ArrayList<BorderPane> buildView();

    JSONArray getSearchResultsData();

    int getAlbumCount();

    int getSongCount();

}
