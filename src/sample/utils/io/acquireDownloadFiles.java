package sample.utils.io;

import com.mpatric.mp3agic.*;
import com.musicg.fingerprint.FingerprintManager;
import com.musicg.fingerprint.FingerprintSimilarityComputer;
import com.musicg.wave.Wave;
import javafx.application.Platform;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sample.model.Model;
import sample.utils.app.debug;
import sample.utils.app.resources;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/*
TODO
 Album art should be cached instantly when added to queue, not here (due to queued items that aren't being downloaded requiring web requests in downloads client)
 Album art file shouldn't be moved to the download file until after the songs have been loaded, apply to metadata via byte stream or some such
 Then instead of deleting album art options, write to disk, this minimises fileIO and redundant web requests
 Download history should include a file md5, creation time, and source, and meta could include a link to the allmusic source?
 */

public class acquireDownloadFiles implements Runnable {

    private JSONObject downloadObject = Model.getInstance().download.getDownloadObject();
    private final JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
    private JSONArray downloadQueue = Model.getInstance().download.getDownloadQueue();

    public acquireDownloadFiles() {
        new Thread(this, "acquire-download-files").start();
    }

    private float evaluateDownloadValidity (String sampleFileSource, String downloadedFile) {

        if (!Files.exists(Paths.get(resources.applicationData + "temp")))
            if (!new File(resources.applicationData + "temp").mkdirs())
                debug.error(Thread.currentThread(), "Failed to create temp file directory for validation.", new IOException());

        // Preparing sample: Downloading sample & Converting
        InputStream mp3Source = null;
        try {
            mp3Source = new URL(sampleFileSource).openStream();
            new Converter().convert(
                    mp3Source,
                    resources.applicationData + "temp\\sample.wav",
                    null,
                    null
            );
            new Converter().convert(
                    downloadedFile,
                    resources.applicationData + "temp\\source.wav"
            );

        } catch (FileNotFoundException e) {
            debug.warn(Thread.currentThread(), "Failed to find file to run check on.");
        } catch (IOException e) {
            debug.warn(Thread.currentThread(), "Error connecting to: " + sampleFileSource);
            e.printStackTrace();
            System.exit(-1);
        } catch (JavaLayerException e) {
            debug.warn(Thread.currentThread(), "Error processing given data for conversion.");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (mp3Source != null) mp3Source.close();
            } catch (IOException e) {
                debug.error(Thread.currentThread(), "Failed to close remote sample source stream.", e);
            }
        }

        // Checking if the download file needs to be converted
        byte[] sampleData;
        byte[] downloadData;

        try {
            downloadData = new FingerprintManager().extractFingerprint(new Wave(resources.applicationData + "temp\\source.wav"));
            sampleData = new FingerprintManager().extractFingerprint(new Wave(resources.applicationData + "temp\\sample.wav"));
        } catch (ArrayIndexOutOfBoundsException ignored) {
            debug.warn(Thread.currentThread(), "File is too large to be checked.");
            return 1;
        }

        // Deleting temporary files
        if (!new File(resources.applicationData + "temp\\source.wav").delete())
            debug.warn(Thread.currentThread(), "Failed to delete source.wav");
        if (!new File(resources.applicationData + "temp\\sample.wav").delete())
            debug.warn(Thread.currentThread(), "Failed to delete sample.wav");

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
        if (!Files.exists(Paths.get(resources.applicationData + "temp")))
            if (!new File(resources.applicationData + "temp").mkdirs())
                debug.error(Thread.currentThread(), "Failed to create temp directory in music downloader app data.", new IOException());

        // Console won't always print out a accurate file name
        List<File> preexistingFiles = Arrays.asList(
                Objects.requireNonNull(
                        new File(resources.applicationData + "temp").listFiles()
                )
        );

        ProcessBuilder builder = new ProcessBuilder("C:\\Program Files (x86)\\youtube-dl\\youtube-dl.exe");
        builder.command("youtube-dl", "--extract-audio", "--audio-format", format, "--ignore-errors", "--retries", "10", "https://www.youtube.com/watch?v=" + song.getJSONArray("source").getString(sourceDepth));
        builder.directory(new File(resources.applicationData + "temp"));
        builder.redirectErrorStream(true);
        Process process = builder.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = reader.readLine()) != null)
            debug.log(Thread.currentThread(), line);

        // Silent debug to not spam console
        ArrayList<File> currentFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(new File(resources.applicationData + "temp\\").listFiles())));
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

        // Validate
        if (Model.getInstance().settings.getSettingBool("advanced_validation")) {

            float downloadValidity = evaluateDownloadValidity(
                    String.format("https://rovimusic.rovicorp.com/playback.mp3?c=%s=&f=I", song.getString("sample")),
                    downloadedFile.getAbsolutePath()
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
                    id3v2tag.setTitle(song.getString("title"));

                if (Model.getInstance().settings.getSettingBool("song_title"))
                    id3v2tag.setAlbum(downloadObject.getJSONObject("metadata").getString("album"));

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

        try {
            debug.trace(
                    Thread.currentThread(),
                    String.format(
                            "Downloading %s by %s, %s item%s left in queue.",
                            downloadObject.getJSONObject("metadata").getString("album"),
                            downloadObject.getJSONObject("metadata").getString("artist"),
                            downloadQueue.length(),
                            downloadQueue.length() == 1 ? "" : "s"
                    )
            );
        } catch (JSONException e) {
            debug.error(Thread.currentThread(), "Failed to get download object or queue information to debug.", e);
        }

        // Making the folder to contain the downloads
        try {
            downloadObject
                    .getJSONObject("metadata")
                    .put(
                            "directory",
                            generateFolder(
                                    downloadObject
                                            .getJSONObject("metadata")
                                            .getString("directory")
                            )
                    );
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

        // TODO: Should be done when adding to queue, cache the album art
        try {
            Files.copy(
                    Paths.get(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg"),
                    Paths.get(resources.applicationData + "cached\\" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg")
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
                                "Downloaded \"%s\" (%s of %s)",
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

                    debug.trace(
                            Thread.currentThread(),
                            String.format(
                                "Found %s items left in queue processing and starting new download...",
                                    downloadQueue.length()
                            )
                    );

                    // Creating new download object by marking songs for downloads controller
                    downloadObject = downloadQueue.getJSONObject(0);
                    for (int i = 0; i < downloadObject.getJSONArray("songs").length(); i++)
                        downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", false);

                    // Updating the model
                    Model.getInstance().download.setDownloadObject(downloadObject);

                    // Updating the queue
                    if (downloadQueue.length() == 1)
                        downloadQueue = new JSONArray();

                    else {

                        JSONArray newQueue = new JSONArray();
                        for (int i = 1; i < newQueue.length(); i++)
                            newQueue.put(downloadQueue.get(i));

                        downloadQueue = newQueue;
                    }
                    Model.getInstance().download.setDownloadQueue(downloadQueue);

                    // Starting the new download
                    new acquireDownloadFiles();

                } catch (JSONException e) {
                    debug.error(Thread.currentThread(), "Failed to process queue.", e);
                }

            } else {
                debug.trace(Thread.currentThread(), "Found 0 items remaining in queue, waiting for next download to start.");
                Model.getInstance().download.setDownloadObject(new JSONObject());
            }
        });

    }
}