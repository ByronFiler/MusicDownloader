package MusicDownloader.utils.io;

import MusicDownloader.model.Model;
import MusicDownloader.utils.app.debug;
import MusicDownloader.utils.app.resources;
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

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/*
TODO
 Download history should include a file md5, creation time, and source, and meta could include a link to the allmusic source?
 */

public class acquireDownloadFiles implements Runnable {

    private JSONObject downloadObject = Model.getInstance().download.getDownloadObject();
    private final JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
    private JSONArray downloadQueue = Model.getInstance().download.getDownloadQueue();

    private byte[] albumArt;

    public acquireDownloadFiles() {
        new Thread(this, "acquire-download-files").start();
    }

    private float evaluateDownloadValidity (String sampleFileSource, String downloadedFile) {

        if (!Files.exists(Paths.get(resources.getInstance().getApplicationData() + "temp")))
            if (!new File(resources.getInstance().getApplicationData() + "temp").mkdirs())
                debug.error("Failed to create temp file directory for validation.", new IOException());

        // Preparing sample: Downloading sample & Converting
        InputStream mp3Source = null;
        try {
            mp3Source = new URL(sampleFileSource).openStream();
            new Converter().convert(
                    mp3Source,
                    resources.getInstance().getApplicationData() + "temp/sample.wav",
                    null,
                    null
            );
            new Converter().convert(
                    downloadedFile,
                    resources.getInstance().getApplicationData() + "temp/source.wav"
            );

        } catch (FileNotFoundException e) {
            debug.warn("Failed to find file to run check on.");
        } catch (IOException e) {
            debug.warn("Error connecting to: " + sampleFileSource);
            e.printStackTrace();
            System.exit(-1);
        } catch (JavaLayerException e) {
            debug.warn("Error processing given data for conversion.");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (mp3Source != null) mp3Source.close();
            } catch (IOException e) {
                debug.error("Failed to close remote sample source stream.", e);
            }
        }

        // Checking if the download file needs to be converted
        byte[] sampleData;
        byte[] downloadData;

        try {
            downloadData = new FingerprintManager().extractFingerprint(new Wave(resources.getInstance().getApplicationData() + "temp/source.wav"));
            sampleData = new FingerprintManager().extractFingerprint(new Wave(resources.getInstance().getApplicationData() + "temp/sample.wav"));
        } catch (ArrayIndexOutOfBoundsException ignored) {
            debug.warn("File is too large to be checked.");
            return 1;
        }

