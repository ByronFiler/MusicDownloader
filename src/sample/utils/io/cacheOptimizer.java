package sample.utils.io;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sample.model.Model;
import sample.utils.app.debug;
import sample.utils.app.resources;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

public class cacheOptimizer implements Runnable{

    private final JSONArray downloadHistory;

    public cacheOptimizer(JSONArray downloadHistory) {
        this.downloadHistory = downloadHistory;
        new Thread(this, "cache-optimizer").start();
    }

    @Override
    public void run() {

        // Clearing temporary files
        if (Files.exists(Paths.get(resources.applicationData + "temp\\"))) {
            try {
                File tempFiles = new File(resources.applicationData + "temp\\");
                int preexistingFiles = Objects.requireNonNull(tempFiles.listFiles()).length;

                FileUtils.deleteDirectory(new File(resources.applicationData + "temp\\"));
                debug.trace(Thread.currentThread(), String.format("Deleted %s temporary files.", preexistingFiles));

            } catch (IOException e) {
                debug.warn(Thread.currentThread(), "Failed to delete temp directory.");
            }
        }

        ArrayList<String> usedArtIds = new ArrayList<>();
        ArrayList<File> deleteFiles = new ArrayList<>();
        JSONArray filesData = new JSONArray();
        JSONArray renameRequests = new JSONArray();
        int deletedFilesCount = 0;

        try {
            for (int i = 0; i < downloadHistory.length(); i++) {
                if (!usedArtIds.contains(downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("artId")))
                    usedArtIds.add(downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("artId"));
            }
        } catch (JSONException e) {
            debug.error(Thread.currentThread(), "Failed to parse download history for art IDs.", e);
        }

        for (File foundFile: Objects.requireNonNull(new File(resources.applicationData + "cached").listFiles())) {

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
                    debug.error(Thread.currentThread(), "Error processing file.", e);
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

            for (int i = 0; i < renameRequests.length(); i++)
                for (int j = 0; j < downloadHistory.length(); j++)

                    if (downloadHistory.getJSONObject(j).getJSONObject("metadata").getString("artId").equals(renameRequests.getJSONObject(i).getString("original")))
                        downloadHistory.getJSONObject(j).getJSONObject("metadata").put("artId", renameRequests.getJSONObject(i).getString("new"));

            try {

                Model.getInstance().download.setDownloadHistory(downloadHistory);

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

                if (
                        !Files.exists(
                                Paths.get(
                                        String.format(
                                                "%scached\\%s.jpg",
                                                resources.applicationData,
                                                downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("artId")
                                        )
                                )
                        )
                ) {

                    boolean alreadyPlanned = false;
                    if (downloadObjects.length() > 0) {
                        for (int j = 0; j < downloadObjects.length(); j++) {

                            if (downloadObjects.getJSONObject(j).getJSONObject("metadata").getString("art").equals(downloadHistory.getJSONObject(i).getJSONObject("metadata").getString("art"))) {
                                alreadyPlanned = true;
                                downloadHistory.getJSONObject(j).put("artId", downloadObjects.getJSONObject(j).getJSONObject("metadata").getString("artId"));
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
                        new URL(downloadObjects.getJSONObject(i).getJSONObject("metadata").getString("art")),
                        new File(String.format(
                                "%scached\\%s.jpg",
                                resources.applicationData,
                                downloadObjects.getJSONObject(i).getJSONObject("metadata").getString("artId")
                        ))
                );
                reacquiredFilesCount++;

            }
            if (reacquiredFilesCount > 0) {
                debug.trace(
                        Thread.currentThread(),
                        String.format("Reacquired %s file%s to cache.", reacquiredFilesCount, reacquiredFilesCount == 1 ? "" : "s")
                );

                Model.getInstance().download.setDownloadHistory(downloadHistory);
            }

        } catch (JSONException | IOException e) {
            debug.error(Thread.currentThread(), "Failed to get art for checking files to re-download.", e);
        }
    }
}
