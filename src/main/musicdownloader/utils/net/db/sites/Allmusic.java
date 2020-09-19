package musicdownloader.utils.net.db.sites;

import javafx.scene.layout.BorderPane;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.net.db.Album;
import musicdownloader.utils.net.db.Song;
import musicdownloader.utils.ui.Result;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

// TODO
// Getting the data should provide a success or failure check and not just print a debug message and return nothing

// Should contain
public class Allmusic {

    public static final String baseDomain = "https://www.allmusic.com/";

    public static class Search implements musicdownloader.utils.net.db.Search {

        private final String searchQuery;

        private final JSONObject results = new JSONObject();
        private final JSONArray songs = new JSONArray();
        private final JSONObject metadata = new JSONObject();

        private int albumCount = 0;
        private int songCount = 0;

        public static final String subdirectory = "search/all/";

        public Search(String query) {
            this.searchQuery = baseDomain + subdirectory + query;
            try {
                this.metadata.put("query", query);
            } catch (JSONException e) {
                Debug.error("Failed to get query for building response.", e);
            }
        }

        // TODO: Thread this
        @SuppressWarnings("StatementWithEmptyBody")
        public void getSongExternalInformation() {
            AtomicInteger completedThreads = new AtomicInteger();
            AtomicInteger threadsToComplete = new AtomicInteger();
            try {
                for (int i = 0; i < songs.length(); i++) {
                    if (!songs.getJSONObject(i).getJSONObject("data").getBoolean("album") && songs.getJSONObject(i).getJSONObject("data").has("allmusicSongId")) {

                        int finalI = i;
                        Thread externalInformationLoader = new Thread(() -> {
                            threadsToComplete.getAndIncrement();

                            try {
                                song songProcessor = new song(songs.getJSONObject(finalI).getJSONObject("data").getString("allmusicSongId"));
                                songProcessor.load();

                                songs.getJSONObject(finalI).getJSONObject("data").put("art", songProcessor.getAlbumArt());
                                songs.getJSONObject(finalI).getJSONObject("data").put("year", songProcessor.getYear());
                                songs.getJSONObject(finalI).getJSONObject("data").put("genre", songProcessor.getGenre());
                                songs.getJSONObject(finalI).getJSONObject("data").put("allmusicAlbumId", songProcessor.getAlbumId());

                                songs.getJSONObject(finalI).getJSONObject("view").put("art", songProcessor.getAlbumArt());

                                StringBuilder metaInfoRaw = new StringBuilder("Song");
                                if (!songs.getJSONObject(finalI).getJSONObject("data").getString("year").isEmpty())
                                    metaInfoRaw.append(" | ").append(songs.getJSONObject(finalI).getJSONObject("data").getString("year"));
                                if (!songs.getJSONObject(finalI).getJSONObject("data").getString("genre").isEmpty())
                                    metaInfoRaw.append(" | ").append(songs.getJSONObject(finalI).getJSONObject("data").getString("genre"));

                                songs.getJSONObject(finalI).getJSONObject("view").put("meta", metaInfoRaw.toString());
                            } catch (JSONException e) {
                                Debug.error("JSONException gathering additional information.", e);
                            } catch (IOException e) {
                                Debug.warn("Connection failed");
                            }

                            completedThreads.getAndIncrement();
                        }, "external-information-getter");
                        externalInformationLoader.setDaemon(true);
                        externalInformationLoader.start();
                    }

                }
            } catch (JSONException e) {
                Debug.error("failed to parse json to build additional information.", e);
            }

            // TODO: Optimise this
            while (completedThreads.get() < threadsToComplete.get());
        }

