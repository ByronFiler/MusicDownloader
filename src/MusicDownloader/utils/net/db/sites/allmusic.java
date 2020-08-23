package MusicDownloader.utils.net.db.sites;

import MusicDownloader.Main;
import MusicDownloader.utils.app.debug;
import MusicDownloader.utils.fx.result;
import javafx.scene.layout.BorderPane;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;

// Should contain
public class allmusic {

    public static final String baseDomain = "https://www.allmusic.com/";

    public static class search implements MusicDownloader.utils.net.db.search {

        private final String searchQuery;
        private final JSONArray searchResultsData = new JSONArray();

        private int albumCount = 0;
        private int songCount = 0;

        public static final String subdirectory = "search/all/";

        public search(String query) {
            this.searchQuery = baseDomain + subdirectory + query;
        }

        public void getSongExternalInformation() throws IOException {

            try {
                for (int i = 0; i < searchResultsData.length(); i++) {

                    if (!searchResultsData.getJSONObject(i).getJSONObject("data").getBoolean("album")) {
                        song songProcessor = new song(searchResultsData.getJSONObject(i).getJSONObject("data").getString("allmusicSongId"));
                        songProcessor.load();

                        searchResultsData.getJSONObject(i).getJSONObject("data").put("art", songProcessor.getAlbumArt());
                        searchResultsData.getJSONObject(i).getJSONObject("data").put("year", songProcessor.getYear());
                        searchResultsData.getJSONObject(i).getJSONObject("data").put("genre", songProcessor.getGenre());
                        searchResultsData.getJSONObject(i).getJSONObject("data").put("allmusicAlbumId", songProcessor.getAlbumId());

                        searchResultsData.getJSONObject(i).getJSONObject("view").put("art", songProcessor.getAlbumArt());

                        StringBuilder metaInfoRaw = new StringBuilder("Song");
                        if (!searchResultsData.getJSONObject(i).getJSONObject("data").getString("year").isEmpty())
                            metaInfoRaw.append(" | ").append(searchResultsData.getJSONObject(i).getJSONObject("data").getString("year"));
                        if (!searchResultsData.getJSONObject(i).getJSONObject("data").getString("genre").isEmpty())
                            metaInfoRaw.append(" | ").append(searchResultsData.getJSONObject(i).getJSONObject("data").getString("genre"));

                        searchResultsData.getJSONObject(i).getJSONObject("view").put("meta", metaInfoRaw.toString());
                    }
                }
            } catch (JSONException e) {
                debug.error("Failed to parse data to get song album art.", e);
            }

        }

        @Override
        public void query(Boolean useDefaultIcons) throws IOException {

            Document doc = Jsoup.connect(searchQuery).get();

            for (Element result: doc.select("ul.search-results").select("li")) {
                // Check that it's either a album or an song, not an artist, the data is a bit odd so the hashcode fixes it
                if (result.select("h4").text().hashCode() != 1969736551 && result.select("h4").text().hashCode() != 73174740) {

                    JSONObject resultData = new JSONObject();
                    JSONObject viewData = new JSONObject();
                    JSONObject applicationData = new JSONObject();

                    try {

                        viewData.put("title", result.select("div.title").text().replaceAll("\"", ""));
                        viewData.put("artist", result.select("h4").text().hashCode() == 2582837 ? result.select("div.performers").select("a").text() : result.select("div.artist").text());

                        applicationData.put("artist", result.select("h4").text().hashCode() == 2582837 ? result.select("div.performers").select("a").text() : result.select("div.artist").text());
                        applicationData.put("year", result.select("div.year").text());
                        applicationData.put("genre", result.select("div.genres").text());
                        applicationData.put("album", result.select("div.cover").size() > 0);

                        // Build a clean view of an overview of our known information
                        StringBuilder metaInfoRaw = new StringBuilder(applicationData.getBoolean("album") ? "Album" : "Song");
                        if (!applicationData.getString("year").isEmpty()) metaInfoRaw.append(" | ").append(applicationData.getString("year"));
                        if (!applicationData.getString("genre").isEmpty()) metaInfoRaw.append(" | ").append(applicationData.getString("genre"));

                        viewData.put("meta", metaInfoRaw.toString());

                        // TODO: This check (in theory) shouldn't be needed?
                        if (!result.select("div.title").select("a").attr("href").isEmpty())
                            applicationData.put(
                                    result.select("div.cover").size() > 0 ? "allmusicAlbumId" : "allmusicSongId",
                                    result.select("div.title").select("a").attr("href").split("/")[4]
                            );

                        if (result.select("div.cover").size() > 0) {
                            albumCount++;

                            // Album (has art)
                            String potentialAlbumArt = result.select("img.lazy").attr("data-original");

                            viewData.put(
                                    "art",
                                    potentialAlbumArt.isEmpty() || useDefaultIcons ?
                                            new File(Main.class.getResource("app/img/album_default.png").getPath()).toURI().toString() :
                                            potentialAlbumArt
                            );
                            applicationData.put(
                                    "art",
                                    potentialAlbumArt.isEmpty() ?
                                            new File(Main.class.getResource("app/img/album_default.png").getPath()).toURI().toString() :
                                            potentialAlbumArt
                            );

                        } else {
                            songCount++;

                            // Song (does not have art)
                            try {

                                viewData.put("art", Main.class.getResource("app/img/song_default.png").toURI().toString());
                                applicationData.put("art", Main.class.getResource("app/img/song_default.png").toURI().toString());

                            } catch (URISyntaxException e) {
                                debug.error("Failed to get default icon for song.", e);
                            }
                        }

                        resultData.put("view", viewData);
                        resultData.put("data", applicationData);

                        searchResultsData.put(resultData);

                    } catch (JSONException e) {
                        debug.error("Failed to process search request JSON for" + searchQuery, e);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        debug.warn("Unknown URL: " + result.select("div.title").select("a").attr("href"));
                    }
                }
            }
        }

