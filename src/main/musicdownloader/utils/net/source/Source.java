package musicdownloader.utils.net.source;

import org.json.JSONArray;

import java.io.IOException;

public interface Source {

    void load() throws IOException;

    JSONArray getResults();
}
