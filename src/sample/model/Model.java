package sample.model;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sample.utils.debug;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

public class Model {
    private final static Model instance = new Model();

    public final sample.model.settings settings = new settings();
    public final sample.model.download download = new download();
    public final sample.model.search search = new search();

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
                debug.error(null, "Failed to parse download history for art IDs.", e);
            }

            for (File foundFile: Objects.requireNonNull(new File(System.getenv("APPDATA") + "\\MusicDownloader/cached").listFiles())) {

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
                        debug.warn(null, "Failed to delete file: " + foundFile.getAbsolutePath());
                    else
                        deletedFilesCount++;
                }

            }

            if (deletedFilesCount > 0)
                debug.trace(Thread.currentThread(), String.format("Deleted %s non used file%s.", deletedFilesCount, deletedFilesCount == 1 ? "" : "s"));

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
                            debug.warn(Thread.currentThread(), "Failed to delete " + deleteFile.getAbsolutePath());
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

                        debug.trace(Thread.currentThread(), deletedFilesMessage.toString());

                    }

                } catch (IOException e) {
                    debug.warn(null, "Failed to write updated downloads history.");
                }


            } catch (JSONException e) {
                debug.error(null, "Failed to rewrite JSON data for download queue optimisation.", e);
            }

            // Start re-downloading missing files.
            JSONArray downloadObjects = new JSONArray();
            try {
                for (int i = 0; i < downloadHistory.length(); i++) {

                    if (!Files.exists(Paths.get(String.format(System.getenv("APPDATA") + "\\MusicDownloader\\cached\\%s.jpg", downloadHistory.getJSONObject(i).getString("artId"))))) {

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
                            new File(String.format(System.getenv("APPDATA") + "\\MusicDownloader\\cached\\%s.jpg", downloadObjects.getJSONObject(i).getString("artId")))
                    );
                    reacquiredFilesCount++;

                }
                if (reacquiredFilesCount > 0) {
                    debug.trace(
                            Thread.currentThread(),
                            String.format("Reacquired %s file%s to cache.", reacquiredFilesCount, reacquiredFilesCount == 1 ? "" : "s")
                    );

                    download.setDownloadHistory(downloadHistory);
                }

            } catch (JSONException | IOException e) {
                debug.error(Thread.currentThread(), "Failed to get art for checking files to re-download.", e);
            }

        }, "cache-optimiser").start();

    }

    public static Model getInstance() {
        return instance;
    }
}