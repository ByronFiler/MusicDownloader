package sample.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sample.utils.acquireDownloadFiles;
import sample.utils.debug;
import sample.utils.gzip;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class download{

    private volatile JSONArray downloadQueue = new JSONArray();
    private volatile JSONObject downloadObject = new JSONObject();
    private JSONArray downloadHistory = new JSONArray();

    public download() {
        refreshDownloadHistory();
        debug.trace(null, String.format("Found a download history of %s item%s.", downloadHistory.length(), downloadHistory.length() == 1 ? "" : "s"));
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
            debug.trace(null, "New download request received, queue is blank and hence will begin downloading...");

            // For processing downloads, the progress must be viewed as started to work
            try {
                for (int i = 0; i < queueItem.getJSONArray("songs").length(); i++)
                    queueItem.getJSONArray("songs").getJSONObject(i).put("completed", false);
            } catch (JSONException e) {
                debug.error(null, "Error updating downloads queue.", e);
            }

            downloadObject = queueItem;
            new acquireDownloadFiles();

        } else {

            // Download already in progress, hence queue
            debug.trace(null, String.format("New download request received, adding to queue in position %s.", getDownloadQueue().length()+1));
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
            for (int i = 0; i < downloadHistory.length(); i++)
                if (!downloadHistory.getJSONObject(i).toString().equals(targetDeletion.toString()))
                    newDownloadHistory.put(downloadHistory.getJSONObject(i));
        } catch (JSONException e) {
            debug.error(null, "Failed to validate download history to remove element.", e);
        }

        // Rewriting the new history
        try {
            setDownloadHistory(newDownloadHistory);
        } catch (IOException e) {
            debug.error(null, "Error writing new download history.", e);
        }

    }

    public synchronized void setDownloadHistory(JSONArray downloadHistory) throws IOException{
        gzip.compressData(new ByteArrayInputStream(downloadHistory.toString().getBytes()), new File(System.getenv("APPDATA") + "\\MusicDownloader\\json\\downloads.gz"));
        this.downloadHistory = downloadHistory;
    }

    public synchronized void setDownloadObject(JSONObject downloadObject) {
        this.downloadObject = downloadObject;
    }

    public synchronized void setDownloadQueue(JSONArray downloadQueue) {
        this.downloadQueue = downloadQueue;
    }

    private synchronized void refreshDownloadHistory() {

        try {

            this.downloadHistory = new JSONArray(
                    gzip.decompressFile(
                            new File(System.getenv("APPDATA") + "\\MusicDownloader\\json\\downloads.gz")
                    ).toString()
            );

        } catch (IOException | JSONException e) {
            try {
                if (!Files.exists(Paths.get(System.getenv("APPDATA") + "\\MusicDownloader\\json\\downloads.gz")))
                    if (!new File(System.getenv("APPDATA") + "\\MusicDownloader\\json\\downloads.gz").createNewFile())
                        throw new IOException();
            } catch (IOException er) {
                debug.warn(null, "Failed to create new downloads history file.");
            }
        }
    }

    /*
    private class acquireDownloadFiles implements Runnable {

        final Thread thread;
        final JSONObject downloadData;
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
                debug.warn(thread, "Error connecting to: " + sampleFileSource);
                // Handle reconnection
            }

            // Preparing sample: Converting to comparable wav format from mp3 & deleting old mp3
            try {
                new Converter().convert("smp.mp3", "smp.wav");
            } catch (Exception ignored) {} // Falsely throws an exception even on success

            if (!new File("smp.mp3").delete())
                debug.warn(thread, "Failed to delete : smp.mp3");

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
                debug.warn(thread, "File is too large to be checked.");
                return 1;
            }

            // Deleting temporary files
            if (!new File("dl.wav").delete())
                debug.warn(thread, "Failed to delete file: dl.wav");
            if (!new File("smp.wav").delete())
                debug.warn(thread, "Failed to delete file: smp.wav");

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
                            debug.error(thread, "Failed to create directory: " + folderRequest + "(" + i + ")", null);
                        }
                    }
                }
            } else {
                if (new File(folderRequest).mkdir())
                    return folderRequest;
                else {
                    debug.error(thread, "Failed to create directory: " + folderRequest, null);
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

                // Sourcing name of downloaded file
                if (line.contains("[ffmpeg]")) {
                    downloadedFile = line.substring(22);
                }

            }

            // Delete now useless bat
            if (!new File("exec.bat").delete())
                debug.warn(thread, "Failed to delete file: exec.bat");

            // Validate
            if (Model.getInstance().settings.getSettingBool("advanced_validation")) {
                float downloadValidity = evaluateDownloadValidity(
                        String.format("https://rovimusic.rovicorp.com/playback.mp3?c=%s=&f=I", song.getString("sample")),
                        downloadedFile
                );

                debug.trace(
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
                        debug.warn(thread, "Failed to delete: " + downloadedFile);
                    }

                    if (song.getJSONArray("source").length() > sourceDepth + 2) {
                        // Can continue and move onto the next source
                        downloadFile(song, format, sourceDepth + 1, index);
                        return;
                    } else {
                        debug.warn(thread, "Failed to find a song in sources that was found to be valid, inform user of failure.");
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
                            debug.error(thread, "Failed to delete file: " + downloadedFile, new IOException());
                        }

                    } catch (IOException | NotSupportedException e) {
                        e.printStackTrace();
                    }
                } catch (InvalidDataException | UnsupportedTagException e) {
                    debug.warn(thread, "Failed to apply meta data to: " + downloadedFile);
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
                debug.error(thread, "Failed to download album art.", e);
            } catch (JSONException ignored) {}

            // Cache the album art
            try {
                Files.copy(
                        Paths.get(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg"),
                        Paths.get(System.getenv("APPDATA") + "\\MusicDownloader\\cached\\" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg")
                );
            } catch (JSONException ignored) {
                debug.warn(thread, "Failed to get JSON data to cache album art.");
            } catch (IOException ignored) {
                debug.warn(thread, "Failed to cache album art.");
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
                            debug.error(thread, "Error downloading song: " + downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"), e);
                        } catch (JSONException er) {
                            debug.error(thread, "JSON Error downloading song", er);
                        }

                    }

                    // Update internal referencing
                    downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", true);

                    debug.trace(
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
                        debug.warn(thread, "Failed to generate JSON for download history result.");
                    }

                }
            } catch (JSONException e) {
                debug.error(thread, "JSON Error when attempting to access songs to download.", e);
            }

            // Updating the history
            try {
                for (int i = 0; i < additionalHistory.length(); i++)
                    downloadHistory.put(additionalHistory.getJSONObject(i));

                setDownloadHistory(downloadHistory);

            } catch (JSONException | IOException e) {
                debug.error(Thread.currentThread(), "Failed to set new download history with current download.", e);
            }

            // Check if we should delete the album art
            try {
                switch (Model.getInstance().settings.getSettingInt("save_album_art")) {

                    // Delete album art always
                    case 0:
                        if (!new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg").delete())
                            debug.warn(thread, "Failed to delete: " + downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg");
                        break;

                    // Delete for songs
                    case 1:
                        if (downloadObject.getJSONArray("songs").length() == 1)
                            if (!new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg").delete())
                                debug.warn(thread, "Failed to delete: " + downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg");
                        break;

                    // Delete for albums
                    case 2:
                        if (downloadObject.getJSONArray("songs").length() > 1)
                            if (!new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg").delete())
                                debug.warn(thread, "Failed to delete: " + downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg");
                        break;

                    default:
                        debug.error(Thread.currentThread(), "Unexpected value: " + Model.getInstance().settings.getSettingInt("save_album_art"), new IllegalStateException());
                }
            } catch (JSONException e) {
                debug.error(thread, "Failed to perform check to delete album art.", e);
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
                        debug.error(thread, "Failed to process queue.", e);
                    }

                } else {
                    downloadObject = new JSONObject();
                }
            });

        }

    }

     */
}