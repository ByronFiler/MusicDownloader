package musicdownloader.utils.net.source.sites;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class YoutubeTest {

    @Test
    void load() {
        YouTube youtubeRequest;

        // Known working request
        try {
            youtubeRequest = new YouTube("Pink Floyd - Speak To Me", 67);
            youtubeRequest.load();
            assert youtubeRequest.getResults().getJSONArray("primary").length() > 0 || youtubeRequest.getResults().getJSONArray("secondary").length() > 0;
        } catch (IOException | JSONException e) {
            assert false;
        }

        // Bad request
        try {
            youtubeRequest = new YouTube("%%%%%%%%", -1);
            youtubeRequest.load();
            assert  youtubeRequest.getResults().getJSONArray("primary").length() == 0 || youtubeRequest.getResults().getJSONArray("secondary").length() == 0;
        } catch (IOException | JSONException e) {
            assert false;
        }
    }
}