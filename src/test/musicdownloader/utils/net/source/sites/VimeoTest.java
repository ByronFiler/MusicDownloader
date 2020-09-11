package musicdownloader.utils.net.source.sites;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class VimeoTest {

    @Test
    void validRequest() {

        try {
            Vimeo vimeoRequest = new Vimeo("The Rolling Stones - Paint it Black", 237);
            vimeoRequest.load();

            assert vimeoRequest.getResults().getJSONArray("primary").length() > 0 || vimeoRequest.getResults().getJSONArray("secondary").length() > 0;
        } catch (IOException | JSONException e) {
            assert false;
        }

    }

    @Test
    void invalidRequest() {

        try {
            Vimeo vimeoRequest = new Vimeo("%%%%%%%%%%%%", -1);
            vimeoRequest.load();
        } catch (IOException | JSONException e) {
            assert false;
        }

    }

}