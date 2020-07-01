package sample;

import javafx.scene.image.ImageView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Model {

    private final static Model instance = new Model();

    private final JSONArray downloadQueue = new JSONArray();
    private resultsSet[] searchResults;
    public final settings settings = new settings();
    public final download download = new download();

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

    public static class download {

        private JSONArray downloadQueue = new JSONArray();
        private JSONArray downloadHistory = new JSONArray();
        private volatile JSONObject downloadObject = new JSONObject();

        public download() {

            try {
                downloadHistory = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());
                Debug.trace(null, String.format("Found a download history of %s item(s).", downloadHistory.length()));
            } catch (FileNotFoundException | JSONException e) {
                try {
                    Debug.trace(null, "No download history found.");
                    if (!new File("resources\\json\\downloads.json").createNewFile()) {
                        throw new IOException();
                    }
                } catch (IOException ex) {
                    Debug.error(null, "Failed to create downloads file.", ex.getCause());
                }
            }

        }

        public synchronized JSONArray getDownloadHistory() {
            return downloadHistory;
        }

        public synchronized JSONArray getDownloadQueue() {
            return downloadQueue;
        }

        public synchronized JSONObject getDownloadObject() {
            return downloadObject;
        }

    }

    public static class settings {

        private JSONObject settings;
        private JSONObject defaultSettings;
        private String version;

        public settings() {

            // Declare default settings for reference
            try{
                defaultSettings = new JSONObject("{\"output_directory\":\"\",\"save_album_art\":1,\"music_format\":0, \"album_art\":1, \"album_title\":1, \"song_title\":1, \"artist\":1, \"year\":1, \"track\":1,\"theme\":0, \"data_saver\":0}");
            } catch (JSONException ignored) {}

            // Load users actual settings
            try {
                settings = new JSONObject(new Scanner(new File("resources\\json\\config.json")).useDelimiter("\\Z").next());
            } catch (FileNotFoundException | JSONException ignored) {
                settings = defaultSettings;
                resetSettings();
            }

            // Load the version
            try {
                version = new JSONObject(new Scanner(new File("resources\\json\\meta.json")).useDelimiter("\\Z").next()).get("version").toString();
            } catch (FileNotFoundException | JSONException e) {
                Debug.warn(null, "Failed to locate version.");
                version = null;
            }

        }

        private void resetSettings() {

            try {

                FileWriter newConfig = new FileWriter("resources\\json\\config.json");
                newConfig.write(defaultSettings.toString());
                newConfig.close();

            } catch (IOException e) {
                Debug.error(null, "Failed to reset settings.", e.getCause());
            }
        }

        public void saveSettings(JSONObject settings) {
            try {
                FileWriter settingsFile = new FileWriter("resources/json/config.json");
                settingsFile.write(settings.toString());
                settingsFile.close();

                this.settings = settings;

            } catch (IOException e) {
                Debug.error(null, "Failed to update settings file.", e.getCause());
            }
        }

        public synchronized boolean getSettingBool(String key) {
            return Integer.parseInt(getSetting(key)) != 0;
        }

        public synchronized String getSetting(String key) {

            try {
                return settings.get(key).toString();

            } catch (JSONException e) {

                // Determine if it was my fault for using a bad key or settings for having bad data
                if (defaultSettings.has(key)) {
                    Debug.warn(null, "Failed to load correct settings, resetting settings.");
                    resetSettings();
                } else {
                    Debug.error(null, "Invalid key specified in settings: " + key, e.getCause());
                }

                return null;

            }

        }

        public synchronized JSONObject getSettings() {
            return settings;
        }

        public String getVersion() {
            return version;
        }

    }

    // Search results table format
    @SuppressWarnings("unused")
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