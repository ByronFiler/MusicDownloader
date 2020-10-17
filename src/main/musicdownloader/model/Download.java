package musicdownloader.model;

import musicdownloader.controllers.Downloads;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.io.Downloader;
import musicdownloader.utils.io.GZip;
import musicdownloader.utils.io.validation.History;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Download {

    private volatile JSONArray downloadQueue = new JSONArray();
    private volatile JSONObject downloadObject = new JSONObject();
    private JSONArray downloadHistory = new JSONArray();

    private Downloads downloadsView = null;
    private Downloader downloader;

    public Download() {

        refreshDownloadHistory();
        if (downloadHistory.length() > 0) {

            double songCount = 0;
            try {
                for (int i = 0; i < downloadHistory.length(); i++)
                    songCount += downloadHistory.getJSONObject(i).getJSONArray("songs").length();
            } catch (JSONException e) {
                Debug.error("Failed to process JSON to calculate downloads song count.", e);
            }

            Debug.trace(
                    String.format(
                            "Found a download history of %s album%s and %.0f song%s.",
                            downloadHistory.length(),
                            downloadHistory.length() == 1 ? "" : "s",
                            songCount,
                            songCount == 1 ? "" : "s"
                    )
            );
        } else Debug.trace("Did not any existing download history.");

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
            Debug.trace("New download request received, queue is blank and hence will begin downloading...");

            // For processing downloads, the progress must be viewed as started to work
            try {
                for (int i = 0; i < queueItem.getJSONArray("songs").length(); i++)
                    queueItem.getJSONArray("songs").getJSONObject(i).put("completed", false);
            } catch (JSONException e) {
                Debug.error("Error updating downloads queue.", e);
            }

            downloadObject = queueItem;
            downloader = new Downloader();

        } else {

            // Download already in progress, hence queue
            Debug.trace(String.format("New download request received, adding to queue in position %s.", getDownloadQueue().length()+1));
            downloadQueue.put(queueItem);

        }
    }

    public void deleteHistory(JSONObject targetDeletion) {
        // Adding all to history except history item to remove
        try {
            downloadHistory = jsonArrayRemoval(targetDeletion, downloadHistory);
        } catch (JSONException e) {
            Debug.error("Failed to validate download history to remove element.", e);
        }

        // Rewriting the new history
        try {
            setDownloadHistory(downloadHistory);
        } catch (IOException e) {
            Debug.error("Error writing new download history.", e);
        }

    }

    private JSONArray jsonArrayRemoval(JSONObject targetDeletion, JSONArray downloadQueue) throws JSONException {

        JSONArray newQueue = new JSONArray();

        for (int i = 0; i < downloadQueue.length(); i++) {

            JSONObject queued = new JSONObject(downloadQueue.getJSONObject(i).toString());
            queued.put("songs", new JSONArray());

            for (int j = 0; j < downloadQueue.getJSONObject(i).getJSONArray("songs").length(); j++) {

                if (!downloadQueue.getJSONObject(i).getJSONArray("songs").getJSONObject(j).toString().equals(targetDeletion.toString()))
                    queued.getJSONArray("songs").put(downloadQueue.getJSONObject(i).getJSONArray("songs").getJSONObject(j));

            }
            // Only save history provided it actually has songs, otherwise wasted space
            if (queued.getJSONArray("songs").length() > 0) newQueue.put(queued);

        }

        return newQueue;
    }

    public synchronized void setDownloadHistory(JSONArray downloadHistory) throws IOException{
        GZip.compressData(
                new ByteArrayInputStream(downloadHistory.toString().getBytes()),
                new File(Resources.getInstance().getApplicationData() + "json/downloads.gz")
        );
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
            JSONArray diskHistory = new JSONArray(
                    GZip.decompressFile(
                            new File(
                                    Resources.getInstance().getApplicationData() + "json/downloads.gz"
                            )
                    ).toString()
            );

            if (diskHistory.toString().equals(new JSONObject().toString())) {
                this.downloadHistory = new JSONArray();
            } else {
                History historyValidator = new History(diskHistory);

                if (!historyValidator.getValidatedHistory().toString().equals(diskHistory.toString())) {
                    Debug.trace(
                            String.format(
                                    "Download history loaded, upon validation %s items removed, %s song%s, and %s histories have missing non essential metadata.",
                                    historyValidator.getModifiedRemovedHistories(),
                                    historyValidator.getModifiedRemovedSongs(),
                                    historyValidator.getModifiedRemovedSongs() == 1 ? "" : "s",
                                    historyValidator.getHistoriesWithPartialMetadata()
                                    )
                    );
                }

                this.downloadHistory = historyValidator.getValidatedHistory();

            }


        } catch (IOException | JSONException e) {
            try {
                if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "json/downloads.gz")))
                    if (!new File(Resources.getInstance().getApplicationData() + "json/downloads.gz").createNewFile())
                        throw new IOException();
            } catch (IOException er) {
                Debug.warn("Failed to create new downloads history file.");
            }
        }
    }

    public void setDownloadsView(Downloads downloadsView) {

        this.downloadsView = downloadsView;
    }

    public synchronized void markCompletedSong(int songIndex) {

        if (downloadsView != null) {

            downloadsView.markSongCompleted(songIndex);

        }

    }

    public synchronized void markCompletedDownload() {

        if (downloadsView != null) {

            downloadsView.markDownloadCompleted();

        }

    }

    public void cancel(int songIndex) {
        Objects.requireNonNull(downloader).cancel(songIndex);

        if (songIndex != -1) {
            try {
                downloadObject.getJSONArray("songs").getJSONObject(songIndex).put("cancelled", true);
            } catch (JSONException e) {
                Debug.error("Failed to access songs to mark song as cancelled.", e);
            }
        }
    }

    public void markCancelled(int songIndex) {

        if (downloadsView != null) {

            downloadsView.markCancelled(songIndex);

        }

    }

}