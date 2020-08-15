package sample.utils.net.db;

import javafx.scene.layout.BorderPane;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import sample.Main;
import sample.utils.app.debug;
import sample.utils.fx.result;

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

        public static final String subdirectory = "search/all/";

        public search(String query) {
            this.searchQuery = baseDomain + subdirectory + query;
        }

        public void query(Boolean useDefaultIcons) throws IOException {

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
                        resultData.put("link", result.select("div.title").select("a").attr("href")); // TODO: Remove in future
                        resultData.put("allmusicId", result.select("div.title").select("a").attr("href").split("/")[4]);

                        if (result.select("div.cover").size() > 0) {
                            String potentialAlbumArt = result.select("img.lazy").attr("data-original");
                            resultData.put(
                                    "art",
                                    potentialAlbumArt.isEmpty() || useDefaultIcons ?
                                            new File(Main.class.getResource("app/img/album_default.png").getPath()).toURI().toString() :
                                            potentialAlbumArt
                            );
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

        public void getSongExternalInformation() throws IOException {

            try {
                for (int i = 0; i < searchResultsData.length(); i++)
                    if (!searchResultsData.getJSONObject(i).getBoolean("album")) {
                        song songProcessor = new song(searchResultsData.getJSONObject(i).getString("allmusicId"));
                        songProcessor.load();

                        searchResultsData.getJSONObject(i).put("art", songProcessor.getAlbumArt());
                        searchResultsData.getJSONObject(i).put("year", songProcessor.getYear());
                        searchResultsData.getJSONObject(i).put("genre", songProcessor.getGenre());
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

    public static class song {

        public static final String subdirectory = "song/";

        private final String pageUrl;
        private Document doc = null;

        public song(String id) {
            this.pageUrl = baseDomain + subdirectory + id;
        }

        public void load() throws IOException {

            doc = Jsoup.connect(pageUrl).get();

        }

        public String getYear() {
            if (doc == null) throw new IllegalArgumentException();

            if (doc.selectFirst("p.song-release-year-text").attr("data-releaseyear").length() == 4)
                return doc.selectFirst("p.song-release-year-text").attr("data-releaseyear");

            else return "";
        }

        public String getAlbumArt() {
            if (doc == null) throw new IllegalArgumentException();

            String albumArtSource = doc.selectFirst("td.cover").selectFirst("img").attr("src");

            try {
                if (albumArtSource.isEmpty())
                    return Main.class.getResource("app/img/song_default.png").toURI().toString();

                else return albumArtSource;

            } catch (URISyntaxException e) {
                debug.error(Thread.currentThread(), "Failed to load song default.", e);
            }
            return "";
        }

        public String getGenre() {
            if (doc == null) throw new IllegalArgumentException();

            if (doc.hasClass("div.song_genres"))

                return doc
                        .selectFirst("div.song_genres")
                        .selectFirst("div.middle")
                        .selectFirst("a")
                        .text()
                        .split("\\(")[0]
                        .substring(
                                0,
                                doc
                                        .selectFirst("div.song_genres")
                                        .selectFirst("div.middle")
                                        .selectFirst("a")
                                        .text()
                                        .split("\\(")[0]
                                        .length() - 1
                        );

            else
                return "";
        }
    }

    @SuppressWarnings("unused")
    public static class album {



    }

    public static class artist {



    }

}
