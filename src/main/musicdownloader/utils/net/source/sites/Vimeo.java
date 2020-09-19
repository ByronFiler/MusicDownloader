package musicdownloader.utils.net.source.sites;

// These should all just extend a class

import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.io.QuickSort;
import musicdownloader.utils.net.source.Site;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class Vimeo extends Site {

    public Vimeo(String query, int targetTime) throws JSONException {
        super(query, targetTime);
    }

    public void load() throws IOException {

        requestedPage = Jsoup.connect(Resources.vimeoSearch + query).get();
        AtomicReference<String> processedLine = new AtomicReference<>("");

        requestedPage.select("script").forEach(scriptContent -> {
            if (scriptContent.toString().contains("    vimeo.config")) {
                Arrays.asList(requestedPage.select("script")
                        .get(requestedPage.select("script").indexOf(scriptContent))
                        .toString()
                        .split("\n")
                ).forEach(line -> {
                    if (line.startsWith("    vimeo.config")) {
                        processedLine.set(line.trim().substring(45));
                    }
                });
            }
        });

        // Looking for a <script> section containing relevant JSON and extracting the line
        Arrays.asList(requestedPage.select("script").get(15).toString().split("\n")).forEach(e -> { if (e.startsWith("    vimeo.config")) processedLine.set(e.trim().substring(45)); });

        try {
            JSONArray searchJson = new JSONObject(processedLine.get())
                    .getJSONObject("api")
                    .getJSONObject("initial_json")
                    .getJSONArray("data");

            for (int i = 0; i < searchJson.length(); i++) {

                int duration = searchJson.getJSONObject(i).getJSONObject("clip").getInt("duration");

                if (duration <= (targetTime * 1.05) && duration >= (targetTime / 1.05)) {

                    results.getJSONArray("primary").put(searchJson.getJSONObject(i).getJSONObject("clip").getString("link").substring(18));

                } else if (duration <= (targetTime * 1.15) && duration >= (targetTime / 1.15)) {

                    JSONObject secondaryItem = new JSONObject();
                    secondaryItem.put("watch_id", searchJson.getJSONObject(i).getJSONObject("clip").getString("link").substring(18));
                    secondaryItem.put("difference", Math.abs(searchJson.getJSONObject(i).getJSONObject("clip").getInt("duration") - targetTime));

                    results.getJSONArray("secondary").put(secondaryItem);
                }

            }

            results.put("secondary", new QuickSort(results.getJSONArray("secondary"), 0, results.getJSONArray("secondary").length() - 1).getSorted());

        } catch (JSONException e) {
            e.printStackTrace();
            Debug.warn("Failed to parse Vimeo query: " + query);
        }

    }



}
