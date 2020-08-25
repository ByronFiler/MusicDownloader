package musicdownloader.utils.net.source.sites;

import musicdownloader.utils.net.source.Source;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import musicdownloader.utils.app.Debug;

import java.io.IOException;

public class Youtube implements Source {

    private final String pageUrl;
    private final int targetTime;

    private int retries = 5;

    private Document requestedPage = null;
    private final JSONArray results = new JSONArray();

    public Youtube(String query, int targetTime) {

        this.pageUrl = "https://www.youtube.com/results?search_query=" + query;
        this.targetTime = targetTime;

    }

    @Override
    public void load() throws IOException {
        requestedPage = Jsoup.connect(pageUrl).get();

        if (requestedPage.select("script").size() == 17) parseHTMLResponse();

        else parseJSONResponse();

    }

    @Override
    public JSONArray getResults() {
        return results;
    }

    private int timeConversion(String stringTime) {
        String[] songDataBreak = stringTime.split(":");

        int songLenSec = 0;

        for (int i = songDataBreak.length-1; i >= 0; i--) {
            // Time * 60^^Index ie
            // 01:27 -> 27:01 -> ((27)*60^^0) + ((1)*60^^1) -> 87
            songLenSec += (Double.parseDouble(songDataBreak[Math.abs(i-songDataBreak.length+1)]) * Math.pow(60, i));
        }

        return songLenSec;
    }

    private void parseJSONResponse() {

        // YouTube has given us the data stored in json stored script tags which be parsed
        JSONObject searchDataTemp;
        try {
            // Web Data -> [JavaScript] -> String -> Json -> Data
            Element jsData = requestedPage.select("script").get(24);

            // Web Data -> JavaScript -> [String] -> Json -> Data
            String jsonConversion = jsData.toString();
            jsonConversion = jsonConversion.substring(39, jsonConversion.length() - 119);

            // Web Data -> JavaScript -> String -> [Json] -> Data
            JSONObject json = new JSONObject(jsonConversion);

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
                else
                    try {
                        retries--;
                        load();
                    } catch (IOException e) {
                        Debug.warn("Failed to connect to youtube get results.");
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
                    if (length < targetTime * 1.15 && length > targetTime / 1.15) {

                        searchDataTemp = new JSONObject();

                        // Extract the playtime and the link to the video
                        searchDataTemp.put(
                                "watch_id",
                                contents
                                        .getJSONObject(i)
                                        .getJSONObject("videoRenderer")
                                        .getString("videoId")
                        );

                        searchDataTemp.put("difference", Math.abs(length - targetTime));

                        results.put(searchDataTemp);
                    }

                } catch (JSONException ignored) {} // Youtube adds random elements that are tricky to handle and are best ignored
            }

        } catch (JSONException e) {
            Debug.error("Failed to parse youtube results.", e);
        }

    }

    private void parseHTMLResponse() {

        // Youtube has given us the data we require embedded in the HTML and must be parsed from the HTML
        // Video Times: youtubeSearch.select("ol.item-section").get(0).select("span.video-time").get(selection)
        // Video Link: youtubeSearch.select("ol.item-section").get(0).select("a[href][aria-hidden]").get(selection).attr("href")

        JSONObject searchDataTemp;
        for (int i = 0; i < requestedPage.select("ol.item-section").get(0).select("span.video-time").size(); i++) {

            try {
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
                results.put(searchDataTemp);
            } catch (JSONException e) {
                Debug.warn("Failed to generate search data from query, from html response.");
            }
        }

    }

}
