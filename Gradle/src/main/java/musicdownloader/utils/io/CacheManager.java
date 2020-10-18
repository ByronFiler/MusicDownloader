package musicdownloader.utils.io;

import musicdownloader.model.Model;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

public class CacheManager implements Runnable {

    private final JSONArray downloadHistory;

    public CacheManager(JSONArray downloadHistory) {
        this.downloadHistory = downloadHistory;
        new Thread(this, "cache-optimizer").start();
    }

    @Override
    public void run() {

        // Clearing temporary files
        if (Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "temp/"))) {
            try {
                File tempFiles = new File(Resources.getInstance().getApplicationData() + "temp");
                int preexistingFiles = Objects.requireNonNull(tempFiles.listFiles()).length;

                FileUtils.deleteDirectory(new File(Resources.getInstance().getApplicationData() + "temp"));
                Debug.trace(String.format("Deleted %s temporary files.", preexistingFiles));

            } catch (IOException e) {
                Debug.warn("Failed to delete temp directory.");
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
            Debug.error("Failed to parse download history for art IDs.", e);
        }

        if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "cached")))
            if (!new File(Resources.getInstance().getApplicationData() + "cached").mkdirs())
                Debug.error("Failed to create cached directory", new IOException());


        for (File foundFile: Objects.requireNonNull(new File(Resources.getInstance().getApplicationData() + "cached").listFiles())) {

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
                    Debug.error("Error processing file.", e);
                }

            } else {
                if (!foundFile.delete())
                    Debug.warn("Failed to delete file: " + foundFile.getAbsolutePath());
                else
                    deletedFilesCount++;
            }

        }

        if (deletedFilesCount > 0)
            Debug.trace(String.format("Deleted %s non used file%s.", deletedFilesCount, deletedFilesCount == 1 ? "" : "s"));

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
                        Debug.warn("Failed to delete " + deleteFile.getAbsolutePath());
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

                    Debug.trace(deletedFilesMessage.toString());

                }

            } catch (IOException e) {
                Debug.warn("Failed to write updated downloads history.");
            }


        } catch (JSONException e) {
            Debug.error("Failed to rewrite JSON data for download queue optimisation.", e);
        }

        // Start re-downloading missing files.
        JSONArray downloadObjects = new JSONArray();
        try {
            for (int i = 0; i < downloadHistory.length(); i++) {

                if (
                        !Files.exists(
                                Paths.get(
                                        String.format(
                                                "%scached/%s.jpg",
                                                Resources.getInstance().getApplicationData(),
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
                                "%scached/%s.jpg",
                                Resources.getInstance().getApplicationData(),
                                downloadObjects.getJSONObject(i).getJSONObject("metadata").getString("artId")
                        ))
                );
                reacquiredFilesCount++;

            }
            if (reacquiredFilesCount > 0) {
                Debug.trace(
                        String.format("Reacquired %s file%s to cache.", reacquiredFilesCount, reacquiredFilesCount == 1 ? "" : "s")
                );

                Model.getInstance().download.setDownloadHistory(downloadHistory);
            }

        } catch (JSONException | IOException e) {
            Debug.error("Failed to get art for checking files to re-download.", e);
        }
    }
}
