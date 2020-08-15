package sample.utils.net;

import javafx.scene.layout.BorderPane;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import sample.Main;
import sample.utils.debug;
import sample.utils.result;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

// Should contain
public class allmusic {

    public static final String baseDomain = "https://www.allmusic.com/";

    public static class search {

        private final String searchQuery;
        private final JSONArray searchResultsData = new JSONArray();

        public static final String searchExtension = "search/all/";

        public search(String query) {
            this.searchQuery = baseDomain +  searchExtension + query;
        }

        public void query() throws IOException {

            Document doc = Jsoup.connect(searchQuery).get();

            for (Element result: doc.select("ul.search-results").select("li"))
            {

                // Check that it's either a album or an song, not an artist, the data is a bit odd so the hashcode fixes it
                if (result.select("h4").text().hashCode() != 1969736551 && result.select("h4").text().hashCode() != 73174740) {
                    JSONObject resultData = new JSONObject();

                    try {
                        // Building data from the webpage
                        resultData.put("title", result.select("div.title").text().replaceAll("\"", ""));
                        resultData.put("artist", result.select("h4").text().hashCode() == 2582837 ? result.select("div.performers").select("a").text() : result.select("div.artist").text());
                        resultData.put("year", result.select("div.year").text());
                        resultData.put("genre", result.select("div.genres").text());
                        resultData.put("album", result.select("div.cover").size() > 0);
                        resultData.put("link", result.select("div.title").select("a").attr("href"));

                        if (result.select("div.cover").size() > 0) {
                            String potentialAlbumArt = result.select("img.lazy").attr("data-original");
                            resultData.put("art", potentialAlbumArt.isEmpty() ? new File(Main.class.getResource("app/img/album_default.png").getPath()).toURI().toString() : potentialAlbumArt);
                        } else {
                            try {
                                resultData.put("art", Main.class.getResource("app/img/song_default.png").toURI().toString());
                            } catch (URISyntaxException e) {
                                debug.error(Thread.currentThread(), "Failed to get default icon for song.", e);
                            }
                        }

                        searchResultsData.put(resultData);

                    } catch (JSONException e) {
                        debug.error(Thread.currentThread(), "Failed to process search request JSON for" + searchQuery, e);
                    }
                }
            }
        }

        public void getSongAlbumArt() throws IOException {

            try {
                for (int i = 0; i < searchResultsData.length(); i++) {
                    if (!searchResultsData.getJSONObject(i).getBoolean("album")) {
                        Document songDataPage = Jsoup.connect(searchResultsData.getJSONObject(i).getString("link")).get();

                        // Album Art
                        try {
                            String potentialAlbumArt = songDataPage.selectFirst("td.cover").selectFirst("img").attr("src") != null && songDataPage.selectFirst("td.cover").selectFirst("img").attr("src").startsWith("https") && !songDataPage.selectFirst("td.cover").selectFirst("img").attr("src").equals("https://cdn-gce.allmusic.com/images/lazy.gif") ? songDataPage.selectFirst("td.cover").selectFirst("img").attr("src") : Main.class.getResource("app/img/song_default.png").toURI().toString();
                            searchResultsData.getJSONObject(i).put("art", potentialAlbumArt.isEmpty() ? new File(Main.class.getResource("app/img/song_default.png").getPath()).toURI().toString() : potentialAlbumArt);
                        } catch (NullPointerException ignored) {
                            searchResultsData.getJSONObject(i).put("art", new File(Main.class.getResource("app/img/song_default.png").getPath()).toURI().toString());
                        } catch (URISyntaxException e) {
                            debug.error(null, "URI Formation exception loading song default image.", e);
                        }

                        // Year
                        try {
                            if (songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear").length() == 4)
                                searchResultsData.getJSONObject(i).put("year", songDataPage.selectFirst("p.song-release-year-text").attr("data-releaseyear"));
                        } catch (NullPointerException ignored) {
                        }

                        try {
                            // Genre
                            searchResultsData.getJSONObject(i).put(
                                    "genre",
                                    songDataPage
                                            .selectFirst("div.song_genres")
                                            .selectFirst("div.middle")
                                            .selectFirst("a")
                                            .text()
                                            .split("\\(")[0]
                                            .substring(
                                                    0,
                                                    songDataPage
                                                            .selectFirst("div.song_genres")
                                                            .selectFirst("div.middle")
                                                            .selectFirst("a")
                                                            .text()
                                                            .split("\\(")[0]
                                                            .length() - 1
                                            )
                            );
                        } catch (NullPointerException ignored) {
                        }


                    }
                }
            } catch (JSONException e) {
                debug.error(Thread.currentThread(), "Failed to parse data to get song album art.", e);
            }

        }

        public ArrayList<BorderPane> buildView() {
            ArrayList<BorderPane> viewResults = new ArrayList<>();

            try {
                for (int i = 0; i < searchResultsData.length(); i++) {

                    StringBuilder metaInfoRaw = new StringBuilder(searchResultsData.getJSONObject(i).getBoolean("album") ? "Album" : "Song");

                    if (!searchResultsData.getJSONObject(i).getString("year").isEmpty())
                        metaInfoRaw.append(" | ").append(searchResultsData.getJSONObject(i).getString("year"));

                    if (!searchResultsData.getJSONObject(i).getString("genre").isEmpty())
                        metaInfoRaw.append(" | ").append(searchResultsData.getJSONObject(i).getString("genre"));

                    result resultBuilder = new result(
                            null,
                            searchResultsData.getJSONObject(i).getString("art"),
                            true,
                            searchResultsData.getJSONObject(i).getString("title"),
                            searchResultsData.getJSONObject(i).getString("artist")
                    );

                    resultBuilder.applyWarning(metaInfoRaw.toString());
                    viewResults.add(resultBuilder.getView());
                }
            } catch (JSONException e) {
                debug.error(Thread.currentThread(), "Failed to parse JSON to build search results view.", e);
            }

            return viewResults;
        }

        public JSONArray getSearchResultsData() {
            return searchResultsData;
        }
    }

    @SuppressWarnings("unused")
    public static class song {



    }

    @SuppressWarnings("unused")
    public static class album {



    }

    public static class artist {



    }

}
