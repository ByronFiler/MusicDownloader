package musicdownloader.utils.io;

import com.mpatric.mp3agic.*;
import javafx.application.Platform;
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
 Download history should include a file md5, creation time, and source, and meta could include a link to the allmusic source?
 */

public class Downloader implements Runnable {

    private JSONObject downloadObject = Model.getInstance().download.getDownloadObject();
    private final JSONArray downloadHistory = Model.getInstance().download.getDownloadHistory();
    private JSONArray downloadQueue = Model.getInstance().download.getDownloadQueue();

    private final ArrayList<Float> songsValidity = new ArrayList<>();
    private final ArrayList<String> sources = new ArrayList<>();

    private byte[] albumArt;

    public Downloader() {
        new Thread(this, "acquire-download-files").start();
    }

    /*
    private float evaluateDownloadValidity (String sampleFileSource, String downloadedFile) {

        if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "temp")))
            if (!new File(Resources.getInstance().getApplicationData() + "temp").mkdirs())
                Debug.error("Failed to create temp file directory for validation.", new IOException());

        // Preparing sample: Downloading sample & Converting
        InputStream mp3Source = null;
        try {
            mp3Source = new URL(sampleFileSource).openStream();
            new Converter().convert(
                    mp3Source,
                    Resources.getInstance().getApplicationData() + "temp/sample.wav",
                    null,
                    null
            );
            new Converter().convert(
                    downloadedFile,
                    Resources.getInstance().getApplicationData() + "temp/source.wav"
            );

        } catch (FileNotFoundException e) {
            Debug.warn("Failed to find file to run check on.");
        } catch (IOException e) {
            Debug.warn("Error connecting to: " + sampleFileSource);
            e.printStackTrace();
            System.exit(-1);
        } catch (JavaLayerException e) {
            Debug.warn("Error processing given data for conversion.");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (mp3Source != null) mp3Source.close();
            } catch (IOException e) {
                Debug.error("Failed to close remote sample source stream.", e);
            }
        }

        // Checking if the download file needs to be converted
        byte[] sampleData;
        byte[] downloadData;

        try {
            downloadData = new FingerprintManager().extractFingerprint(new Wave(Resources.getInstance().getApplicationData() + "temp/source.wav"));
            sampleData = new FingerprintManager().extractFingerprint(new Wave(Resources.getInstance().getApplicationData() + "temp/sample.wav"));
        } catch (ArrayIndexOutOfBoundsException ignored) {
            Debug.warn("File is too large to be checked.");
            return 1;
        }

        // Deleting temporary files
        if (!new File(Resources.getInstance().getApplicationData() + "temp/source.wav").delete())
            Debug.warn("Failed to delete source.wav");
        if (!new File(Resources.getInstance().getApplicationData() + "temp/sample.wav").delete())
            Debug.warn("Failed to delete sample.wav");

        FingerprintSimilarityComputer fingerprint = new FingerprintSimilarityComputer(sampleData, downloadData);
        return fingerprint.getFingerprintsSimilarity().getScore();
    }
     */

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

        // TODO: Happens rarely, likely due to a youtube-dl renaming process, consider awaiting for maybe 50 or 100ms?
        if (currentFiles.size() != 1)
            Debug.error(
                    String.format(
                            "Expected 1 new file to have been created, %s found.", currentFiles.size()
                    ),
                    new IllegalArgumentException("Unexpected new files")
            );

        File downloadedFile = currentFiles.get(0);

        if (downloadedFile == null) throw new IOException("Failed to find downloaded file.");

        // Validate
        if (Model.getInstance().settings.getSettingBool("advanced_validation") && song.get("sample") != JSONObject.NULL) {

            float downloadValidity = analysis.compare(downloadedFile.getAbsolutePath());

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

                if (!downloadedFile.delete())
                    Debug.warn("Failed to delete: " + downloadedFile.getName());

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

            analysis.correctAmplitude(downloadedFile.getAbsolutePath());

            sources.add(song.getJSONArray("source").getString(sourceDepth));
        } else {
            sources.add(song.getJSONArray("source").getString(0));
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
                    mp3Applicator.save(downloadObject.getJSONObject("metadata").getString("directory") + "/" + song.getString("title").replaceAll("[\u0000-\u001f<>:\"/\\\\|?*\u007f]+", "_") + "." + format);

                    // Delete old file
                    if (!downloadedFile.delete()) {
                        Debug.error("Failed to delete file: " + downloadedFile, new IOException());
                    }

                } catch (IOException | NotSupportedException e) {
                    Debug.warn("Failed to apply metadata.");
                }
            } catch (InvalidDataException | UnsupportedTagException e) {
                Debug.warn("Failed to apply meta data to: " + downloadedFile);
            }

        } else {

            // Just move & rename the file
            Files.move(
                    Paths.get(downloadedFile.getAbsolutePath()),
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
                    System.out.println(downloadObject.getJSONArray("songs").getJSONObject(i).get("sample"));
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

            // If the application is iconified then notify the user using their OSs notification system
            if (Model.getInstance().getPrimaryStage().isIconified()) new Notification(downloadObject.getJSONObject("metadata").getString("album"), "", new File(downloadObject.getJSONObject("metadata").getString("directory")), TrayIcon.MessageType.INFO);

        } catch (JSONException e) {
            Debug.error("JSON Error when attempting to access songs to download.", e);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JavaLayerException e) {
            e.printStackTrace();
        }

        // Updating the history
        try {
            downloadHistory.put(newHistory);
            Model.getInstance().download.setDownloadHistory(downloadHistory);

        } catch (IOException e) {
            Debug.error("Failed to set new download history with current download.", e);
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
        Platform.runLater(() -> {

            if (downloadQueue.length() > 0) {

                try {

                    Debug.trace(
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
                    Model.getInstance().download.markCompletedDownload();

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
                    new Downloader();

                } catch (JSONException e) {
                    Debug.error("Failed to process queue.", e);
                }

            } else {
                Debug.trace("Found 0 items remaining in queue, waiting for next download to start.");
                Model.getInstance().download.setDownloadObject(new JSONObject());
                Model.getInstance().download.markCompletedDownload();
            }
        });

    }
}