        // Deleting temporary files
        if (!new File(resources.getInstance().getApplicationData() + "temp/source.wav").delete())
            debug.warn("Failed to delete source.wav");
        if (!new File(resources.getInstance().getApplicationData() + "temp/sample.wav").delete())
            debug.warn("Failed to delete sample.wav");

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
                        debug.error("Failed to create directory: " + folderRequest + "(" + i + ")", null);
                    }
                }
            }
        } else {
            if (new File(folderRequest).mkdir())
                return folderRequest;
            else {
                debug.error("Failed to create directory: " + folderRequest, null);
            }
        }

        return "";

    }

    private synchronized void downloadFile(JSONObject song, String format, int sourceDepth, String index) throws IOException, JSONException {

        // Start download
        if (!Files.exists(Paths.get(resources.getInstance().getApplicationData() + "temp")))
            if (!new File(resources.getInstance().getApplicationData() + "temp").mkdirs())
                debug.error("Failed to create temp directory in music downloader app data.", new IOException());

        // Console won't always print out a accurate file name
        List<File> preexistingFiles = Arrays.asList(
                Objects.requireNonNull(
                        new File(resources.getInstance().getApplicationData() + "temp").listFiles()
                )
        );

        ProcessBuilder builder = new ProcessBuilder("C:\\Program Files (x86)/youtube-dl/youtube-dl.exe");
        builder.command("youtube-dl", "--extract-audio", "--audio-format", format, "--ignore-errors", "--retries", "10", "https://www.youtube.com/watch?v=" + song.getJSONArray("source").getString(sourceDepth));
        builder.directory(new File(resources.getInstance().getApplicationData() + "temp"));
        builder.redirectErrorStream(true);
        Process process = builder.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = reader.readLine()) != null)
            debug.log(Thread.currentThread(), line);

        // Silent debug to not spam console
        ArrayList<File> currentFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(new File(resources.getInstance().getApplicationData() + "temp\\").listFiles())));
        currentFiles.removeAll(preexistingFiles);

        // TODO: Happens rarely, likely due to a youtube-dl renaming process, consider awaiting for maybe 50 or 100ms?
        if (currentFiles.size() != 1)
            debug.error(
                    String.format(
                            "Expected 1 new file to have been created, %s found.", currentFiles.size()
                    ),
                    new IllegalArgumentException("Unexpected new files")
            );

        File downloadedFile = currentFiles.get(0);

        if (downloadedFile == null) throw new IOException("Failed to find downloaded file.");

        // Validate
        if (Model.getInstance().settings.getSettingBool("advanced_validation")) {

            float downloadValidity = evaluateDownloadValidity(
                    String.format("https://rovimusic.rovicorp.com/playback.mp3?c=%s=&f=I", song.getString("sample")),
                    downloadedFile.getAbsolutePath()
            );

            debug.trace(
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
                    debug.warn("Failed to delete: " + downloadedFile.getName());

                if (song.getJSONArray("source").length() > sourceDepth + 2) {

                    // Can continue and move onto the next source
                    downloadFile(song, format, sourceDepth + 1, index);
                    return;

                } else
                    debug.warn("Failed to find a song in sources that was found to be valid, inform user of failure.");

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
                    id3v2tag.setAlbumImage(albumArt, "image/jpg");
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
                        debug.error("Failed to delete file: " + downloadedFile, new IOException());
                    }

                } catch (IOException | NotSupportedException e) {
                    e.printStackTrace();
                }
            } catch (InvalidDataException | UnsupportedTagException e) {
                debug.warn("Failed to apply meta data to: " + downloadedFile);
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
                    String.format(
                            "Downloading %s by %s, %s item%s left in queue.",
                            downloadObject.getJSONObject("metadata").getString("album"),
                            downloadObject.getJSONObject("metadata").getString("artist"),
                            downloadQueue.length(),
                            downloadQueue.length() == 1 ? "" : "s"
                    )
            );
        } catch (JSONException e) {
            debug.error("Failed to get download object or queue information to debug.", e);
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

        // Loading album art
        try {
            if (Files.exists(Paths.get(resources.getInstance().getApplicationData() + String.format("cached\\%s.jpg", downloadObject.getJSONObject("metadata").getString("artId"))))) {

                try {
                    this.albumArt = Files.readAllBytes(Paths.get(resources.getInstance().getApplicationData() + String.format("cached\\%s.jpg", downloadObject.getJSONObject("metadata").getString("artId"))));
                } catch (IOException e) {
                    debug.error("Failed to read all bytes, album art was likely a corrupt download.", e);
                }
            } else {

                debug.warn("Failed to use cached album art, should've already been in cache if downloading, reacquiring file.");
                try {
                    FileUtils.copyURLToFile(
                            new URL(downloadObject.getJSONObject("metadata").getString("art")),
                            new File(resources.getInstance().getApplicationData() + String.format("cached\\%s.jpg", downloadObject.getJSONObject("metadata").getString("artId")))
                    );
                    this.albumArt = Files.readAllBytes(Paths.get(resources.getInstance().getApplicationData() + String.format("cached\\%s.jpg", downloadObject.getJSONObject("metadata").getString("artId"))));
                } catch (IOException e) {
                    debug.error("Failed to connect and download album art.", e);
                    // TODO: Handle reconnection
                }

            }
        } catch (JSONException e) {
            debug.error("Failed to load JSON data when reading album art.", e);
        }

        // Download files
        JSONObject newHistory = new JSONObject();
        try {

            // Preparing history data structure
            newHistory.put("metadata", downloadObject.getJSONObject("metadata"));
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
                        debug.error("Error downloading song: " + downloadObject.getJSONArray("songs").getJSONObject(i).getString("title"), e);
                    } catch (JSONException er) {
                        debug.error("JSON Error downloading song", er);
                    }

                }

                // Update internal referencing
                downloadObject.getJSONArray("songs").getJSONObject(i).put("completed", true);
                downloadObject.getJSONArray("songs").getJSONObject(i).put("downloadCompleted", Instant.now().toEpochMilli());

                debug.trace(
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
            debug.error("JSON Error when attempting to access songs to download.", e);
        }

        // Updating the history
        try {
            downloadHistory.put(newHistory);
            Model.getInstance().download.setDownloadHistory(downloadHistory);
        } catch (IOException e) {
            debug.error("Failed to set new download history with current download.", e);
        }

        // Check if we should delete the album art
        // TODO: Change number settings
        try {
            switch (Model.getInstance().settings.getSettingInt("save_album_art")) {

                // Delete album art always
                case 0:
                    break;

                // Keep For Albums
                case 1:
                    if (downloadObject.getJSONArray("songs").length() > 1)
                        FileUtils.copyFile(
                                new File(resources.getInstance().getApplicationData() + "cached\\" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg"),
                                new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg")
                        );
                    break;

                // Keep for songs
                case 2:
                    if (downloadObject.getJSONArray("songs").length() == 1)
                        FileUtils.copyFile(
                                new File(resources.getInstance().getApplicationData() + "cached\\" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg"),
                                new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg")
                        );
                    break;

                // Keep always
                case 3:
                    FileUtils.copyFile(
                            new File(resources.getInstance().getApplicationData() + "cached\\" + downloadObject.getJSONObject("metadata").getString("artId") + ".jpg"),
                            new File(downloadObject.getJSONObject("metadata").getString("directory") + "\\art.jpg")
                    );
                    break;

                default:
                    debug.error("Unexpected value: " + Model.getInstance().settings.getSettingInt("save_album_art"), new IllegalStateException());
            }
        } catch (JSONException e) {
            debug.error("Failed to perform check to copy album art to download.", e);
        } catch (IOException e) {
            debug.warn("Failed to copy album art into downloads folder.");
        }

        // Move onto the next item if necessary
        Platform.runLater(() -> {

            if (downloadQueue.length() > 0) {

                try {

                    debug.trace(
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
                    debug.error("Failed to process queue.", e);
                }

            } else {
                debug.trace("Found 0 items remaining in queue, waiting for next download to start.");
                Model.getInstance().download.setDownloadObject(new JSONObject());
            }
        });

    }
}