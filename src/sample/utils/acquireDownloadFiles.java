package sample.utils;

import com.mpatric.mp3agic.*;
import com.musicg.fingerprint.FingerprintManager;
import com.musicg.fingerprint.FingerprintSimilarityComputer;
import com.musicg.wave.Wave;
import javafx.application.Platform;
import javazoom.jl.converter.Converter;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sample.model.Model;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class acquireDownloadFiles implements Runnable {

    private final JSONObject downloadObject = Model.getInstance().download.getDownloadObject();
    private final JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
    private final JSONArray downloadQueue = Model.getInstance().download.getDownloadQueue();

    public acquireDownloadFiles() {
        new Thread(this, "acquire-download-files").start();
    }

    // TODO: Fix and write tests
    private float evaluateDownloadValidity (String sampleFileSource, String downloadedFile) {

        // Preparing sample: Downloading sample
        try {
            FileUtils.copyURLToFile(
                    new URL(sampleFileSource),
                    new File("smp.mp3")
            );
        } catch (IOException e) {
            debug.warn(Thread.currentThread(), "Error connecting to: " + sampleFileSource);
            // Handle reconnection
        }

        // Preparing sample: Converting to comparable wav format from mp3 & deleting old mp3
        try {
            new Converter().convert("smp.mp3", "smp.wav");
        } catch (Exception ignored) {} // Falsely throws an exception even on success

        if (!new File("smp.mp3").delete())
            debug.warn(Thread.currentThread(), "Failed to delete : smp.mp3");

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
            debug.warn(Thread.currentThread(), "File is too large to be checked.");
            return 1;
        }

        // Deleting temporary files
        if (!new File("dl.wav").delete())
            debug.warn(Thread.currentThread(), "Failed to delete file: dl.wav");
        if (!new File("smp.wav").delete())
            debug.warn(Thread.currentThread(), "Failed to delete file: smp.wav");

        FingerprintSimilarityComputer fingerprint = new FingerprintSimilarityComputer(sampleData, downloadData);
        return fingerprint.getFingerprintsSimilarity().getScore();
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
                        debug.error(Thread.currentThread(), "Failed to create directory: " + folderRequest + "(" + i + ")", null);
                    }
                }
            }
        } else {
            if (new File(folderRequest).mkdir())
                return folderRequest;
            else {
                debug.error(Thread.currentThread(), "Failed to create directory: " + folderRequest, null);
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

        // Console won't always print out a accurate file name

        List<File> preexistingFiles = Arrays.asList(
                Objects.requireNonNull(
                        new File(System.getProperty("user.dir"))
                                .listFiles()
                )
        );

        String line;
        while ((line = reader.readLine()) != null)
            debug.log(Thread.currentThread(), line);


        // Silent debug to not spam console

        ArrayList<File> currentFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(new File(System.getProperty("user.dir")).listFiles())));
        currentFiles.removeAll(preexistingFiles);

        if (currentFiles.size() != 1)
            debug.error(
                    Thread.currentThread(),
                    String.format(
                            "Expected 1 new files to have been created %s found, specifically %s.",
                            currentFiles.size(),
                            String.join(",", currentFiles.stream().map((File::getName)).toString())
                    ),
                    new IllegalArgumentException("Unexpected new files")
            );

        File downloadedFile = currentFiles.get(0);

        if (downloadedFile == null)
            throw new IOException("Failed to find downloaded file.");

        // Delete now useless bat
        if (!new File("exec.bat").delete())
            debug.warn(Thread.currentThread(), "Failed to delete file: exec.bat");

        // Validate
        if (Model.getInstance().settings.getSettingBool("advanced_validation")) {

            float downloadValidity = evaluateDownloadValidity(
                    String.format("https://rovimusic.rovicorp.com/playback.mp3?c=%s=&f=I", song.getString("sample")),
                    downloadedFile.getName()
            );

            debug.trace(
                    Thread.currentThread(),
                    String.format(
                            "Calculated downloaded validity of %s at %2.2f [%s]",
                            song.getString("title"),
                            downloadValidity * 100,
                            downloadValidity > 0.7 ? "PASS" : "FAIL"
                    )
            );

            if (downloadValidity <= 0.7) {

                // Delete downloaded files & sample
                if (!downloadedFile.delete())
                    debug.warn(Thread.currentThread(), "Failed to delete: " + downloadedFile.getName());

                if (song.getJSONArray("source").length() > sourceDepth + 2) {

                    // Can continue and move onto the next source
                    downloadFile(song, format, sourceDepth + 1, index);
                    return;

                } else
                    debug.warn(Thread.currentThread(), "Failed to find a song in sources that was found to be valid, inform user of failure.");

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
                    // Check if already exists, remove special characters

                    mp3Applicator.save(downloadObject.getJSONObject("metadata").getString("directory") + "\\" + song.getString("title").replaceAll("[\u0000-\u001f<>:\"/\\\\|?*\u007f]+", "_") + "." + format);

                    // Delete old file
                    if (!downloadedFile.delete()) {
                        debug.error(Thread.currentThread(), "Failed to delete file: " + downloadedFile, new IOException());
                    }

                } catch (IOException | NotSupportedException e) {
                    e.printStackTrace();
                }
            } catch (InvalidDataException | UnsupportedTagException e) {
                debug.warn(Thread.currentThread(), "Failed to apply meta data to: " + downloadedFile);
            }

        } else {

            // Just move & rename the file
            Files.move(
                    Paths.get(downloadedFile.getAbsolutePath()),
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

        // TODO: Should be cached instantly then moved from the cache

        // Download the album art
        try {
            FileUtils.copyURLToFile(
                    new URL(downloadObject.getJSONObject("metadata").getString("art")),
                    new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg")
            );
        } catch (IOException e) {
            debug.error(Thread.currentThread(), "Failed to download album art.", e);
        } catch (JSONException ignored) {}

        // Cache the album art
        try {
            Files.copy(
                    Paths.get(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg"),
                    Paths.get(System.getenv("APPDATA") + "\\MusicDownloader\\cached\\" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg")
            );
        } catch (JSONException ignored) {
            debug.warn(Thread.currentThread(), "Failed to get JSON data to cache album art.");
        } catch (IOException ignored) {
            debug.warn(Thread.currentThread(), "Failed to cache album art.");
        }

        // Download files
        JSONObject newHistory = new JSONObject();
        try {

            // Preparing history data structure
            newHistory.put("metadata", downloadObject.getJSONObject("metadata"));
            JSONArray songs = new JSONArray();

            // Working the download
            for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++) {

                // Will call it's self recursively until it exhausts possible files or succeeds
                try {
                    downloadFile(
                            downloadObject.getJSONArray("songs").getJSONObject(i),
                            resources.songReferences.get(Model.getInstance().settings.getSettingInt("music_format")),
                            0,
                            String.valueOf(i+1)
                    );
                } catch (IOException | JSONException e) {
                    try {
                        debug.error(Thread.currentThread(), "Error downloading song: " + downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"), e);
                    } catch (JSONException er) {
                        debug.error(Thread.currentThread(), "JSON Error downloading song", er);
                    }

                }

                // Update internal referencing
                downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", true);

                debug.trace(
                        Thread.currentThread(),
                        String.format(
                                "Successfully downloaded \"%s\" (%s of %s)",
                                downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"),
                                i+1,
                                downloadObject.getJSONArray("songs").length()
                        )
                );

                JSONObject newSongHistory = new JSONObject();
                newSongHistory.put("title", downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"));
                newSongHistory.put("id", downloadObject.getJSONArray("songs").getJSONObject(i).getString("id"));

                songs.put(newSongHistory);

            }

            newHistory.put("songs", songs);


        } catch (JSONException e) {
            debug.error(Thread.currentThread(), "JSON Error when attempting to access songs to download.", e);
        }

        // Updating the history
        try {
            downloadHistory.put(newHistory);
            Model.getInstance().download.setDownloadHistory(downloadHistory);
        } catch (IOException e) {
            debug.error(Thread.currentThread(), "Failed to set new download history with current download.", e);
        }

        // Check if we should delete the album art
        try {
            switch (Model.getInstance().settings.getSettingInt("save_album_art")) {

                // Delete album art always
                case 0:
                    if (!new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg").delete())
                        debug.warn(Thread.currentThread(), "Failed to delete: " + downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg");
                    break;

                // Delete for songs
                case 1:
                    if (downloadObject.getJSONArray("songs").length() == 1)
                        if (!new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg").delete())
                            debug.warn(Thread.currentThread(), "Failed to delete: " + downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg");
                    break;

                // Delete for albums
                case 2:
                    if (downloadObject.getJSONArray("songs").length() > 1)
                        if (!new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg").delete())
                            debug.warn(Thread.currentThread(), "Failed to delete: " + downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg");
                    break;

                default:
                    debug.error(Thread.currentThread(), "Unexpected value: " + Model.getInstance().settings.getSettingInt("save_album_art"), new IllegalStateException());
            }
        } catch (JSONException e) {
            debug.error(Thread.currentThread(), "Failed to perform check to delete album art.", e);
        }

        // Move onto the next item if necessary
        Platform.runLater(() -> {
            if (downloadQueue.length() > 0) {
                try {

                    // Spawning new thread
                    Model.getInstance().download.setDownloadObject(downloadQueue.getJSONObject(0));

                    // Marking it now as primed to download
                    for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++)
                        downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", false);

                    Model.getInstance().download.setDownloadObject(downloadObject);

                    // Starting the new download
                    new acquireDownloadFiles();

                    // Update queue
                    if (downloadQueue.length() > 1) {

                        // Move queued item to the model and respawn this thread.
                        JSONArray newQueue = new JSONArray();
                        for (int i = 1; i < downloadQueue.length(); i++)
                            newQueue.put(downloadQueue.getJSONObject(i));

                        Model.getInstance().download.setDownloadQueue(newQueue);

                    } else {

                        // Queue had only one item, hence has now been cleared
                        Model.getInstance().download.setDownloadQueue(new JSONArray());

                    }

                } catch (JSONException e) {
                    debug.error(Thread.currentThread(), "Failed to process queue.", e);
                }

            } else {
                Model.getInstance().download.setDownloadObject(new JSONObject());
            }
        });

    }
}