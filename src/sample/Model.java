package sample;

import com.mpatric.mp3agic.*;
import com.musicg.fingerprint.FingerprintManager;
import com.musicg.fingerprint.FingerprintSimilarityComputer;
import com.musicg.wave.Wave;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javazoom.jl.converter.Converter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
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

                        // Can cause an error if the file is blank, but will load the JSON Array
                        JSONArray downloadsHistory = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());

                        // Iterate through downloads history, replacing the old references with the new
                        for (int i = 0; i < downloadsHistory.length(); i++) {
                            try {
                                if (downloadsHistory.getJSONObject(i).getString("artId").equals(fileNames.get(0).replace(".jpg", "")))
                                    downloadsHistory.getJSONObject(i).put("artId", fileNames.get(1).replace(".jpg", ""));
                            } catch (JSONException ignored) {
                                Debug.warn(null, "Failed to work on downloads file.");
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
        private final List<String> songReferences = Arrays.asList("mp3", "wav", "ogg", "aac");
        private volatile JSONObject historyItem = new JSONObject();

        public download() {

            try {
                JSONArray downloadHistory = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());
                Debug.trace(null, String.format("Found a download history of %s item(s).", downloadHistory.length()));
            } catch (FileNotFoundException e) {
                try {
                    Debug.trace(null, "No download history found.");
                    if (!new File("resources\\json\\downloads.json").createNewFile())
                        throw new IOException();
                } catch (IOException ex) {
                    Debug.error(null, "Failed to create downloads file.", ex.getCause());
                }
            } catch (JSONException | NoSuchElementException ignored) {}

        }

        public synchronized JSONArray getDownloadHistory() {

            JSONArray downloadHistory = new JSONArray();
            try {
                downloadHistory =
                        new JSONArray(
                            new Scanner(
                                new File("resources\\json\\downloads.json")
                            ).useDelimiter("\\Z").next()
                );
            } catch (FileNotFoundException | JSONException e) {
                try {
                    if (Files.exists(Paths.get("resources\\json\\downloads.json")))
                        if (!new File("resources\\json\\downloads.json").createNewFile())
                            throw new IOException();
                } catch (IOException er) {
                    Debug.warn(null, "Failed to create new downloads history folder.");
                }
            }

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

                downloadHistory = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());

            } catch (FileNotFoundException e) {
                // Regenerate the downloads file

            } catch (JSONException ignored) {}

            return downloadHistory.length() > 0 || downloadQueue.length() > 0 || downloadObject.has("metadata");

        }

        public void setHistoryItem(JSONObject historyItem) {
            this.historyItem = historyItem;
        }

        public JSONObject getHistoryItem() {
            return historyItem;
        }

        public void deleteHistory(JSONObject targetDeletion) {

            // TODO: Fix
            // Delete all data not just target

            JSONArray downloadHistory = new JSONArray();
            JSONArray newDownloadHistory = new JSONArray();

            // Loading existing history
            try {
                downloadHistory = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());
            } catch (FileNotFoundException | JSONException e) {
                Debug.trace(null, "Failed to read history to delete");
            }

            // Adding all to history except history item to remove
            try {
                for (int i = 0; i < downloadHistory.length(); i++) {
                    if (!downloadHistory.getJSONObject(i).toString().equals(targetDeletion.toString())) {
                        newDownloadHistory.put(downloadHistory.getJSONObject(i));
                    }
                }
            } catch (JSONException e) {
                Debug.error(null, "Failed to validate download history to remove element.", e.getCause());
            }

            // Rewriting the new history
            try {
                FileWriter downloadHistoryFile = new FileWriter("resources\\json\\downloads.json");
                downloadHistoryFile.write(newDownloadHistory.toString());
                downloadHistoryFile.close();
            } catch (IOException e) {
                Debug.error(null, "Error writing new download history.", e.getCause());
            }

        }

        private void updateDownloadHistory(JSONObject newHistory) {

            JSONArray existingHistory = new JSONArray();
            try {
                existingHistory = new JSONArray(new Scanner(new File("resources\\json\\downloads.json")).useDelimiter("\\Z").next());
            } catch (FileNotFoundException e) {

                // Recreate the downloads file
                try {
                    if (new File("resources\\json\\downloads.json").createNewFile())
                        Debug.trace(null, "Created new downloads history file.");

                } catch (IOException er) {
                    Debug.error(null, "Failed to recreate downloads folder.", e.getCause());
                }

            } catch (JSONException | NoSuchElementException ignored) {}
            existingHistory.put(newHistory);

            try {
                FileWriter updateHistory = new FileWriter("resources\\json\\downloads.json");
                updateHistory.write(existingHistory.toString());
                updateHistory.close();
            } catch (IOException e) {
                Debug.warn(null, "Failed to write updated downloads history.");
            }

        }

        class acquireDownloadFiles implements Runnable {
            Thread thread;
            JSONObject downloadData;

            public acquireDownloadFiles(JSONObject downloadData) {
                this.downloadData = downloadData;

                // Not a daemon, a kill must handle this properly
                thread = new Thread(this, "acquire-download-files");
                thread.start();
            }

            private float evaluateDownloadValidity (String sampleFileSource, String downloadedFile) {

                // Preparing sample: Downloading sample
                try {
                    FileUtils.copyURLToFile(
                            new URL(sampleFileSource),
                            new File("smp.mp3")
                    );
                } catch (IOException e) {
                    Debug.warn(thread, "Error connecting to: " + sampleFileSource);
                    // Handle reconnection
                }

                // Preparing sample: Converting to comparable wav format from mp3 & deleting old mp3
                try {
                    new Converter().convert("smp.mp3", "smp.wav");
                } catch (Exception ignored) {} // Falsely throws an exception even on success

                if (!new File("smp.mp3").delete())
                    Debug.warn(thread, "Failed to delete : smp.mp3");

                // Checking if the download file needs to be converted

                byte[] sampleData = new byte[0];
                byte[] downloadData = new byte[0];
                try {
                    try {
                        downloadData = new FingerprintManager().extractFingerprint(new Wave(downloadedFile));
                        sampleData = new FingerprintManager().extractFingerprint(new Wave("smp.wav"));
                    } catch (Exception e) {
                        try {
                            new Converter().convert(downloadedFile, "dl.wav");
                            downloadData = new FingerprintManager().extractFingerprint(new Wave("dl.wav"));
                        } catch (Exception ignored) {} // Always errors even on success, ignored
                    }

                } catch (ArrayIndexOutOfBoundsException ignored) {
                    Debug.warn(thread, "File is too large to be checked.");
                    return 1;
                }

                // Deleting temporary files
                if (!new File("dl.wav").delete())
                    Debug.warn(thread, "Failed to delete file: dl.wav");
                if (!new File("smp.wav").delete())
                    Debug.warn(thread, "Failed to delete file: smp.wav");

                FingerprintSimilarityComputer fingerprint = new FingerprintSimilarityComputer(sampleData, downloadData);
                return fingerprint.getFingerprintsSimilarity().getScore();
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

            private synchronized void downloadFile(JSONObject song, String format, int sourceDepth, String index) throws IOException, JSONException {

                // Start download
                FileWriter batCreator = new FileWriter("exec.bat");
                batCreator.write(
                        String.format(
                            "youtube-dl --extract-audio --audio-format %s --ignore-errors --retries 10 https://www.youtube.com/watch?v=%s",
                            format,
                            song.getJSONArray("source").getString(sourceDepth)
                    )
                );
                batCreator.close();

                ProcessBuilder builder = new ProcessBuilder("exec.bat");
                builder.redirectErrorStream(true);
                Process process = builder.start();
                InputStream is = process.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                String downloadedFile = "";
                while ((line = reader.readLine()) != null) {
                    // Can parse this later
                    if (line.contains("[ffmpeg]")) {
                        downloadedFile = line.substring(22);
                    }

                }

                // Delete now useless bat
                if (!new File("exec.bat").delete())
                    Debug.warn(thread, "Failed to delete file: exec.bat");

                // Validate
                if (Model.getInstance().settings.getSettingBool("advanced_validation")) {
                    float downloadValidity = evaluateDownloadValidity(
                            String.format("https://rovimusic.rovicorp.com/playback.mp3?c=%s=&f=I", song.getString("sample")),
                            downloadedFile
                    );

                    Debug.trace(
                            thread,
                            String.format(
                                    "Calculated downloaded validity of %s at %2.2f [%s]",
                                    song.getString("title"),
                                    downloadValidity * 100,
                                    downloadValidity > 0.7 ? "PASS" : "FAIL"
                            )
                    );

                    if (downloadValidity <= 0.7) {

                        // Delete downloaded files & sample
                        if (!new File(downloadedFile).delete()) {
                            Debug.warn(thread, "Failed to delete: " + downloadedFile);
                        }

                        if (song.getJSONArray("source").length() > sourceDepth + 2) {
                            // Can continue and move onto the next source
                            downloadFile(song, format, sourceDepth + 1, index);
                            return;
                        } else {
                            Debug.warn(thread, "Failed to find a song in sources that was found to be valid, inform user of failure.");
                        }

                    }
                }

                // Apply meta-data
                if (format.equals("mp3")) {

                    try {
                        Mp3File mp3Applicator = new Mp3File(downloadedFile);

                        ID3v2 id3v2tag = new ID3v24Tag();
                        mp3Applicator.setId3v2Tag(id3v2tag);

                        if (Model.getInstance().settings.getSettingBool("album_art")) {
                            // Could break this up into mb loads

                            RandomAccessFile art = new RandomAccessFile(
                                    new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg"),
                                    "r"
                            );

                            byte[] bytes;
                            bytes = new byte[(int) art.length()]; // Maybe make this 1024
                            art.read(bytes);
                            art.close();

                            id3v2tag.setAlbumImage(bytes, "image/jpg");
                        }

                        // Applying remaining data
                        if (Model.getInstance().settings.getSettingBool("album_title"))
                            id3v2tag.setTitle(downloadObject.getJSONObject("metadata").getString("album"));

                        if (Model.getInstance().settings.getSettingBool("song_title"))
                            id3v2tag.setAlbum(song.getString("title"));

                        if (Model.getInstance().settings.getSettingBool("artist")) {
                            id3v2tag.setArtist(downloadObject.getJSONObject("metadata").getString("artist"));
                            id3v2tag.setAlbumArtist(downloadObject.getJSONObject("metadata").getString("artist"));
                        }

                        if (Model.getInstance().settings.getSettingBool("year"))
                            id3v2tag.setYear(downloadObject.getJSONObject("metadata").getString("year"));

                        // Add this as a possible setting, also contain
                        id3v2tag.setTrack(index);

                        try {
                            mp3Applicator.save(downloadObject.getJSONObject("metadata").getString("directory") + "\\" + song.getString("title") + "." + format);

                            // Delete old file
                            if (!new File(downloadedFile).delete()) {
                                Debug.error(thread, "Failed to delete file: " + downloadedFile, new IOException().getCause());
                            }

                        } catch (IOException | NotSupportedException e) {
                            e.printStackTrace();
                        }
                    } catch (InvalidDataException | UnsupportedTagException e) {
                        Debug.warn(thread, "Failed to apply meta data to: " + downloadedFile);
                    }

                } else {

                    // Just move & rename the file
                    Files.move(
                            Paths.get(downloadedFile),
                            Paths.get(downloadObject.getJSONObject("metadata").getString("directory") + "\\" + song.getString("title") + "." + format)
                    );

                }

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

                // Cache the album art
                try {
                    Files.copy(
                            Paths.get(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg"),
                            Paths.get("resources\\cached\\" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg")
                    );
                } catch (JSONException ignored) {
                    Debug.warn(thread, "Failed to get JSON data to cache album art.");
                } catch (IOException ignored) {
                    Debug.warn(thread, "Failed to cache album art.");
                }

                // Download files
                try {
                    for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {

                        // Will call it's self recursively until it exhausts possible files or succeeds
                        try {
                            downloadFile(
                                    downloadObject.getJSONArray("songs").getJSONObject(i),
                                    songReferences.get(Model.getInstance().settings.getSettingInt("music_format")),
                                    0,
                                    String.valueOf(i+1)
                            );
                        } catch (IOException | JSONException e) {
                            try {
                                Debug.error(thread, "Error downloading song: " + downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"), e.getCause());
                            } catch (JSONException er) {
                                Debug.error(thread, "JSON Error downloading song", er.getCause());
                            }

                        }

                        Debug.trace(
                                thread,
                                String.format(
                                        "Successfully downloaded \"%s\" (%s of %s)",
                                        downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"),
                                        i+1,
                                        downloadObject.getJSONArray("songs").length()
                                )
                        );

                        // Updating the downloads history
                        try {

                            JSONObject downloadHistory = new JSONObject();
                            downloadHistory.put("title", downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"));
                            downloadHistory.put("artist", downloadObject.getJSONObject("metadata").getString("artist"));
                            downloadHistory.put("artUrl", downloadObject.getJSONObject("metadata").getString("art"));
                            downloadHistory.put("artId", downloadObject.getJSONObject("metadata").getString("artId"));
                            downloadHistory.put("directory", downloadObject.getJSONObject("metadata").getString("directory"));
                            downloadHistory.put("id", downloadObject.getJSONArray("songs").getJSONObject(i).getString("id"));
                            Model.getInstance().download.updateDownloadHistory(downloadHistory);

                        } catch (JSONException e) {
                            Debug.warn(thread, "Failed to generate JSON for download history result.");
                        }


                    }
                } catch (JSONException e) {
                    Debug.error(thread, "JSON Error when attempting to access songs to download.", e.getCause());
                }

                // Check if we should delete the album art
                try {
                    switch (Model.getInstance().settings.getSettingInt("save_album_art")) {

                        // Delete album art always
                        case 0:
                            if (!new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg").delete())
                                Debug.warn(thread, "Failed to delete: " + downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg");
                            break;

                        // Delete for songs
                        case 1:
                            if (downloadObject.getJSONArray("songs").length() == 1)
                                if (!new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg").delete())
                                    Debug.warn(thread, "Failed to delete: " + downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg");
                            break;

                        // Delete for albums
                        case 2:
                            if (downloadObject.getJSONArray("songs").length() > 1)
                                if (!new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg").delete())
                                    Debug.warn(thread, "Failed to delete: " + downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg");
                            break;
                    }
                } catch (JSONException e) {
                    Debug.error(thread, "Failed to perform check to delete album art.", e.getCause());
                }

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
                defaultSettings = new JSONObject("{\"advanced_validation\": true, \"output_directory\":\"\",\"save_album_art\":true,\"music_format\":0, \"album_art\":true, \"album_title\":true, \"song_title\":true, \"artist\":true, \"year\":true, \"track\":true,\"dark_theme\":false, \"data_saver\":false}");
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
            return Boolean.parseBoolean(getSetting(key));
        }

        public synchronized int getSettingInt(String key) {
            return Integer.parseInt(getSetting(key));
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