package sample;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class Utils {

    public static synchronized ArrayList<ArrayList<String>> allmusicQuery(String query) throws IOException {

        ArrayList<ArrayList<String>> songsData = new ArrayList<>();
        ArrayList<String> resultData;

        Document doc = Jsoup.connect("https://www.allmusic.com/search/all/" + query).get();
        Elements mst = doc.select("ul.search-results");
        Elements results = mst.select("li");

        for (Element result: results)
        {
            // Check that it's either a album or an song, not an artist, the data is a bit odd so the hashcode fixes it
            if (result.select("h4").text().hashCode() != 1969736551) {
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

                songsData.add(resultData);
            }
        }

        return songsData;
    }

}
