package sample;

import javafx.scene.image.ImageView;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Utils {

    public View view;

    // TODO: Implemented in latest
    public static class resultsSet {
        private ImageView albumArt;
        private String title;
        private String artist;
        private String year;
        private String genre;
        private String type;

        public resultsSet(ImageView albumArt, String title, String artist, String year, String genre, String type) {
            super();
            this.albumArt = albumArt;
            this.title = title;
            this.artist = artist;
            this.year = year;
            this.genre = genre;
            this.type = type;
        }

        public ImageView getAlbumArt() {
            albumArt.setFitHeight(75);
            albumArt.setFitWidth(75);
            return albumArt;
        }

        public void setAlbumArt(ImageView albumArtLink) {
            this.albumArt = albumArtLink;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() { return artist; }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    // TODO: Removed/Replaced in latest
    public static class autocompleteResultsSet {
        private ImageView icon;
        private String name;

        public autocompleteResultsSet(ImageView icon, String name) {
            super();
            this.icon = icon;
            this.name = name;
        }

        public ImageView getIcon() {
            icon.setFitHeight(25);
            icon.setFitWidth(25);
            return icon;
        }

        public void setIcon(ImageView icon) {
            this.icon = icon;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static synchronized int timeConversion(String stringTime){
        String[] songDataBreak = stringTime.split(":");
        int songLenSec = 0;
        for (int i = songDataBreak.length-1; i >= 0; i--)
        {
            // Time * 60^^Index ie
            // 01:27 -> 27:01 -> ((27)*60^^0) + ((1)*60^^1) -> 87
            songLenSec += (Double.parseDouble(songDataBreak[Math.abs(i-songDataBreak.length+1)]) * Math.pow(60, i));
        }
        return songLenSec;
    }

    // TODO: Implemented in latest
    public static synchronized ArrayList<ArrayList<String>> allmusicQuery(Document doc) {

        ArrayList<ArrayList<String>> songsData = new ArrayList<>();
        ArrayList<String> resultData;

        Elements mst = doc.select("ul.search-results");
        Elements results = mst.select("li");

        for (Element result: results)
        {

            // Check that it's either a album or an song, not an artist, the data is a bit odd so the hashcode fixes it
            if (result.select("h4").text().hashCode() != 1969736551 && result.select("h4").text().hashCode() != 73174740) {
                resultData = new ArrayList<>();

                // Handling The Title, If it's a song it has "" surround it, which it to be removed
                String dataRequest = result.select("div.title").text();
                if (result.select("h4").text().hashCode() == 2582837)
                    dataRequest = dataRequest.substring(1, dataRequest.length()-1);

                resultData.add(dataRequest);

                // Handling The Artist
                if (result.select("h4").text().hashCode() == 2582837)
                    resultData.add(result.select("div.performers").select("a").text());
                else
                    resultData.add(result.select("div.artist").text());

                // Handling The Year (only for Albums)
                resultData.add(result.select("div.year").text());

                // Handing the Genre (only for Albums)
                resultData.add(result.select("div.genres").text());

                // Determining if it's an album or an image
                try{
                    result.select("div.cover").get(0); // Only exists for albums, hence will error on songs
                    resultData.add("Album");
                } catch (Exception e) {
                    resultData.add("Song");
                }

                // Handling the image link (only for Albums)
                resultData.add(result.select("img.lazy").attr("data-original"));

                // Page Link for Albums
                resultData.add(result.select("div.title").select("a").attr("href"));

                resultData.add(Integer.toString(results.indexOf(result))); // Identifier

                songsData.add(resultData);
            }
        }

        return songsData;
    }

    public static synchronized ArrayList<ArrayList<String>> youtubeQuery(String query) throws IOException, JSONException {

        // [Web Data] -> JavaScript -> String -> Json -> Data
        Document youtubeSearch = Jsoup.connect("https://www.youtube.com/results?search_query=" + query).get();
        ArrayList<ArrayList<String>> searchDataExtracted = new ArrayList<>();
        ArrayList<String> searchDataTemp;

        if (youtubeSearch.select("script").size() == 17) {
            // Youtube has given us the data we require embedded in the HTML and must be parsed from the HTML

            // Video Times: youtubeSearch.select("ol.item-section").get(0).select("span.video-time").get(selection)
            // Video Link: youtubeSearch.select("ol.item-section").get(0).select("a[href][aria-hidden]").get(selection).attr("href")

            for (int i = 0; i < youtubeSearch.select("ol.item-section").get(0).select("span.video-time").size(); i++) {

                searchDataTemp = new ArrayList<>();
                searchDataTemp.add("https://youtube.com" + youtubeSearch.select("ol.item-section").get(0).select("a[href][aria-hidden]").get(i).attr("href"));
                searchDataTemp.add(Integer.toString(timeConversion(youtubeSearch.select("ol.item-section").get(0).select("span.video-time").get(i).text())));
                searchDataExtracted.add(searchDataTemp);
            }

        } else {
            // YouTube has given us the data stored in json stored script tags which be parsed

            // Web Data -> [JavaScript] -> String -> Json -> Data
            Element jsData = youtubeSearch.select("script").get(24);

            // Web Data -> JavaScript -> [String] -> Json -> Data
            String jsonConversion = jsData.toString();
            jsonConversion = jsonConversion.substring(39, jsonConversion.length()-119);

            // Web Data -> JavaScript -> String -> [Json] -> Data
            JSONObject json = new JSONObject(jsonConversion);

            // contents -> twoColumnSearchResultsRenderer -> primaryContents -> sectionListRenderer
            JSONObject contents = (JSONObject)(
                    (JSONObject)(
                            (JSONObject)(
                                    (JSONObject)json.get("contents")
                            ).get("twoColumnSearchResultsRenderer")
                    ).get("primaryContents")
            ).get("sectionListRenderer");

            JSONArray contents1 = (JSONArray)contents.get("contents");
            JSONObject contents2 = (JSONObject)(
                    (JSONObject)contents1.get(0)
            ).get("itemSectionRenderer");

            JSONArray contents3 = (JSONArray)contents2.get("contents");

            //for (Object videoData: contents3)
            for (int i = 0; i < contents3.length(); i++)
            {
                searchDataTemp = new ArrayList<>();
                JSONObject jsonVideoData = contents3.getJSONObject(i);
                try {
                    // Extract the playtime and the link to the video
                    String lengthData = (String)((JSONObject)((JSONObject)jsonVideoData.get("videoRenderer")).get("lengthText")).get("simpleText");
                    String watchLink = "https://www.youtube.com/watch?v=" + ((JSONObject)jsonVideoData.get("videoRenderer")).get("videoId");

                    //String title = (String)((JSONObject)((JSONObject)((JSONObject)jsonVideoData.get("videoRenderer")).get("title")).get("runs")).get("text");

                    searchDataTemp.add(watchLink);
                    searchDataTemp.add(Integer.toString(timeConversion(lengthData)));


                    searchDataExtracted.add(searchDataTemp);
                } catch (Exception ignored) {}
            }

        }

        return searchDataExtracted;
    }

    public static synchronized String generateFolder(String folderRequest) {
        if (Files.exists(Paths.get(folderRequest))) {
            int i = 1; // Looks better than Album (0)
            while (true) {

                // File exists so move onto the next one
                if (Files.exists(Paths.get(folderRequest + "(" + i + ")"))) {
                    i++;
                } else {
                    if (new File(folderRequest + "(" + i + ")").mkdir())
                        return folderRequest + "(" + i + ")";
                    else {
                        System.out.println("Failed to create directory: " + folderRequest + "(" + i + ")");
                        System.exit(-1);
                    }
                }
            }
        } else {
            if (new File(folderRequest).mkdir())
                return folderRequest;
            else {
                System.out.println("Failed to create directory: " + folderRequest);
                System.exit(-1);
            }
        }

        return ""; // Unreachable

    }

    public static synchronized void downloadAlbumArt(String albumDirectory, String urlRequest) throws IOException {

        URL url = new URL(urlRequest);
        InputStream in = new BufferedInputStream(url.openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] buf = new byte[1024];
        int n;

        while (-1!=(n=in.read(buf))) {
            out.write(buf, 0, n);
        }

        out.close();
        in.close();

        byte[] response = out.toByteArray();

        // Write the album art to the folder
        FileOutputStream fos = new FileOutputStream(albumDirectory + "art.jpg");
        fos.write(response);
        fos.close();
    }

    public static synchronized String evaluateBestLink(ArrayList<ArrayList<String>> fullData, int songRealTime){

        int bestTimeDifference = Integer.MAX_VALUE;
        int indexOfBest = -1;

        for (ArrayList<String> video: fullData)
        {
            if (Math.abs(Integer.parseInt(video.get(1)) - songRealTime) < bestTimeDifference){
                bestTimeDifference = Math.abs(Integer.parseInt(video.get(1)) - songRealTime);
                indexOfBest = fullData.indexOf(video);
            }
        }

        return fullData.get(indexOfBest).get(0);

    }

    public static synchronized boolean downloadYoutubedl(Thread t) {

        boolean needsDownload = true;

        String latestVersionSource = "";
        String latestVersion = "";

        // Locating the latest version
        try {
            Document githubPageSrc = Jsoup.connect("https://ytdl-org.github.io/youtube-dl/download.html").get();
            latestVersionSource = githubPageSrc.select("a").get(2).attr("href");
            latestVersion = latestVersionSource.split("/")[4];

        } catch (IOException e) {
            Debug.error(t, "Error sending web request: https://ytdl-org.github.io/youtube-dl/download.html", e.getCause());
            return false;
        }

        // Check if the folder to be stored in already exists
        if (!Files.exists(Paths.get("C:\\Program Files (x86)\\youtube-dl\\"))) {
            new File("C:\\Program Files (x86)\\youtube-dl\\");
        }

        // Checking if the folder already exists
        if (Files.exists(Paths.get("C:\\Program Files (x86)\\youtube-dl\\youtube-dl.exe"))) {

            try {
                // Check the file is operational, if it is, no need to download
                ProcessBuilder builder = new ProcessBuilder("C:\\Program Files (x86)\\youtube-dl\\youtube-dl.exe", "--version");
                builder.redirectErrorStream(true);
                Process p = builder.start();

                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

                if (r.readLine().equals(latestVersion)) {
                    // User has the latest version
                    needsDownload = false;
                }

            } catch (IOException ignored) {} // Cannot happen
        }

        // Downloading latest version
        if (needsDownload) {

            // Completing the download
            try {
                URL url = new URL(latestVersionSource);
                File youtubeDlExe = new File("C:\\Program Files (x86)\\youtube-dl\\youtube-dl.exe"); // Overwrite previous if needed
                FileUtils.copyURLToFile(url, youtubeDlExe);
            } catch (IOException e) {
                Debug.error(t, "Invalid Permissions, run as administrator" + latestVersionSource, e.getCause());
                return false;
            }
        }

        // Must now bind to path so youtube-dl called executes that

        return false;
    }

    public static synchronized boolean downloadFFMPEG(Thread t) {
        return false;
    }

    public static synchronized String generateNewId(JSONArray temporaryData, JSONArray downloadsQueue) throws IOException, JSONException {

        String id = Double.toString(Math.random()).split("\\.")[1];

        // Checking in temporary data
        if (idExistsInData(temporaryData, id)) {
            return generateNewId(temporaryData, downloadsQueue);
        }

        // Checking in actual downloads queue
        for (int i = 0; i < downloadsQueue.length(); i++) {

            if (idExistsInData( (JSONArray) ((JSONObject) downloadsQueue.get(i)).get("songs"), id )) {
                return generateNewId(temporaryData, downloadsQueue);
            }

        }

        // Checking in downloads history
        try {
            JSONArray fileData = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());
            if (idExistsInData(fileData, id)) {
                return generateNewId(temporaryData, downloadsQueue);
            }
        } catch (NoSuchElementException ignored) {}

        // Did not match existing records, return generated ID
        return id;
    }

    public static synchronized String generateNewCacheArtId(JSONArray downloadsQueue) {

        // Generating the potential ID
        String id = Double.toString(Math.random()).split("\\.")[1];

        // Checking if it exists in the downloads queue
        try {
            for (int i = 0; i < downloadsQueue.length(); i++) {

                if (downloadsQueue.getJSONObject(i).getJSONObject("meta").get("artId").equals(id)) {
                    // Our generated ID already exists in the queue, generate a new one
                    return generateNewCacheArtId(downloadsQueue);
                }

            }
        } catch (JSONException ignored) {}

        // Checking if it exists in existing cached arts
        File[] existingCache = new File("resources\\cache").listFiles();
        for (File cachedArt: existingCache) {
            if (cachedArt.isFile() && cachedArt.getName().split("\\.")[1].equals("jpg") && cachedArt.getName().split("\\.")[0].equals(id) ) {
                // Our generated ID already exists in the files, generate a new one
                return generateNewCacheArtId(downloadsQueue);
            }
        }

        // Generated ID was not found to match any existing record, hence use this ID
        return id;

    }

    public static synchronized JSONArray deleteFromJSONArray(JSONArray data, int target) {

        try {
            ArrayList<String> list = new ArrayList<>();

            for (int j = 0; j < data.length(); j++) {
                list.add(data.get(j).toString());
            }

            list.remove(target);
            return new JSONArray(list);

        } catch (JSONException e) {
            Debug.error(null, "Error deleting from JSONArray.", e.getCause());
            return new JSONArray();
        }

    }

    public static synchronized void updateDownloads(JSONObject addition) throws IOException, JSONException {

        JSONArray downloadHistory = new JSONArray();
        try {
            downloadHistory = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());
        } catch (NoSuchElementException ignored) { }

        downloadHistory.put(downloadHistory.length(), addition);
        FileWriter recordUpdater = new FileWriter("resources\\json\\downloads.json");
        recordUpdater.write(downloadHistory.toString());
        recordUpdater.close();

    }

    private static synchronized boolean idExistsInData(JSONArray songs, String id) throws NoSuchElementException{

        try {
            for (int i = 0; i < songs.length(); i++)
            {
                if ((songs.getJSONObject(0)).get("id").equals(id)) {
                    return true;
                }
            }
        } catch (JSONException | NoSuchElementException ignored) {}
        return false;

    }

}
