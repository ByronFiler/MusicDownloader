package musicdownloader.utils.io;

import com.mpatric.mp3agic.*;
import com.sun.nio.sctp.IllegalReceiveException;
import javazoom.jl.decoder.JavaLayerException;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.ui.Notification;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

/*
 todo

 - cancelling albums uses a deprecated method
 - cancelling songs doesn't really work (?)
 - cancelling album doesn't update the model to mark queued as active download

 - should really implement a safe termination method and await it's completion on the main thread via use of a flag

 - modern termination flag not properly implemented in downloads
 - termination flags untested in "Download" class
 - process termination untested in "Download" class to ProcessDebugger

// todo on cancelling songs, it seems the issue is on being redrawn, hence download items should have a cancelled flag in the model

// todo; should spawn a new thread to the model, not a new fucking thread from it self that's just dumb, v memory inefficent

 */

// Handles All Downloads From Queue
public class Downloader implements Runnable {

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("resources.locale.notification");

    private JSONObject downloadObject = Model.getInstance().download.getDownloadObject();
    private final JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
    private JSONArray downloadQueue = Model.getInstance().download.getDownloadQueue();

    private final Thread mainThread;
    private boolean[] allowedDownloads;
    private byte[] albumArt;

    private volatile boolean cancelDownload = false;

    public Downloader() {

        try {
            allowedDownloads = new boolean[downloadObject.getJSONArray("songs").length()];
            Arrays.fill(allowedDownloads, true);
        } catch (JSONException e) {
            Debug.error("Error getting songs to populate allowed downloads.", e);
        }

        mainThread = new Thread(this, "acquire-download-files");
        mainThread.start();
    }

    public void cancel(int songIndex) {

        if (songIndex == -1 || (IntStream.range(0, allowedDownloads.length).mapToObj(ind -> allowedDownloads[ind]).mapToInt(e -> e ? 1 : 0).sum() <= 1)) {

            mainThread.interrupt();
            cancelDownload = true;
            processQueue();

            // always clear temp
            // delete album art
            // delete folder

        } else allowedDownloads[songIndex] = false;

        Model.getInstance().download.markCancelled(songIndex);

    }