        @Override
        public ArrayList<BorderPane> buildView() {
            ArrayList<BorderPane> viewResults = new ArrayList<>();

            try {
                for (int i = 0; i < searchResultsData.length(); i++) {
                    result resultBuilder = new result(
                            null,
                            searchResultsData.getJSONObject(i).getJSONObject("view").getString("art"),
                            true,
                            searchResultsData.getJSONObject(i).getJSONObject("view").getString("title"),
                            searchResultsData.getJSONObject(i).getJSONObject("view").getString("artist")
                    );

                    resultBuilder.setSubtext(searchResultsData.getJSONObject(i).getJSONObject("view").getString("meta"));
                    viewResults.add(resultBuilder.getView());
                }
            } catch (JSONException e) {
                debug.error("Failed to parse JSON to build search results view.", e);
            }

            return viewResults;
        }

        @Override
        public JSONArray getSearchResultsData() {
            return searchResultsData;
        }

        @Override
        public synchronized int getAlbumCount() {
            return albumCount;
        }

        @Override
        public synchronized int getSongCount() {
            return songCount;
        }
    }

    public static class song implements MusicDownloader.utils.net.db.song {

        public static final String subdirectory = "song/";

        private final String pageUrl;
        private Document doc = null;

        public song(String id) {
            this.pageUrl = baseDomain + subdirectory + id;
        }

        public void load() throws IOException {
            this.doc = Jsoup.connect(pageUrl).get();
        }

        public String getYear() {
            if (doc == null) throw new IllegalCallerException();

            if (doc.selectFirst("p.song-release-year-text").attr("data-releaseyear").length() == 4)
                return doc.selectFirst("p.song-release-year-text").attr("data-releaseyear");

            else return "";
        }

        public String getAlbumArt() {
            if (doc == null) throw new IllegalCallerException();

            String albumArtSource = doc.selectFirst("td.cover").selectFirst("img").attr("src");

            try {
                if (albumArtSource.isEmpty())
                    return Main.class.getResource("app/img/song_default.png").toURI().toString();

                else return albumArtSource;

            } catch (URISyntaxException e) {
                debug.error("Failed to load song default.", e);
            }
            return "";
        }

        public String getGenre() {
            if (doc == null) throw new IllegalCallerException();

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

        public String getAlbumId() {
            if (doc == null) throw new IllegalCallerException();

            // Get the link to the top result album, this saves it being done later if the song is added to queue
            return doc.select("tr").get(1).selectFirst("a").attr("href").split("/")[2];
        }
    }

    public static class album implements MusicDownloader.utils.net.db.album {

        public static final String subdirectory = "album/";

        private final String pageUrl;
        private Document doc = null;
        private final ArrayList<song> songs = new ArrayList<>();

        public album(String identifier) {
            this.pageUrl = baseDomain + subdirectory + identifier;
        }

        @Override
        public void load() throws IOException {
            this.doc = Jsoup.connect(pageUrl).get();

            for (Element albumResult: doc.select("tr.track")) {
                song foundSong = new song(albumResult);

                if (foundSong.isValid()) songs.add(foundSong);
            }
        }

        @Override
        public String getAlbum() {
            if (doc == null) throw new IllegalCallerException();

            return Objects.requireNonNull(doc).selectFirst("h1.album-title").text();
        }

        @Override
        public ArrayList<song> getSongs() {
            if (doc == null) throw new IllegalCallerException();
            return songs;
        }

        @Override
        public int getPlaytime() {
            return songs.stream().mapToInt(song::getPlaytime).sum();
        }

        protected static int timeConversion(String stringTime) {

            String[] songDataBreak = stringTime.split(":");

            int songLenSec = 0;

            for (int i = songDataBreak.length-1; i >= 0; i--) {
                // Time * 60^^Index ie
                // 01:27 -> 27:01 -> ((27)*60^^0) + ((1)*60^^1) -> 87
                songLenSec += (Double.parseDouble(songDataBreak[Math.abs(i-songDataBreak.length+1)]) * Math.pow(60, i));
            }

            return songLenSec;
        }

        public static class song {

            private final int playtime;
            private final String title;
            private final String sample;

            public song(Element track) {
                if (!track.select("div.title").text().isEmpty())
                    this.title = track.select("div.title").text();
                else this.title = null;

                if (!track.select("td.time").text().isEmpty())
                    this.playtime = timeConversion(track.select("td.time").text());
                else this.playtime = -1;

                if (track.select("a.audio-player").size() > 0) {
                    String sampleSource = track.selectFirst("a.audio-player").attr("data-sample-url");
                    this.sample = sampleSource.substring(46, sampleSource.length() - 5);
                }
                else this.sample = null;
            }

            public int getPlaytime() {
                return playtime;
            }

            public String getTitle() {
                return title;
            }

            public String getSample() {
                return sample;
            }

            public boolean isValid() {
                return this.playtime != -1 && this.title != null && !this.title.isEmpty();
            }

        }

    }

    @SuppressWarnings("unused")
    public static class artist {
    }

}
