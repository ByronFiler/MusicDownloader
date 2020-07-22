package sample;

import com.mpatric.mp3agic.*;
import com.musicg.fingerprint.FingerprintManager;
import com.musicg.fingerprint.FingerprintSimilarityComputer;
import com.musicg.wave.Wave;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javazoom.jl.converter.Converter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Model {
    private final static Model instance = new Model();

    public final settings settings = new settings();
    public final download download = new download();
    public final search search = new search();

    public Model() {
        new Thread(() -> {

            JSONArray downloadHistory = download.getDownloadHistory();
            ArrayList<String> usedArtIds = new ArrayList<>();
            ArrayList<File> deleteFiles = new ArrayList<>();
            JSONArray filesData = new JSONArray();
            JSONArray renameRequests = new JSONArray();
            int deletedFilesCount = 0;

            try {
                for (int i = 0; i < downloadHistory.length(); i++) {

                    if (!usedArtIds.contains(downloadHistory.getJSONObject(i).getString("artId")))
                        usedArtIds.add(downloadHistory.getJSONObject(i).getString("artId"));
                }
            } catch (JSONException e) {
                Debug.error(null, "Failed to parse download history for art IDs.", e.getCause());
            }

            for (File foundFile: Objects.requireNonNull(new File("usr/cached").listFiles())) {

                // Check the file is an image and is being used
                if (FilenameUtils.getExtension(foundFile.getAbsolutePath()).equals("jpg") && usedArtIds.contains(FilenameUtils.removeExtension(foundFile.getName())) ) {

                    try {
                        String hash = DigestUtils.md5Hex(
                                Files.newInputStream(
                                        Paths.get(foundFile.getAbsolutePath())
                                )
                        );

                        boolean add = true;

                        for (int i = 0; i < filesData.length(); i++) {

                            if (filesData.getJSONObject(i).getString("hash").equals(hash)) {

                                renameRequests.put(
                                        new JSONObject(
                                                String.format(
                                                        "{\"original\": \"%s\", \"new\": \"%s\"}",
                                                        FilenameUtils.removeExtension(foundFile.getName()),
                                                        filesData.getJSONObject(i).getString("id")
                                                )
                                        )
                                );
                                deleteFiles.add(foundFile);
                                add = false;

                            }

                        }

                        if (add)
                            filesData.put(
                                    new JSONObject(
                                            String.format(
                                                    "{\"id\": \"%s\", \"hash\": \"%s\"}",
                                                    FilenameUtils.removeExtension(foundFile.getName()),
                                                    hash
                                            )
                                    )
                            );


                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    if (!foundFile.delete())
                        Debug.warn(null, "Failed to delete file: " + foundFile.getAbsolutePath());
                    else
                        deletedFilesCount++;
                }

            }

            if (deletedFilesCount > 0)
                Debug.trace(Thread.currentThread(), String.format("Deleted %s non used file%s.", deletedFilesCount, deletedFilesCount == 1 ? "" : "s"));

            try {

                for (int i = 0; i < renameRequests.length(); i++) {

                    for (int j = 0; j < downloadHistory.length(); j++) {

                        if (downloadHistory.getJSONObject(j).getString("artId").equals(renameRequests.getJSONObject(i).getString("original"))) {

                            downloadHistory.getJSONObject(j).put("artId", renameRequests.getJSONObject(i).getString("new"));

                        }

                    }

                }

                try {

                    download.setDownloadHistory(downloadHistory);

                    // Now to delete files
                    deletedFilesCount = 0;
                    for (File deleteFile: deleteFiles) {
                        if (!deleteFile.delete())
                            Debug.warn(Thread.currentThread(), "Failed to delete " + deleteFile.getAbsolutePath());
                        else
                            deletedFilesCount++;
                    }
                    if (deleteFiles.size() > 0) {

                        StringBuilder deletedFilesMessage = new StringBuilder(String.format(
                                "Successfully deleted %s file%s",
                                deletedFilesCount,
                                deletedFilesCount == 1 ? "" : "s"
                        ));

                        if (deletedFilesCount == deleteFiles.size())
                            deletedFilesMessage.append(".");

                        else
                            deletedFilesMessage.append(
                                    String.format(
                                            " and failed to delete %s file%s.",
                                            deleteFiles.size() - deletedFilesCount,
                                            deleteFiles.size() - deletedFilesCount == 1 ? "" : "s"
                                    )
                            );

                        Debug.trace(Thread.currentThread(), deletedFilesMessage.toString());

                    }

                } catch (IOException e) {
                    Debug.warn(null, "Failed to write updated downloads history.");
                }


            } catch (JSONException e) {
                Debug.error(null, "Failed to rewrite JSON data for download queue optimisation.", e.getCause());
            }

            // Start re-downloading missing files.
            JSONArray downloadObjects = new JSONArray();
            try {
                for (int i = 0; i < downloadHistory.length(); i++) {

                    if (!Files.exists(Paths.get(String.format("usr\\cached\\%s.jpg", downloadHistory.getJSONObject(i).getString("artId"))))) {

                        boolean alreadyPlanned = false;
                        if (downloadObjects.length() > 0) {
                            for (int j = 0; j < downloadObjects.length(); j++) {

                                if (downloadObjects.getJSONObject(j).getString("artUrl").equals(downloadHistory.getJSONObject(i).getString("artUrl"))) {
                                    alreadyPlanned = true;
                                    downloadHistory.getJSONObject(j).put("artId", downloadObjects.getJSONObject(j).getString("artId"));
                                }

                            }
                        }
                        if (!alreadyPlanned)
                            downloadObjects.put(downloadHistory.getJSONObject(i));

                    }

                }

                int reacquiredFilesCount = 0;
                for (int i = 0; i < downloadObjects.length(); i++) {

                    FileUtils.copyURLToFile(
                            new URL(downloadObjects.getJSONObject(i).getString("artUrl")),
                            new File(String.format("usr\\cached\\%s.jpg", downloadObjects.getJSONObject(i).getString("artId")))
                    );
                    reacquiredFilesCount++;

                }
                if (reacquiredFilesCount > 0) {
                    Debug.trace(
                            Thread.currentThread(),
                            String.format("Reacquired %s file%s to cache.", reacquiredFilesCount, reacquiredFilesCount == 1 ? "" : "s")
                    );

                    download.setDownloadHistory(downloadHistory);
                }

            } catch (JSONException | IOException e) {
                Debug.error(Thread.currentThread(), "Failed to get art for checking files to re-download.", e.getCause());
            }

        }, "cache-optimiser").start();
    }

    public static Model getInstance() {
        return instance;
    }

    public static class search {
        private BorderPane[] searchResults;
        private JSONArray searchResultsJson;

        public BorderPane[] getSearchResults(){
            return searchResults;
        }

        public void setSearchResults(BorderPane[] searchResults) {
            this.searchResults = searchResults;
        }

        public JSONArray getSearchResultsJson() {return searchResultsJson; }

        public void setSearchResultsJson(JSONArray searchResultsJson) {
            this.searchResultsJson = searchResultsJson;
        }
    }

    public static class download{
        private volatile JSONArray downloadQueue = new JSONArray();
        private volatile JSONObject downloadObject = new JSONObject();
        private JSONArray downloadHistory = new JSONArray();
        private final List<String> songReferences = Arrays.asList("mp3", "wav", "ogg", "aac");

        public download() {
            refreshDownloadHistory();
            Debug.trace(null, String.format("Found a download history of %s item%s.", downloadHistory.length(), downloadHistory.length() == 1 ? "" : "s"));
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

        public synchronized void updateDownloadQueue(JSONObject queueItem) {

            if (downloadQueue.length() == 0 && !downloadObject.has("songs")) {

                // No downloads in progress or in queue, hence start a new download thread.
                Debug.trace(null, "New download request received, queue is blank and hence will begin downloading...");

                // For processing downloads, the progress must be viewed as started to work
                try {
                    for (int i = 0; i < queueItem.getJSONArray("songs").length(); i++)
                        queueItem.getJSONArray("songs").getJSONObject(i).put("completed", false);
                } catch (JSONException e) {
                    Debug.error(null, "Error updating downloads queue.", e.getCause());
                }

                downloadObject = queueItem;
                new acquireDownloadFiles(downloadObject);

            } else {

                // Download already in progress, hence queue
                Debug.trace(null, String.format("New download request received, adding to queue in position %s.", Model.getInstance().download.getDownloadQueue().length()+1));
                downloadQueue.put(queueItem);

            }
        }

        public synchronized boolean downloadsAccessible() {

            // Should allow access to downloads if there is: download history or downloads in progress
            return downloadHistory.length() > 0 || downloadQueue.length() > 0 || downloadObject.has("metadata");

        }

        public void deleteHistory(JSONObject targetDeletion) {
            JSONArray newDownloadHistory = new JSONArray();

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
                setDownloadHistory(newDownloadHistory);
            } catch (IOException e) {
                Debug.error(null, "Error writing new download history.", e.getCause());
            }

        }

        protected synchronized void setDownloadHistory(JSONArray downloadHistory) throws IOException{

            ByteArrayInputStream fis = new ByteArrayInputStream(downloadHistory.toString().getBytes());
            GZIPOutputStream gzipOS = new GZIPOutputStream(
                    new FileOutputStream(
                            new File("usr\\json\\downloads.gz")
                    )
            );

            byte[] buffer = new byte[1024];
            int len;
            while((len=fis.read(buffer)) != -1)
                gzipOS.write(buffer, 0, len);

            gzipOS.close();
            fis.close();

            this.downloadHistory = downloadHistory;

        }

        private synchronized void refreshDownloadHistory() {
            try {
                GZIPInputStream gis = new GZIPInputStream(
                        new FileInputStream(
                                new File("usr\\json\\downloads.gz")
                        )
                );
                ByteArrayOutputStream fos = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int len;
                while((len = gis.read(buffer)) != -1)
                    fos.write(buffer, 0, len);

                fos.close();
                gis.close();

                this.downloadHistory = new JSONArray(fos.toString());

            } catch (IOException | JSONException e) {
                try {
                    if (!Files.exists(Paths.get("usr\\json\\downloads.gz")))
                        if (!new File("usr\\json\\downloads.gz").createNewFile())
                            throw new IOException();
                } catch (IOException er) {
                    Debug.warn(null, "Failed to create new downloads history file.");
                }
            }
        }

        private class acquireDownloadFiles implements Runnable {
            final Thread thread;
            final JSONObject downloadData;

            volatile JSONArray graphData = new JSONArray();
            volatile String percentComplete = "0%";
            volatile String eta = "Calculating...";
            volatile String downloadSpeed = "Calculating...";
            volatile String song = "";

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

                    try {

                        if (line.contains("%") && !line.contains("in")) {

                            // Getting ETA
                            eta = line.split("ETA")[1].strip();

                            // Getting download speed
                            downloadSpeed = line.split("at")[1].split("at")[0].split("ETA")[0].strip();

                            // Getting Progress
                            percentComplete = line.substring(11).split("%")[0].strip() + "%";


                            // Getting graph data
                            JSONObject lineGraphData = new JSONObject();
                            switch (downloadSpeed.replaceAll("[^A-Za-z]+", "")) {
                                case "MiBs":
                                    lineGraphData.put("speed", Double.parseDouble(downloadSpeed.replaceAll("[^\\d.]", "")) * 1024 * 1024);
                                    break;

                                case "KiBs":
                                    lineGraphData.put("speed", Double.parseDouble(downloadSpeed.replaceAll("[^\\d.]", "")) * 1024);
                                    break;

                                default:
                                    lineGraphData.put("speed", Double.parseDouble(downloadSpeed.replaceAll("[^\\d.]", "")));
                            }

                            // Playtime: (Complete Songs Playtime) + (Playtime of Current Song * Percent Complete)
                            lineGraphData.put("time", IntStream.of(Integer.parseInt(index)-1).mapToDouble(i -> {
                                try {
                                    return downloadObject.getJSONArray("songs").getJSONObject(i).getInt("playtime");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                return 0;
                            }).sum() + (Double.parseDouble(percentComplete.replaceAll("[^\\d.]", "")) / 100) * downloadObject.getJSONArray("songs").getJSONObject(Integer.parseInt(index)-1).getDouble("playtime"));

                            graphData.put(lineGraphData);
                        }

                    } catch (Exception e) {
                        Debug.warn(null, "[" + e.getClass() + "] Failed to process line: " + line);
                    }

                    // Sourcing name of downloaded file
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
                            Paths.get("usr\\cached\\" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg")
                    );
                } catch (JSONException ignored) {
                    Debug.warn(thread, "Failed to get JSON data to cache album art.");
                } catch (IOException ignored) {
                    Debug.warn(thread, "Failed to cache album art.");
                }

                // Download files
                JSONArray additionalHistory = new JSONArray();
                try {

                    for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {

                        song = downloadObject.getJSONArray("songs").getJSONObject(i).getString("title");

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

                        // Update internal referencing
                        downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", true);

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

                            additionalHistory.put(downloadHistory);

                        } catch (JSONException e) {
                            Debug.warn(thread, "Failed to generate JSON for download history result.");
                        }

                    }
                } catch (JSONException e) {
                    Debug.error(thread, "JSON Error when attempting to access songs to download.", e.getCause());
                }

                // Updating the history
                try {
                    for (int i = 0; i < additionalHistory.length(); i++)
                        downloadHistory.put(additionalHistory.getJSONObject(i));

                    setDownloadHistory(downloadHistory);

                } catch (JSONException | IOException e) {
                    Debug.error(Thread.currentThread(), "Failed to set new download history with current download.", e.getCause());
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

                        default:
                            Debug.error(Thread.currentThread(), "Unexpected value: " + Model.getInstance().settings.getSettingInt("save_album_art"), new IllegalStateException().getCause());
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

                            // Marking it now as primed to download
                            for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++)
                                downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", false);

                            // Starting the new download
                            new acquireDownloadFiles(downloadObject);

                            // Update queue
                            if (downloadQueue.length() > 1) {

                                // Move queued item to the model and respawn this thread.
                                JSONArray newQueue = new JSONArray();
                                for (int i = 1; i < downloadQueue.length(); i++)
                                    newQueue.put(downloadQueue.getJSONObject(i));

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
                defaultSettings = new JSONObject("{\"advanced_validation\": true, \"output_directory\":\"\",\"save_album_art\":0,\"music_format\":0, \"album_art\":true, \"album_title\":true, \"song_title\":true, \"artist\":true, \"year\":true, \"track\":true,\"dark_theme\":false, \"data_saver\":false}");
            } catch (JSONException ignored) {}

            // Load users actual settings
            try {
                settings = new JSONObject(new Scanner(new File("usr\\json\\config.json")).useDelimiter("\\Z").next());
            } catch (FileNotFoundException | JSONException ignored) {
                Debug.warn(null, "Failed to load user settings.");
                settings = defaultSettings;
                resetSettings();
            }

            // Load the version
            try {
                version = new JSONObject(new Scanner(new File("src/sample/app/meta.json")).useDelimiter("\\Z").next()).get("version").toString();
            } catch (JSONException | FileNotFoundException e) {
                Debug.warn(null, "Failed to locate version.");
                version = null;
            }

        }

        private void resetSettings() {

            try {

                FileWriter newConfig = new FileWriter("usr\\json\\config.json");
                newConfig.write(defaultSettings.toString());
                newConfig.close();

            } catch (IOException e) {

                // Attempt to see if this was due to folders not working or a system IO error
                if (resetDirectories())
                    resetSettings();
                else
                    Debug.error(null, "Failed to reset settings.", e.getCause());
            }
        }

        private boolean resetDirectories() {

            boolean wasUseful = false;

            // Checking for non existing folders
            if (!Files.exists(Paths.get("usr\\cached"))) {
                wasUseful = true;
                if (!new File("usr\\cached").mkdirs())
                    Debug.error(null, "Failed to create non existing directory: usr\\cached", null);
            }

            if (!Files.exists(Paths.get("usr\\json"))) {
                wasUseful = true;
                if (!new File("usr\\json").mkdirs())
                    Debug.error(null, "Failed to create non existing directory: usr\\json", null);
            }

            return wasUseful;
        }

        public void saveSettings(JSONObject settings) {
            try {
                FileWriter settingsFile = new FileWriter("usr/json/config.json");
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
}