    private void processQueue() {

        if (downloadQueue.length() > 0) {
            try {
                Debug.trace(String.format("Found %s items left in queue processing and starting new download...", downloadQueue.length()));

                // Creating new download object by marking songs for downloads controller
                downloadObject = downloadQueue.getJSONObject(0);
                for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++)
                    downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", false);

                // Updating the model
                Model.getInstance().download.setDownloadObject(downloadObject);
                Model.getInstance().download.markCompletedDownload();

                // Updating the queue
                if (downloadQueue.length() == 1) downloadQueue = new JSONArray();
                else {
                    JSONArray newQueue = new JSONArray();
                    for (int i = 1; i < downloadQueue.length(); i++) newQueue.put(downloadQueue.get(i));
                    downloadQueue = newQueue;
                }
                Model.getInstance().download.setDownloadQueue(downloadQueue);

                // Notification
                if (Model.getInstance().view.getPrimaryStage().isIconified() || Model.getInstance().view.isStageClosed()) {
                    int length = Model.getInstance().download.getDownloadQueue().length();
                    new Notification(
                            String.format(resourceBundle.getString("downloadStartedMessage"), downloadObject.getJSONObject("metadata").getString("album")),
                            length == 0 ? "" : resourceBundle.getString(length == 1 ? "itemsLeftInQueueSingular" : "itemsLeftInQueuePlural"),
                            new File(downloadObject.getJSONObject("metadata").getString("directory")),
                            TrayIcon.MessageType.INFO
                    );
                }

                // Starting the new download
                new Downloader();

            } catch (JSONException e) {
                Debug.error("Failed to process queue.", e);
            }

        } else {

            // No items left in queue.
            if (Model.getInstance().view.getPrimaryStage().isIconified() || Model.getInstance().view.isStageClosed()) {
                new Notification(resourceBundle.getString("allDownloadsCompletedMessage"), "", null, TrayIcon.MessageType.INFO);
                System.exit(0);
            }

            Debug.trace("Found 0 items remaining in queue, waiting for next download to start.");
            Model.getInstance().download.setDownloadObject(new JSONObject());
            Model.getInstance().download.markCompletedDownload();
        }
    }

    private String generateFolder(String folderRequest) {

        if (Files.exists(Paths.get(folderRequest))) {

            int i = 1;
            while (true) {

                // File exists so move onto the next one
                if (Files.exists(Paths.get(folderRequest + "(" + i + ")"))) {
                    i++;
                } else {
                    if (new File(folderRequest + "(" + i + ")").mkdir()) {
                        return folderRequest + "(" + i + ")";
                    } else {
                        Debug.error("Failed to create directory: " + folderRequest + "(" + i + ")", null);
                    }
                }
            }

        } else {

            if (new File(folderRequest).mkdir()) {
                return folderRequest;
            } else {
                Debug.error("Failed to create directory: " + folderRequest, null);
            }

        }

        return "";

    }

    private byte[] readAlbumArt() throws JSONException {

        if (Files.exists(Paths.get(Resources.getInstance().getApplicationData() + String.format("cached/%s.jpg", downloadObject.getJSONObject("metadata").getString("artId"))))) {

            try {
                return Files.readAllBytes(
                        Paths.get(
                                Resources.getInstance().getApplicationData() + String.format(
                                        "cached/%s.jpg", downloadObject.getJSONObject("metadata").getString("artId")
                                )
                        )
                );
            } catch (IOException e) {
                Debug.error("Failed to read all bytes, album art was likely a corrupt download.", e);
            }
        } else {

            Debug.warn("Failed to use cached album art, should've already been in cache if downloading, reacquiring file.");

            while (true) {

                try {
                    FileUtils.copyURLToFile(
                            new URL(downloadObject.getJSONObject("metadata").getString("art")),
                            new File(Resources.getInstance().getApplicationData() + String.format("cached/%s.jpg", downloadObject.getJSONObject("metadata").getString("artId")))
                    );
                    return Files.readAllBytes(
                            Paths.get(
                                    Resources.getInstance().getApplicationData() + String.format("cached/%s.jpg", downloadObject.getJSONObject("metadata").getString("artId")
                                    )
                            )
                    );

                } catch (IOException e) {

                    try {
                        if (Model.getInstance().connectionWatcher.isOffline()) {
                            Model.getInstance().connectionWatcher.getLatch().await();
                        }
                    } catch (InterruptedException e1) {
                        Debug.error("Thread interrupted when awaiting for reconnection.", e);
                    }

                    Debug.warn("Failed to connect and download album art.");
                }
            }

        }

        return new byte[]{};
    }

    @Override
    public void run() {
        try {
            if (Model.getInstance().connectionWatcher.isOffline()) {
                Model.getInstance().connectionWatcher.getLatch().await();
            }

            try {
                Debug.trace(
                        String.format(
                                "Downloading %s by %s, %s item%s left in queue.",
                                downloadObject.getJSONObject("metadata").getString("album"),
                                downloadObject.getJSONObject("metadata").getString("artist"),
                                downloadQueue.length(),
                                downloadQueue.length() == 1 ? "" : "s"
                        )
                );
            } catch (JSONException e) {
                Debug.error("Failed to get download object or queue information to debug.", e);
            }


            // Making the folder to contain the downloads
            try {
                downloadObject
                        .getJSONObject("metadata")
                        .put(
                                "directory",
                                downloadObject.getJSONObject("metadata").getBoolean("is_album") ?
                                        generateFolder(downloadObject.getJSONObject("metadata").getString("directory"))
                                        : downloadObject.getJSONObject("metadata").getString("directory")
                        );
            } catch (JSONException e) {
                Debug.error("JSON Exception generating directory", e);
            }

            // Loading album art
            try {
                albumArt = readAlbumArt();
            } catch (JSONException e) {
                Debug.error("Failed to load JSON data when reading album art.", e);
            }

            if (cancelDownload) throw new InterruptedException();

        } catch (InterruptedException e) {

            try {
                Files.delete(Paths.get(downloadObject.getJSONObject("metadata").getString("directory")));
                Files.delete(Paths.get(
                        Resources.getInstance().getApplicationData() + String.format(
                                "cached/%s.jpg", downloadObject.getJSONObject("metadata").getString("artId")
                        )
                ));
            } catch (JSONException e1) {
                Debug.error("Failed to access json to delete album art & directory", e);
            } catch (IOException e1) {
                Debug.error("Failed to delete files after cancelled download.", e);
            }
        }

        // Download files
        JSONObject newHistory = new JSONObject();
        Download download;
        try {
            JSONArray songs = new JSONArray();

            for (int i = 0; i < downloadObject.getJSONArray("songs").length() && !cancelDownload; i++) {

                if (allowedDownloads[i]) {

                    // download start
                    download = new Download(downloadObject.getJSONArray("songs").getJSONObject(i));
                    download.getCompleted().await();

                    if (allowedDownloads[i]) {

                        // Update internal referencing
                        downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", true);
                        downloadObject.getJSONArray("songs").getJSONObject(i).put("downloadCompleted", Instant.now().toEpochMilli());

                        Model.getInstance().download.markCompletedSong(i);

                        try {
                            JSONObject newSongHistory = new JSONObject();
                            newSongHistory.put("title", downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"));
                            newSongHistory.put("id", downloadObject.getJSONArray("songs").getJSONObject(i).getString("id"));
                            newSongHistory.put("source", new JSONArray("[\"" + download.getSource() + "\"]"));
                            newSongHistory.put("position", downloadObject.getJSONArray("songs").getJSONObject(i).getInt("position"));

                            songs.put(newSongHistory);
                        } catch (IndexOutOfBoundsException e) {
                            Debug.error("Fatal error building history", e);
                        }

                        Debug.trace(
                                String.format(
                                        "Downloaded \"%s\" (%s of %s)",
                                        downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"),
                                        i + 1,
                                        downloadObject.getJSONArray("songs").length()
                                )
                        );

                    } else {

                        Debug.trace(
                                String.format(
                                        "Downloaded \"%s\" (%s of %s)",
                                        downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"),
                                        i + 1,
                                        downloadObject.getJSONArray("songs").length()
                                )
                        );

                    }

                } else {

                    Debug.trace(
                            String.format(
                                    "Skipped (Cancelled) \"%s\" (%s of %s)",
                                    downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"),
                                    i + 1,
                                    downloadObject.getJSONArray("songs").length()
                            )
                    );

                }

            }

            if (cancelDownload) return;

            newHistory.put("metadata", downloadObject.getJSONObject("metadata"));
            newHistory.put("songs", songs);

        } catch (JSONException e) {
            Debug.error("JSON Error when attempting to access songs to download.", e);

        } catch (InterruptedException e) {

            Debug.success("Got interrupt mid-download");

            return;

        }

        // Updating the history
        try {
            if (!cancelDownload) {
                downloadHistory.put(newHistory);
                Model.getInstance().download.setDownloadHistory(downloadHistory);
            }

        } catch (IOException e) {
            Debug.error("Failed to set new download history with current download.", e);
        }

        try {
            if (!cancelDownload) {
                switch (Model.getInstance().settings.getSettingInt("save_album_art")) {
                    // Delete album art always
                    case 0:
                        break;

                    // Keep For Albums
                    case 1:
                        if (downloadObject.getJSONArray("songs").length() > 1) {

                            if (Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "cached/" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg"))) {

                                FileUtils.copyFile(
                                        new File(Resources.getInstance().getApplicationData() + "cached/" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg"),
                                        new File(downloadObject.getJSONObject("metadata").getString("directory") + "/art.jpg")
                                );

                            } else {

                                Debug.warn("Failed to find cached file, using remote resource.");
                                while (true) {
                                    try {
                                        FileUtils.copyURLToFile(
                                                new URL(downloadObject.getJSONObject("metadata").getString("art")),
                                                new File(downloadObject.getJSONObject("metadata").getString("directory") + "/art.jpg")
                                        );
                                        break;
                                    } catch (IOException e) {

                                        try {
                                            if (Model.getInstance().connectionWatcher.isOffline()) {
                                                Model.getInstance().connectionWatcher.getLatch().await();
                                            }
                                        } catch (InterruptedException e1) {
                                            Debug.error("Thread interrupted while awaiting reconnection.", e);
                                        }

                                    }
                                }

                            }
                        }
                        break;

                    // Keep for songs
                    case 2:
                        if (downloadObject.getJSONArray("songs").length() == 1)
                            FileUtils.copyFile(
                                    new File(Resources.getInstance().getApplicationData() + "cached/" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg"),
                                    new File(downloadObject.getJSONObject("metadata").getString("directory") + "/art.jpg")
                            );
                        break;

                    // Keep always
                    case 3:
                        FileUtils.copyFile(
                                new File(Resources.getInstance().getApplicationData() + "cached/" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg"),
                                new File(downloadObject.getJSONObject("metadata").getString("directory") + "/art.jpg")
                        );
                        break;

                    default:
                        Debug.error("Unexpected value: " + Model.getInstance().settings.getSettingInt("save_album_art"), new IllegalStateException());
                }
            }
        } catch (JSONException e) {
            Debug.error("Failed to perform check to copy album art to download.", e);
        } catch (IOException e) {
            Debug.warn("Failed to copy album art into downloads folder.");
        }

        // Move onto the next item if necessary
        // todo queue processing should be completed by the model, this should callback to the model, excessive internal calls leads to in-efficent mem management
        processQueue();

    }

    // Handles: Downloading File & Post Processing => (Validation of File, Audio Correction & Application of Metadata)
    private class Download implements Runnable {

        // Inherits from downloads object
        private final JSONObject downloadObject = Downloader.this.downloadObject;

        // Finalised per download from Model
        private final boolean downloadValidation = Model.getInstance().settings.getSettingBool("advanced_validation");
        private final boolean volumeCorrection = Model.getInstance().settings.getSettingBool("volume_correction");
        private final String format = Resources.songReferences.get(Model.getInstance().settings.getSettingInt("music_format"));

        // Internal vars
        private int sourceDepth = 0;
        private boolean kill = false;
        private final CountDownLatch completed = new CountDownLatch(1);
        private String source;

        // Constructors
        private final JSONObject song;

        private final Thread downloadExecutor;

        public Download(JSONObject song) {
            this.song = song;

            downloadExecutor = new Thread(this, "download-song-thread");
            downloadExecutor.setDaemon(true);
            downloadExecutor.start();

        }

        @Override
        public void run() {

            // Something containing File, And Validity
            DownloadResultsData downloadsData = new DownloadResultsData();
            File bestFile = null;
            AudioAnalysis analysis = null;

            boolean running = true;
            while (running && !kill) {

                float compare = -1;
                File downloadedFile = null;

                try {

                    // Download
                    downloadedFile = downloadToDisk(song.getJSONArray("source").getString(sourceDepth));

                    if (Model.getInstance().settings.getSettingBool("advanced_validation") & !kill) {
                        try {
                            analysis = new AudioAnalysis(song.getString("sample"));
                            compare = analysis.compare(Objects.requireNonNull(downloadedFile).getAbsolutePath());
                        } catch (UnsupportedAudioFileException e) {
                            Debug.warn("Failed to convert sample");
                        }
                    }

                } catch (IOException e) {

                    // Networking issue
                    try {
                        if (Model.getInstance().connectionWatcher.isOffline()) {
                            Model.getInstance().connectionWatcher.getLatch().await();
                        }
                    } catch (InterruptedException e1) {
                        cancel();
                        return;
                    }

                } catch (IllegalReceiveException e) {

                    // Weird IO issue
                    Debug.error("Failed to download file", e);

                } catch (JSONException e) {

                    // Should never happen
                    e.printStackTrace();

                } catch (JavaLayerException e) {

                    Debug.warn("Unexpected exception while attempting to perform audio analysis on download.");

                }

                try {

                    if (downloadValidation && !kill) {

                        if (compare > 0.7) {

                            // Successful download
                            bestFile = Objects.requireNonNull(downloadedFile);
                            running = false;

                        } else if (sourceDepth == (song.getJSONArray("sources").length() - 1)) {

                            // Exhausted sources without finding a match, use best match
                            bestFile = downloadsData.getBestFile();
                            running = false;

                        } else {

                            downloadsData.add(song.getJSONArray("sources").getString(sourceDepth), compare, downloadedFile);
                            sourceDepth++;

                        }

                    } else {

                        bestFile = downloadedFile;
                        running = false;

                    }

                } catch (JSONException e) {
                    Debug.error("Failed to access sources.", e);
                }
            }

            // Loop Terminated
            try {

                try {
                    if (mainThread.isInterrupted() || bestFile == null) return;

                    // Best download found, correct volume
                    if (volumeCorrection) {
                        if (analysis == null)
                            analysis = new AudioAnalysis(String.format(Resources.mp3Source, song.getString("sample")));
                        analysis.correctAmplitude(Objects.requireNonNull(bestFile).getAbsolutePath());
                    }

                    // Apply-metadata
                    if (!mainThread.isInterrupted()) {
                        applyMeta(Objects.requireNonNull(bestFile));
                    }
                } catch (UnsupportedAudioFileException e) {

                    // cancel shouldn't happen often
                }

            } catch (IOException | InvalidDataException | UnsupportedTagException | JSONException | JavaLayerException e) {
                e.printStackTrace();
            } finally {
                completed.countDown();
            }
        }

        public void cancel() {

            // Terminates
            this.kill = true;

            // todo Send signal to process debugger to terminate

            try {
                completed.await();
            } catch (InterruptedException e) {
                Debug.error("Unexpected interruption while awaiting the termination of a download thread.", e);
            }

        }

        public CountDownLatch getCompleted() {
            return completed;
        }

        public String getSource() {
            return source;
        }

        private File downloadToDisk(String source) throws IllegalReceiveException {

            // Start download
            if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "temp")))
                if (!new File(Resources.getInstance().getApplicationData() + "temp").mkdirs())
                    Debug.error("Failed to create temp directory in music downloader app data.", new IOException());

            // Console won't always print out a accurate file name
            List<File> preexistingFiles = Arrays.asList(
                    Objects.requireNonNull(
                            new File(Resources.getInstance().getApplicationData() + "temp").listFiles()
                    )
            );

            ProcessBuilder builder = new ProcessBuilder(Resources.getInstance().getYoutubeDlExecutable());
            builder.command(
                    "youtube-dl",
                    "--extract-audio",
                    "--audio-format", format,
                    "--ignore-errors",
                    "--retries", "10",
                    source
            );
            builder.directory(new File(Resources.getInstance().getApplicationData() + "temp"));

            ProcessDebugger processDebugger = new ProcessDebugger(builder);
            try {
                processDebugger.getAwaiter().await();
            } catch (InterruptedException e) {
                processDebugger.kill();
                return null;
            }

            // May receive a kill signal in way of download, should hence just return null
            if (!kill) {

                ArrayList<File> currentFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(new File(Resources.getInstance().getApplicationData() + "temp").listFiles())));
                currentFiles.removeAll(preexistingFiles);

                if (currentFiles.size() == 0) {
                    long preTime = Instant.now().toEpochMilli();
                    while (Instant.now().toEpochMilli() - preTime < 200) {

                        ArrayList<File> downloadedFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(new File(Resources.getInstance().getApplicationData() + "temp").listFiles())));
                        downloadedFiles.removeAll(preexistingFiles);

                        if (downloadedFiles.size() == 1) return downloadedFiles.get(0);

                    }

                    throw new IllegalReceiveException();
                } else return currentFiles.get(0);

            } else return null;

        }

        private void applyMeta(File downloadedFile) throws InvalidDataException, IOException, UnsupportedTagException, JSONException {

            Mp3File mp3Applicator = new Mp3File(downloadedFile);

            ID3v2 id3v2tag = new ID3v24Tag();
            mp3Applicator.setId3v2Tag(id3v2tag);

            if (Model.getInstance().settings.getSettingBool("album_art")) {
                // Could break this up into mb loads
                id3v2tag.setAlbumImage(albumArt, "image/jpg");
            }

            // Applying remaining data
            if (Model.getInstance().settings.getSettingBool("album_title")) {
                id3v2tag.setTitle(song.getString("title"));
            }

            if (Model.getInstance().settings.getSettingBool("song_title")) {
                id3v2tag.setAlbum(downloadObject.getJSONObject("metadata").getString("album"));
            }

            if (Model.getInstance().settings.getSettingBool("artist")) {
                id3v2tag.setArtist(downloadObject.getJSONObject("metadata").getString("artist"));
                id3v2tag.setAlbumArtist(downloadObject.getJSONObject("metadata").getString("artist"));
            }

            if (Model.getInstance().settings.getSettingBool("year")) {
                id3v2tag.setYear(downloadObject.getJSONObject("metadata").getString("year"));
            }

            if (Model.getInstance().settings.getSettingBool("track")) {
                id3v2tag.setTrack(song.getString("position"));
            }

            try {
                // Check if already exists, remove special characters
                mp3Applicator.save(downloadObject.getJSONObject("metadata").getString("directory") + "/" + song.getString("title").replaceAll("[\u0000-\u001f<>:\"/\\\\|?*\u007f]+", "_") + "." + format);

                // Delete old file
                if (!downloadedFile.delete()) Debug.warn("Failed to delete file: " + downloadedFile.getAbsolutePath());

            } catch (IOException | NotSupportedException e) {
                Debug.warn("Failed to apply metadata.");
            }


        }

        private class DownloadResultsData {

            private final ArrayList<DownloadResultData> resultData = new ArrayList<>();
            private float greatestValidity = 0;
            private int greatestValidityIndex = -1;

            public void add(String source, float validity, File downloadedFile) {

                resultData.add(new DownloadResultData(validity, downloadedFile));

                if (validity > greatestValidity) {
                    Download.this.source = source;

                    greatestValidity = validity;
                    greatestValidityIndex = resultData.size() - 1;
                }

            }

            public File getBestFile() {
                return resultData.get(greatestValidityIndex).getDownloadedFile();
            }

            private class DownloadResultData {

                private final float validity;
                private final File downloadedFile;

                public DownloadResultData(float validity, File downloadedFile) {

                    this.validity = validity;
                    this.downloadedFile = downloadedFile;

                }

                public float getValidity() {
                    return validity;
                }

                public File getDownloadedFile() {
                    return downloadedFile;
                }
            }

        }

    }
}