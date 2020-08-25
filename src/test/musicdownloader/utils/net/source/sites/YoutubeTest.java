package musicdownloader.utils.net.source.sites;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class YoutubeTest {

    @Test
    void load() {
        Youtube youtubeRequest;

        // Known working request
        youtubeRequest = new Youtube("Pink Floyd - Speak To Me", 67);
        try {
            youtubeRequest.load();
            assert youtubeRequest.getResults().length() > 0;
        } catch (IOException e) {
            assert false;
        }

        // Bad request
        youtubeRequest = new Youtube("%%%%%%%%", -1);
        try {
            youtubeRequest.load();
            assert  youtubeRequest.getResults().length() == 0;
        } catch (IOException e) {
            assert false;
        }
    }
}