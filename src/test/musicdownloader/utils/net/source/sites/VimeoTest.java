package musicdownloader.utils.net.source.sites;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

// todo rewrite using the new method of running ie w/ latches
class VimeoTest {

    @Test
    void validRequest() {

        try {
            Vimeo vimeoRequest = new Vimeo("The Rolling Stones - Paint it Black", 237);

            assert vimeoRequest.getResults().getJSONArray("primary").length() > 0 || vimeoRequest.getResults().getJSONArray("secondary").length() > 0;
        } catch (JSONException e) {
            assert false;
        }

    }

    @Test
    void invalidRequest() {

        try {
            Vimeo vimeoRequest = new Vimeo("%%%%%%%%%%%%", -1);

        } catch (JSONException e) {
            assert false;
        }

    }

}