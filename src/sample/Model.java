package sample;

import com.musicg.fingerprint.FingerprintManager;
import com.musicg.fingerprint.FingerprintSimilarityComputer;
import com.musicg.wave.Wave;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

// TODO
// Switch to using actual boolean values instead of 0 and 1

// TODO
// On downloads class, add an advanced audio referential check setting
// If enabled once the target file is downloaded, open the all music song link -> get music player 30 second clip download -> create trimmed version of song to 30 seconds -> compare

public class Model {

    private final static Model instance = new Model();

    public final settings settings = new settings();
    public final download download = new download();
    public final search search = new search();

    public Model() {

        // Runs in background and checks the cache for optimisations every five minutes to ensure files aren't built up unnecessarily
        Timer cacheOptimiser = new Timer();
        cacheOptimiser.schedule(new TimerTask() {
            @Override
            public synchronized void run() {
                // List all files
                File[] cachedFiles = new File("resources\\cached").listFiles();
                ArrayList<String> imgData = new ArrayList<>();
                ArrayList<ArrayList<String>> rename = new ArrayList<>();

                // File sizes at different times
                int deletedFiles = 0;
                int originalSize = Arrays.stream(Objects.requireNonNull(new File("resources\\cached\\").listFiles())).mapToInt(existingFile -> (int) existingFile.length()).sum();

                if (cachedFiles == null | Objects.requireNonNull(cachedFiles).length == 0)
                    return;

                // Load binary data, md5 hashes instead of keeping full file in memory
                for (File workFile : cachedFiles) {
                    if (workFile.getName().split("\\.")[1].equals("jpg")) {

                        try {
                            String hash = DigestUtils.md5Hex(
                                    Files.newInputStream(
                                            Paths.get(workFile.getAbsolutePath())
                                    )
                            );

                            if (imgData.contains(hash)) {

                                // Exists in data hence to setup a rename reference
                                rename.add(
                                        new ArrayList<>(
                                                Arrays.asList(
                                                        workFile.getName(),
                                                        cachedFiles[imgData.indexOf(hash)].getName()
                                                )
                                        )
                                );

                            } else {
                                imgData.add(hash);
                            }


                        } catch (IOException ignored) { }

                    } else {

                        if (!workFile.delete())
                            Debug.warn(null, "Failed to delete non jpeg file in cache: " + workFile.getAbsolutePath());
                        else
                            deletedFiles++;
                    }

                }

                // Begin the process of deleting files and renaming references
                for (ArrayList<String> fileNames : rename) {

                    // Delete the old no longer relevant file
                    if (!new File("resources/cached/" + fileNames.get(0)).delete())
                        Debug.warn(null, "Failed to delete file: " + new File("resources\\cached\\" + fileNames.get(0)).getAbsolutePath());
                    else
                        deletedFiles++;

                    // Delete all references to the file in the downloads history
                    try {

                        // Can cause an error if the file is blank, but will load the jsonarray
                        JSONArray downloadsHistory = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());

                        // Iterate through downloads history, replacing the old references with the new
                        for (int i = 0; i < downloadsHistory.length(); i++) {

                            try {
                                if (downloadsHistory.getJSONObject(i).get("cached").toString().equals("resources/cached/" + fileNames.get(1)))
                                    downloadsHistory.getJSONObject(i).put("cached", "resources/cached/" + fileNames.get(0));
                            } catch (JSONException ignored) {
                            }

                        }

                        // Writing changes
                        try {
                            FileWriter updateDownloads = new FileWriter("resources\\json\\downloads.json");
                            updateDownloads.write(downloadsHistory.toString());
                            updateDownloads.close();

                        } catch (IOException e) {
                            // Likely file deleted, adjust to handle, for now just error
                            Debug.error(null, "Error writing to downloads file", e.getCause());
                        }

                    } catch (JSONException e) {

                        // There is no download history and no use of the cached files, hence delete them
                        for (File deleteFile : cachedFiles) {
                            if (!deleteFile.delete()) {
                                Debug.warn(null, "Failed to delete file: " + deleteFile.getAbsolutePath());
                            }
                        }
                        return;

                    } catch (FileNotFoundException ignored) {
                        // Generate a new downloads folder
                    }

                }

                if (deletedFiles > 0) {
                    int currentSize = Arrays.stream(Objects.requireNonNull(new File("resources\\cached\\").listFiles())).mapToInt(existingFile -> (int) existingFile.length()).sum();
                    Debug.trace(
                            null,
                            String.format(
                                    "Cache optimisation finished, deleted %s files, a cache size reduction of %2.2f%%",
                                    deletedFiles,
                                    ((double) (originalSize - currentSize) / originalSize) * 100
                            )
                    );
                } else {
                    Debug.trace(null, "Cache is optimised.");
                }

            }
        }, 0, 60 * 1000);

    }

    public static Model getInstance() {
        return instance;
    }

    public static class search {
        private resultsSet[] searchResults;
        private JSONArray searchResultsJson;

        public resultsSet[] getSearchResults() {
            return searchResults;
        }

        public void setSearchResults(resultsSet[] searchResults) {
            this.searchResults = searchResults;
        }

        public JSONArray getSearchResultsJson() {return searchResultsJson; }

        public void setSearchResultsJson(JSONArray searchResultsJson) {
            this.searchResultsJson = searchResultsJson;
        }
    }

    public static class download {
        private acquireDownloadFiles downloader;
        private volatile JSONArray downloadQueue = new JSONArray();
        private volatile JSONObject downloadObject = new JSONObject();
        private JSONArray downloadHistory = new JSONArray();

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

        public synchronized acquireDownloadFiles getDownloader() {
            try {
                return downloader;
            } catch (NullPointerException ignored) {
                return null;
            }
        }

        public synchronized void updateDownloadQueue(JSONObject queueItem) {
            if (downloadQueue.length() == 0 && !downloadObject.has("songs")) {

                // No downloads in progress or in queue, hence start a new download thread.
                Debug.trace(null, "New download request received, queue is blank and hence will begin downloading...");
                downloadObject = queueItem;
                downloader = new acquireDownloadFiles(downloadObject);

            } else {

                // Download already in progress, hence queue
                Debug.trace(null, String.format("New download request received, adding to queue in position %s.", Model.getInstance().download.getDownloadQueue().length()+1));
                downloadQueue.put(queueItem);

            }
        }

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

        // TODO Implement the functionality of downloading
        class acquireDownloadFiles implements Runnable {
            Thread thread;
            JSONObject downloadData;

            public acquireDownloadFiles(JSONObject downloadData) {
                this.downloadData = downloadData;

                // Not a daemon, a kill must handle this properly
                thread = new Thread(this, "acquire-download-files");
                thread.start();
            }

            // 0.7+ Acceptable
            private float evaluateDownloadValidity (String sampleFile, String downloadedFile) {

                try {

                    byte[] sampleData = {};
                    byte[] downloadedData = {};

                    String sampleFileTemp = "tmp_" + Double.toString(Math.random()).split("\\.")[1] + ".wav";
                    String fullFileTemp = "tmp_" + Double.toString(Math.random()).split("\\.")[1] + ".wav";

                    // Heavy, consider checking if this is necessary
                    new Converter().convert(sampleFile, sampleFileTemp);
                    new Converter().convert(downloadedFile, fullFileTemp);

                    try {
                        sampleData = new FingerprintManager().extractFingerprint(new Wave(sampleFileTemp));
                        downloadedData = new FingerprintManager().extractFingerprint(new Wave(fullFileTemp));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Debug.warn(null, "File is too large to compare.");
                    }

                    if (!new File(sampleFileTemp).delete())
                        Debug.warn(null, "Failed to delete file: " + sampleFileTemp);
                    if (!new File(fullFileTemp).delete())
                        Debug.warn(null, "Failed to delete file: " + fullFileTemp);

                    FingerprintSimilarityComputer fingerprint = new FingerprintSimilarityComputer(sampleData, downloadedData);
                    return fingerprint.getFingerprintsSimilarity().getScore();

                } catch (JavaLayerException e) {
                    Debug.error(null, String.format("Error comparing files: %s and %s", sampleFile, downloadedFile), e.getCause());
                }

                return 1;
            }

            private String generateFolder(String folderRequest) {

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
                                Debug.error(thread, "Failed to create directory: " + folderRequest + "(" + i + ")", null);
                            }
                        }
                    }
                } else {
                    if (new File(folderRequest).mkdir())
                        return folderRequest;
                    else {
                        Debug.error(thread, "Failed to create directory: " + folderRequest, null);
                    }
                }

                return "";

            }

            @Override
            public void run() {

                // Making the folder to contain the downloads
                try {
                    downloadObject.getJSONObject("metadata").put("directory", generateFolder(downloadObject.getJSONObject("metadata").getString("directory")));
                } catch (JSONException ignored) {}

                // Download the album art
                try {
                    FileUtils.copyURLToFile(
                            new URL(downloadObject.getJSONObject("metadata").getString("art")),
                            new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg")
                    );
                } catch (IOException e) {
                    Debug.error(thread, "Failed to download album art.", e.getCause());
                } catch (JSONException ignored) {}

                // Download files
                

                // Check song if necessary

                // Apply meta data

                // Move onto the next item if necessary
                Platform.runLater(() -> {
                    if (downloadQueue.length() > 0) {
                        try {
                            // Spawning new thread
                            downloadObject = downloadQueue.getJSONObject(0);
                            downloader = new acquireDownloadFiles(downloadObject);

                            // Update queue
                            if (downloadQueue.length() > 1) {

                                // Move queued item to the model and respawn this thread.
                                JSONArray newQueue = new JSONArray();
                                for (int i = 1; i < downloadQueue.length(); i++) {
                                    newQueue.put(downloadQueue.getJSONObject(i));
                                }

                                downloadQueue = newQueue;

                            } else {

                                // Queue had only one item, hence has now been cleared

                                downloadQueue = new JSONArray();

                            }

                        } catch (JSONException e) {
                            Debug.error(thread, "Failed to process queue.", e.getCause());
                        }

                    } else {
                        downloadObject = new JSONObject();
                    }
                });

            }
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