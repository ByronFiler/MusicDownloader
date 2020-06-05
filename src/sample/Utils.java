package sample;

import javafx.scene.image.ImageView;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Utils {

    public View view;

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

    public static synchronized ArrayList<ArrayList<String>> allmusicQuery(String query) throws IOException {

        ArrayList<ArrayList<String>> songsData = new ArrayList<>();
        ArrayList<String> resultData;

        Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + query).get();
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

    public static synchronized ArrayList<ArrayList<String>> youtubeQuery(String query) throws IOException, ParseException {
        // [Web Data] -> JavaScript -> String -> Json -> Data
        Document youtubeSearch = Jsoup.connect("https://www.youtube.com/results?search_query=" + query).get();

        // Web Data -> [JavaScript] -> String -> Json -> Data
        Element jsData = youtubeSearch.select("script").get(25);

        // Web Data -> JavaScript -> [String] -> Json -> Data
        String jsonConversion = jsData.toString();
        jsonConversion = jsonConversion.substring(39, jsonConversion.length()-119);

        // Web Data -> JavaScript -> String -> [Json] -> Data
        JSONParser queryData = new JSONParser();
        JSONObject json = (JSONObject) queryData.parse(jsonConversion);

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

        ArrayList<ArrayList<String>> searchDataExtracted = new ArrayList<>();
        ArrayList<String> searchDataTemp;

        for (Object videoData: contents3)
        {
            searchDataTemp = new ArrayList<>();
            JSONObject jsonVideoData = (JSONObject)videoData;
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

        System.out.println(albumDirectory);
        System.out.println(urlRequest);

        URL url = new URL(urlRequest);
        InputStream in = new BufferedInputStream(url.openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while (-1!=(n=in.read(buf)))
        {
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

}
