package musicdownloader.utils.net.source.sites;

import musicdownloader.utils.app.Debug;
import musicdownloader.utils.io.QuickSort;
import musicdownloader.utils.net.source.Source;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;

public class Youtube implements Source {

    private final String pageUrl;
    private final int targetTime;

    private int retries = 5;

    private Document requestedPage = null;
    private final ArrayList<String> results = new ArrayList<>();

    private final JSONArray secondaryResults = new JSONArray(); // Songs within +=15% of target time, sorted by QuickSort based on time

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
    public ArrayList<String> getResults() {
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

        Debug.log("JSON Response received from youtube.");

        // YouTube has given us the data stored in json stored script tags which be parsed
        JSONObject searchDataTemp;
        try {
            // Web Data -> [JavaScript] -> String -> Json -> Data
            Element jsData = requestedPage.select("script").get(24);

            // Web Data -> JavaScript -> [String] -> Json -> Data
            String jsonConversion = jsData.toString();
            jsonConversion = jsonConversion.substring(39, jsonConversion.length() - 119);

            // Web Data -> JavaScript -> String -> [Json] -> Data
            JSONObject json = new JSONObject();
            try {
                json = new JSONObject(jsonConversion);
            } catch (JSONException e) {
                e.printStackTrace();
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
                    if (length < targetTime * 1.05 && length > targetTime / 1.05) {

                        results.add(
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

                        secondaryResults.put(searchDataTemp);

                    }

                } catch (JSONException ignored) {} // Youtube adds random elements that are tricky to handle and are best ignored
            }

            results.addAll(new QuickSort(secondaryResults, 0, secondaryResults.length() - 1).getSorted());

        } catch (JSONException e) {
            Debug.error("Failed to parse youtube results.", e);
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

                    results.add(
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

            results.addAll(new QuickSort(secondaryResults, 0, secondaryResults.length()).getSorted());

        } catch (JSONException e) {
            Debug.error("Failed to create response from youtube.", e);
        }


    }

}
