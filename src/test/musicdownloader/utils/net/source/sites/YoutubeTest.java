package musicdownloader.utils.net.source.sites;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

// todo rewrite with new method of running
class YoutubeTest {

    @Test
    void validRequest() {
        try {
            YouTube youtubeRequest = new YouTube("Pink Floyd - Speak To Me", 67);
            assert youtubeRequest.getResults().getJSONArray("primary").length() > 0 || youtubeRequest.getResults().getJSONArray("secondary").length() > 0;
        } catch (JSONException e) {
            assert false;
        }
    }

    @Test
    void invalidRequest() {
        try {
            YouTube youtubeRequest = new YouTube("%%%%%%%%", -1);
        } catch (JSONException e) {
            assert false;
        }

    }
}