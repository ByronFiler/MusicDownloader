package musicdownloader.utils.net.source.sites;

import com.sun.nio.sctp.IllegalReceiveException;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.io.QuickSort;
import musicdownloader.utils.net.source.Site;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class YouTube extends Site {
    public YouTube(String query, int targetTime){
        super(query, targetTime);
    }

    @Override
    public void run() {

        try {
            requestedPage = Jsoup.connect(Resources.youtubeSearch + query).get();

            if (requestedPage.select("script").size() == 17) parseHTMLResponse();
            else parseJSONResponse();
        } catch (IOException e) {
            Debug.log("Failed to connect to: " + Resources.youtubeSearch + query);
        }

        latch.countDown();

    }

    private void parseJSONResponse() {

        Debug.log("JSON Response received from youtube.");

        // YouTube has given us the data stored in json stored script tags which be parsed
        JSONObject searchDataTemp;

        AtomicReference<Element> jsDataAtm = new AtomicReference<>();
        requestedPage.select("script").forEach(script -> {

            if (script.toString().contains("window[\"ytInitialData\"]")) {
                jsDataAtm.set(script);
            }

        });

        if (jsDataAtm.get() == null) {
            //jsDataAtm.set(requestedPage.select("script").get(24));
            Debug.error("Failed to find youtube response.", new IllegalReceiveException());
        }

        try {
            // Web Data -> [JavaScript] -> String -> Json -> Data
            Element jsData = jsDataAtm.get();

            //Element jsData = requestedPage.select("script").get(24);

            // Web Data -> JavaScript -> [String] -> Json -> Data
            String jsonConversion = jsData.toString();
            jsonConversion = jsonConversion.substring(39, jsonConversion.length() - 119);

            // Web Data -> JavaScript -> String -> [Json] -> Data
            JSONObject json = new JSONObject();
            try {
                json = new JSONObject(jsonConversion);
            } catch (JSONException e) {

                // TODO: ANALYSE THIS
                requestedPage.select("script").forEach(System.out::println);

                Debug.error("Failed to parse JSON response.", e);

            }

            // Parsing deep JSON to get relevant data
            JSONArray contents = json
                    .getJSONObject("contents")
                    .getJSONObject("twoColumnSearchResultsRenderer")
                    .getJSONObject("primaryContents")
                    .getJSONObject("sectionListRenderer")
                    .getJSONArray("contents")
                    .getJSONObject(0)
                    .getJSONObject("itemSectionRenderer")
                    .getJSONArray("contents");

            // If youtube gives a bad response, just retry
            if (contents.length() < 10) {
                Debug.warn(String.format("Youtube sent a bad response, resent request, %s retr%s remaining.", retries, retries == 1 ? "y" : "ies"));
                if (retries == 0) return;
                else {
                    retries--;
                    run();
                }
            }

            for (int i = 0; i < contents.length(); i++) {

                try {
                    int length = timeConversion(
                            contents
                                    .getJSONObject(i)
                                    .getJSONObject("videoRenderer")
                                    .getJSONObject("lengthText")
                                    .getString("simpleText")
                    );

                    // Checks that the length is within 15% either way of the target time, otherwise definitely not relevant.
                    if (length < targetTime * 1.05 && length > targetTime / 1.05) {

                        results.getJSONArray("primary").put(
                                contents
                                        .getJSONObject(i)
                                        .getJSONObject("videoRenderer")
                                        .getString("videoId")
                        );

                    } else if (length < targetTime * 1.15 && length > targetTime / 1.15) {

                        searchDataTemp = new JSONObject();
                        searchDataTemp.put(
                                "watch_id",
                                contents
                                        .getJSONObject(i)
                                        .getJSONObject("videoRenderer")
                                        .getString("videoId")
                        );
                        searchDataTemp.put("difference", Math.abs(length - targetTime));

                        results.getJSONArray("secondary").put(searchDataTemp);

                    }

                } catch (JSONException ignored) {} // Youtube adds random elements that are tricky to handle and are best ignored
            }

            results.put("secondary", new QuickSort(results.getJSONArray("secondary"), 0, results.getJSONArray("secondary").length() - 1).getSorted());

        } catch (JSONException e) {

            Debug.warn(String.format("Youtube sent a bad response, resent request, %s retr%s remaining.", retries, retries == 1 ? "y" : "ies"));
            if (retries > 0) {
                retries--;
                run();
            }
        }

    }

    private void parseHTMLResponse() {

        // Youtube has given us the data we require embedded in the HTML and must be parsed from the HTML
        // Video Times: youtubeSearch.select("ol.item-section").get(0).select("span.video-time").get(selection)
        // Video Link: youtubeSearch.select("ol.item-section").get(0).select("a[href][aria-hidden]").get(selection).attr("href")

        Debug.trace("HTML Response received from youtube.");

        JSONObject searchDataTemp;
        try {
            for (int i = 0; i < requestedPage.select("ol.item-section").get(0).select("span.video-time").size(); i++) {

                int length = timeConversion(
                        requestedPage
                                .select("ol.item-section")
                                .get(0)
                                .select("span.video-time")
                                .get(i)
                                .text()
                );

                if (length < targetTime * 1.05 && length > targetTime / 1.05) {

                    results.getJSONArray("primary").put(
                            requestedPage
                                    .select("ol.item-section")
                                    .get(0)
                                    .select("a[href][aria-hidden]")
                                    .get(i).attr("href")
                                    .substring(9)
                    );

                } else if (length < targetTime * 1.15 && length > targetTime / 1.15) {

                    searchDataTemp = new JSONObject();
                    searchDataTemp.put(
                            "watch_id",
                            requestedPage
                                    .select("ol.item-section")
                                    .get(0)
                                    .select("a[href][aria-hidden]")
                                    .get(i).attr("href")
                                    .substring(9) // Removes /watch?v= from the source
                    );
                    searchDataTemp.put(
                            "difference",
                            Math.abs(
                                    timeConversion(
                                            requestedPage
                                                    .select("ol.item-section")
                                                    .get(0)
                                                    .select("span.video-time")
                                                    .get(i)
                                                    .text()
                                    ) - targetTime
                            )
                    );

                }
            }

            results.put("secondary", new QuickSort(results.getJSONArray("secondary"), 0, results.getJSONArray("secondary").length()).getSorted());

        } catch (JSONException e) {
            Debug.error("Failed to create response from youtube.", e);
        }


    }

}
