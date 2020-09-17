package musicdownloader.utils.io;

import com.mpatric.mp3agic.*;
import javazoom.jl.decoder.JavaLayerException;
import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.ui.Notification;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.*;

/*
TODO
 Download history should include a file md5, creation time?
 When the window is closed the thread seems to hang and not continue to downlaod
 */

public class Downloader implements Runnable {

    private JSONObject downloadObject = Model.getInstance().download.getDownloadObject();
    private final JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
    private JSONArray downloadQueue = Model.getInstance().download.getDownloadQueue();

    private final ArrayList<Float> songsValidity = new ArrayList<>();
    private final ArrayList<String> sources = new ArrayList<>();

    private byte[] albumArt;
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("resources.locale.notification");

    public Downloader() {
        new Thread(this, "acquire-download-files").start();
    }

    private String generateFolder(String folderRequest) {

        if (Files.exists(Paths.get(folderRequest))) {
            int i = 1; // Looks better than Album (0)
            while (true) {

                // File exists so move onto the next one
                if (Files.exists(Paths.get(folderRequest + "(" + i + ")")))
                    i++;
                else {
                    if (new File(folderRequest + "(" + i + ")").mkdir())
                        return folderRequest + "(" + i + ")";
                    else {
                        Debug.error("Failed to create directory: " + folderRequest + "(" + i + ")", null);
                    }
                }
            }
        } else {
            if (new File(folderRequest).mkdir())
                return folderRequest;
            else {
                Debug.error("Failed to create directory: " + folderRequest, null);
            }
        }

        return "";

    }

    private synchronized void downloadFile(JSONObject song, String format, int sourceDepth, String index, boolean overrideSimilarity, SpectroAnalysis analysis) throws IOException, JSONException, JavaLayerException {

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
                song.getJSONArray("source").getString(sourceDepth)
        );
        builder.directory(new File(Resources.getInstance().getApplicationData() + "temp"));
        debugProcess(builder);

