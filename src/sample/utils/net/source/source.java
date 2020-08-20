package sample.utils.net.source;

import org.json.JSONArray;

import java.io.IOException;

public interface source {

    void load() throws IOException;

    JSONArray getResults();
}
