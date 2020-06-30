package sample;

import javafx.scene.image.ImageView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Model {

    private final static Model instance = new Model();

    private JSONObject settings = SettingsFunc.getSettings();
    private JSONArray downloadQueue = new JSONArray();
    private resultsSet[] searchResults;

    public static Model getInstance() {
        return instance;
    }

    public void setSearchResults(resultsSet[] generatedSearchResults) {
        searchResults = generatedSearchResults;
    }

    public resultsSet[] getSearchResults() {
        return searchResults;
    }

    // Allow Access to Downloads
    public boolean downloadsAccessible() {

        // Should allow access to downloads if there is: download history or downloads in progress
        JSONArray downloadHistory = new JSONArray();
        try {

            downloadHistory = new JSONArray(new Scanner(new File("resources\\json\\config.json")).useDelimiter("\\Z").next());

        } catch (FileNotFoundException e) {
            // Regenerate the downloads file

        } catch (JSONException ignored) {}

        return downloadHistory.length() > 0 || downloadQueue.length() > 0;

    }

    // Settings: Settings & Model
    public JSONObject getSettings() {
        return settings;
    }
    public void setSettings(JSONObject newSettings) {
        settings = newSettings;
    }

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

}