        // Silent debug to not spam console
        ArrayList<File> currentFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(new File(Resources.getInstance().getApplicationData() + "temp").listFiles())));
        currentFiles.removeAll(preexistingFiles);

        // TODO: attempt every 50ms until 200ms then just say it fucked up
        // TODO: Happens rarely, likely due to a youtube-dl renaming process, consider awaiting for maybe 50 or 100ms?
        final File[] downloadedFile = {null};
        if (currentFiles.size() != 1) {

            Debug.warn("Did not find the downloaded file, retrying...");
            final int[] originalRetries = {5};
            final int[] retries = new int[]{originalRetries[0]};
            int delay = 50;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {

                    List<File> x = Arrays.asList(Objects.requireNonNull(new File(Resources.getInstance().getApplicationData() + "temp").listFiles()));
                    x.removeAll(preexistingFiles);

                    if (x.size() == 1) {
                        Debug.trace("Located the downloaded file.");
                        downloadedFile[0] = x.get(0);
                        this.cancel();
                    } else {
                        retries[0]--;
                        Debug.warn(
                                String.format(
                                        "Failed to find the file after %sms, waiting %sms and trying again (%s tr%s remaining)",
                                        originalRetries[0] * delay,
                                        delay,
                                        retries[0],
                                        retries[0] == 1 ? "y" : "ies"
                                )
                        );
                    }

                    if (retries[0] == 0) {
                        Debug.error(
                                String.format("Expected 1 new file to have been created, %s found.", currentFiles.size()),
                                new IOException("Unexpected new files")
                        );
                    }

                }
            }, delay, delay);
        } else {
            downloadedFile[0] = currentFiles.get(0);
        }

        if (downloadedFile[0] == null) throw new IOException("Failed to find downloaded file.");

        // Validate
        if (Model.getInstance().settings.getSettingBool("advanced_validation") && song.get("sample") != JSONObject.NULL) {

            float downloadValidity = analysis.compare(downloadedFile[0].getAbsolutePath());

            JSONObject tempValidityLog = new JSONObject();
            tempValidityLog.put("song", song);
            tempValidityLog.put("validity", downloadValidity);

            songsValidity.add(downloadValidity);

            Debug.trace(
                    String.format(
                            "Calculated downloaded validity of %s at %2.2f [%s%s]",
                            song.getString("title"),
                            downloadValidity * 100,
                            downloadValidity > 0.7 ? "PASS" : "FAIL",
                            overrideSimilarity ? " OVERRODE" : ""
                    )
            );

            if (downloadValidity <= 0.7 && !overrideSimilarity) {

                if (!downloadedFile[0].delete()) Debug.warn("Failed to delete: " + downloadedFile[0].getName());

                if (song.getJSONArray("source").length() > sourceDepth + 2) {

                    // Can continue and move onto the next source
                    downloadFile(song, format, sourceDepth + 1, index, false, analysis);

                } else {
                    float highestValidity = Collections.max(songsValidity);
                    Debug.warn(
                            String.format(
                                    "Failed to find a song matching required similarity, defaulting to using the highest validity, which was %.2f",
                                    highestValidity
                            )
                    );

                    // Need to call the download function and override check to use the greatest value song
                    downloadFile(song, format, songsValidity.indexOf(highestValidity), index, true, analysis);
                }
                return;

            }

            analysis.correctAmplitude(downloadedFile[0].getAbsolutePath());
            sources.add(song.getJSONArray("source").getString(sourceDepth));
        } else sources.add(song.getJSONArray("source").getString(0));

        // Apply meta-data
        if (format.equals("mp3")) {

            try {
                Mp3File mp3Applicator = new Mp3File(downloadedFile[0]);

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
                    id3v2tag.setTrack(downloadObject.getJSONArray("songs").getJSONObject(Integer.parseInt(index) - 1).getString("position"));
                }

                try {
                    // Check if already exists, remove special characters
                    mp3Applicator.save(downloadObject.getJSONObject("metadata").getString("directory") + "/" + song.getString("title").replaceAll("[\u0000-\u001f<>:\"/\\\\|?*\u007f]+", "_") + "." + format);

                    // Delete old file
                    if (!downloadedFile[0].delete()) Debug.warn("Failed to delete file: " + downloadedFile[0]);

                } catch (IOException | NotSupportedException e) {
                    Debug.warn("Failed to apply metadata.");
                }
            } catch (InvalidDataException | UnsupportedTagException e) {
                Debug.warn("Failed to apply meta data to: " + downloadedFile[0]);
            }

        } else {

            // Just move & rename the file
            Files.move(
                    Paths.get(downloadedFile[0].getAbsolutePath()),
                    Paths.get(downloadObject.getJSONObject("metadata").getString("directory") + "/" + song.getString("title") + "." + format)
            );

        }

        songsValidity.clear();

    }

    public static void debugProcess(ProcessBuilder builder) throws IOException {
        builder.redirectErrorStream(true);
        Process process = builder.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = reader.readLine()) != null) Debug.log(line);
    }

    @Override
    public void run() {

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
                                generateFolder(downloadObject.getJSONObject("metadata").getString("directory")) : downloadObject.getJSONObject("metadata").getString("directory")
                    );
        } catch (JSONException ignored) {}

        // Loading album art
        try {
            if (Files.exists(Paths.get(Resources.getInstance().getApplicationData() + String.format("cached/%s.jpg", downloadObject.getJSONObject("metadata").getString("artId"))))) {

                try {
                    this.albumArt = Files.readAllBytes(Paths.get(Resources.getInstance().getApplicationData() + String.format("cached/%s.jpg", downloadObject.getJSONObject("metadata").getString("artId"))));
                } catch (IOException e) {
                    Debug.error("Failed to read all bytes, album art was likely a corrupt download.", e);
                }
            } else {

                Debug.warn("Failed to use cached album art, should've already been in cache if downloading, reacquiring file.");
                try {
                    FileUtils.copyURLToFile(
                            new URL(downloadObject.getJSONObject("metadata").getString("art")),
                            new File(Resources.getInstance().getApplicationData() + String.format("cached/%s.jpg", downloadObject.getJSONObject("metadata").getString("artId")))
                    );
                    this.albumArt = Files.readAllBytes(Paths.get(Resources.getInstance().getApplicationData() + String.format("cached/%s.jpg", downloadObject.getJSONObject("metadata").getString("artId"))));
                } catch (IOException e) {
                    Debug.error("Failed to connect and download album art.", e);
                    // TODO: Handle reconnection
                }

            }
        } catch (JSONException e) {
            Debug.error("Failed to load JSON data when reading album art.", e);
        }

        // Download files
        JSONObject newHistory = new JSONObject();
        try {

            // Preparing history data structure
            newHistory.put("metadata", downloadObject.getJSONObject("metadata"));
            newHistory.getJSONObject("metadata").put("format", Resources.songReferences.get(Model.getInstance().settings.getSettingInt("music_format")));
            JSONArray songs = new JSONArray();

            JSONObject downloadObject = Model.getInstance().download.getDownloadObject();
            downloadObject.put(
                    "metadata",
                    Model
                        .getInstance()
                        .download
                        .getDownloadObject()
                        .getJSONObject("metadata")
                        .put(
                                "downloadStarted",
                                Instant.now().toEpochMilli()
                        )
            );
            Model.getInstance().download.setDownloadObject(downloadObject);

            // Working the download
            for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {

                SpectroAnalysis analysis = null;
                if (downloadObject.getJSONArray("songs").getJSONObject(i).get("sample") != JSONObject.NULL) {
                    analysis = new SpectroAnalysis(String.format(Resources.mp3Source, downloadObject.getJSONArray("songs").getJSONObject(i).getString("sample")));
                }

                // Will call it's self recursively until it exhausts possible files or succeeds
                try {
                    downloadFile(
                            downloadObject.getJSONArray("songs").getJSONObject(i),
                            downloadObject.getJSONObject("metadata").getString("format"),
                            0,
                            String.valueOf(i+1),
                            false,
                            analysis
                    );
                } catch (IOException | JSONException e) {
                    try {
                        Debug.error("Error downloading song: " + downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"), e);
                    } catch (JSONException er) {
                        Debug.error("JSON Error downloading song", er);
                    }

                }

                // Update internal referencing
                downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", true);
                downloadObject.getJSONArray("songs").getJSONObject(i).put("downloadCompleted", Instant.now().toEpochMilli());

                Model.getInstance().download.markCompletedSong(i);

                Debug.trace(
                        String.format(
                                "Downloaded \"%s\" (%s of %s)",
                                downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"),
                                i+1,
                                downloadObject.getJSONArray("songs").length()
                        )
                );

                JSONObject newSongHistory = new JSONObject();
                newSongHistory.put("title", downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"));
                newSongHistory.put("id", downloadObject.getJSONArray("songs").getJSONObject(i).getString("id"));

                newSongHistory.put("source", new JSONArray("[\"" + sources.get(i) + "\"]"));
                newSongHistory.put("position", downloadObject.getJSONArray("songs").getJSONObject(i).getInt("position"));

                songs.put(newSongHistory);
            }

            newHistory.put("songs", songs);

        } catch (JSONException e) {
            Debug.error("JSON Error when attempting to access songs to download.", e);
        } catch (IOException | JavaLayerException e) {
            e.printStackTrace();
        }

        // Updating the history
        try {
            downloadHistory.put(newHistory);
            Model.getInstance().download.setDownloadHistory(downloadHistory);

        } catch (IOException e) {
            Debug.error("Failed to set new download history with current download.", e);
        }

        try {
            switch (Model.getInstance().settings.getSettingInt("save_album_art")) {

                // Delete album art always
                case 0:
                    break;

                // Keep For Albums
                case 1:
                    if (downloadObject.getJSONArray("songs").length() > 1)
                        FileUtils.copyFile(
                                new File(Resources.getInstance().getApplicationData() + "cached/" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg"),
                                new File(downloadObject.getJSONObject("metadata").getString("directory") + "/art.jpg")
                        );
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
        } catch (JSONException e) {
            Debug.error("Failed to perform check to copy album art to download.", e);
        } catch (IOException e) {
            Debug.warn("Failed to copy album art into downloads folder.");
        }

        // Move onto the next item if necessary
        if (downloadQueue.length() > 0) {
            try {
                Debug.trace(String.format("Found %s items left in queue processing and starting new download...", downloadQueue.length()));

                // Creating new download object by marking songs for downloads controller
                downloadObject = downloadQueue.getJSONObject(0);
                for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", false);

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
                if (Model.getInstance().getPrimaryStage().isIconified() || Model.getInstance().isStageClosed()) {
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
            if (Model.getInstance().getPrimaryStage().isIconified() || Model.getInstance().isStageClosed()) {
                new Notification(resourceBundle.getString("allDownloadsCompletedMessage"), "", null, TrayIcon.MessageType.INFO);
                System.exit(0);
            }

            Debug.trace("Found 0 items remaining in queue, waiting for next download to start.");
            Model.getInstance().download.setDownloadObject(new JSONObject());
            Model.getInstance().download.markCompletedDownload();
        }
    }
}