        @Override
        public void query(Boolean useDefaultIcons) throws IOException {
            try {
                Document doc = Jsoup.connect(searchQuery).get();

                for (Element result : doc.select("ul.search-results").select("li")) {
                    // Check that it's either a album or an song, not an artist, the data is a bit odd so the hashcode fixes it
                    if (!Resources.invalidSearchTypes.contains(result.select("h4").text().hashCode())) {

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
                            if (!applicationData.getString("year").isEmpty())
                                metaInfoRaw.append(" | ").append(applicationData.getString("year"));
                            if (!applicationData.getString("genre").isEmpty())
                                metaInfoRaw.append(" | ").append(applicationData.getString("genre"));

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
                                                new File(getClass().getClassLoader().getResource("resources/img/album_default.png").getPath()).toURI().toString() :
                                                potentialAlbumArt
                                );
                                applicationData.put(
                                        "art",
                                        potentialAlbumArt.isEmpty() ?
                                                new File(getClass().getClassLoader().getResource("resources/img/album_default.png").getPath()).toURI().toString() :
                                                potentialAlbumArt
                                );

                            } else {
                                songCount++;

                                // Song (does not have art)
                                try {

                                    viewData.put("art", getClass().getClassLoader().getResource("resources/img/song_default.png").toURI().toString());
                                    applicationData.put("art", getClass().getClassLoader().getResource("resources/img/song_default.png").toURI().toString());

                                } catch (URISyntaxException e) {
                                    Debug.error("Failed to get default icon for song.", e);
                                }
                            }

                            resultData.put("view", viewData);
                            resultData.put("data", applicationData);

                            songs.put(resultData);

                        } catch (JSONException e) {
                            Debug.error("Failed to process search request JSON for" + searchQuery, e);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Debug.warn("Unknown URL: " + result.select("div.title").select("a").attr("href"));
                        }
                    }
                }
            } catch (HttpStatusException e) {
                Debug.warn("Invalid query given.");
            }
        }

        @Override
        public ArrayList<BorderPane> buildView() {
            ArrayList<BorderPane> viewResults = new ArrayList<>();

            try {
                for (int i = 0; i < songs.length(); i++) {
                    Result resultBuilder = new Result(
                            null,
                            songs.getJSONObject(i).getJSONObject("view").getString("art"),
                            false,
                            songs.getJSONObject(i).getJSONObject("view").getString("title"),
                            songs.getJSONObject(i).getJSONObject("view").getString("artist")
                    );

                    resultBuilder.setSubtext(songs.getJSONObject(i).getJSONObject("view").getString("meta"));
                    viewResults.add(resultBuilder.getView());
                }
            } catch (JSONException e) {
                Debug.error("Failed to parse JSON to build search results view.", e);
            }

            return viewResults;
        }

        @Override
        public JSONObject getResults() {
            try {
                results.put("metadata", metadata);
                results.put("songs", songs);
            } catch (JSONException e) {
                Debug.error("Failed in building results from search data.", e);
            }

            return results;
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

    public static class song implements Song {

        public static final String subdirectory = "song/";

        private final String pageUrl;
        private Document doc = null;

        public song(String id) {
            this.pageUrl = baseDomain + subdirectory + id;
        }

        public void load() throws IOException {
            try {
                this.doc = Jsoup.connect(pageUrl).get();
            } catch (HttpStatusException e) {

                Debug.warn(e.getStatusCode() == 404 ? "Dead link detected in search: " + pageUrl : "Invalid song link supplied, error: " + e.getStatusCode());

            }
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
                    return getClass().getClassLoader().getResource("resources/img/song_default.png").toURI().toString();

                else return albumArtSource;

            } catch (URISyntaxException e) {
                Debug.error("Failed to load song default.", e);
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

    public static class album implements Album {

        public static final String subdirectory = "album/";

        private final String pageUrl;
        private Document doc = null;
        private final ArrayList<song> songs = new ArrayList<>();
        private boolean isLoaded = false;

        public album(String identifier) {
            this.pageUrl = baseDomain + subdirectory + identifier;
        }

        @Override
        public void load() throws IOException {
            isLoaded = true;
            try {
                this.doc = Jsoup.connect(pageUrl).get();

                for (Element albumResult: doc.select("tr.track")) {
                    song foundSong = new song(albumResult);

                    if (foundSong.isValid()) songs.add(foundSong);
                }
            } catch (HttpStatusException e) {
                Debug.warn("Invalid album link supplied.");
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

        @Override
        public boolean getIsLoaded() {
            return isLoaded;
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

}
