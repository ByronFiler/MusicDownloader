package musicdownloader.utils.net.source.sites;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class YoutubeTest {

    @Test
    void validRequest() {
        try {
            YouTube youtubeRequest = new YouTube("Pink Floyd - Speak To Me", 67);
            youtubeRequest.load();
            assert youtubeRequest.getResults().getJSONArray("primary").length() > 0 || youtubeRequest.getResults().getJSONArray("secondary").length() > 0;
        } catch (IOException | JSONException e) {
            assert false;
        }
    }

    @Test
    void invalidRequest() {
        try {
            YouTube youtubeRequest = new YouTube("%%%%%%%%", -1);
            youtubeRequest.load();
        } catch (IOException | JSONException e) {
            assert false;
        }

    }
}