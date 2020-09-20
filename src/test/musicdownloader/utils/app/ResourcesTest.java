package musicdownloader.utils.app;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

class ResourcesTest {

    @Test
    public void remoteResourcesCheck() {

        List<String> remoteResources = Arrays.asList(
                Resources.remoteVersionUrl,
                String.format(Resources.mp3Source, "0"),
                Resources.youtubeVideoSource,
                Resources.youtubeSearch,
                Resources.vimeoVideoSource,
                Resources.vimeoSearch
        );

        for (String resource: remoteResources) {

            try {
                Jsoup.connect(resource).get();

            } catch (HttpStatusException e) {

                Debug.warn(e.getStatusCode() == 404 ? "Missing Resource: " + resource : e.getStatusCode() + " error accessing resource: " + resource);
                assert false;

            } catch (IOException e) {

                Debug.warn("Likely connection failure detected.");
                assert false;

            }

        }

    }

}