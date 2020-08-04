package sample.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sample.utils.acquireDownloadFiles;
import sample.utils.debug;
import sample.utils.gzip;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class download{

    private volatile JSONArray downloadQueue = new JSONArray();
    private volatile JSONObject downloadObject = new JSONObject();
    private JSONArray downloadHistory = new JSONArray();

    public download() {
        refreshDownloadHistory();
        debug.trace(null, String.format("Found a download history of %s item%s.", downloadHistory.length(), downloadHistory.length() == 1 ? "" : "s"));
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
            debug.trace(null, "New download request received, queue is blank and hence will begin downloading...");

            // For processing downloads, the progress must be viewed as started to work
            try {
                for (int i = 0; i < queueItem.getJSONArray("songs").length(); i++)
                    queueItem.getJSONArray("songs").getJSONObject(i).put("completed", false);
            } catch (JSONException e) {
                debug.error(null, "Error updating downloads queue.", e);
            }

            downloadObject = queueItem;
            new acquireDownloadFiles();

        } else {

            // Download already in progress, hence queue
            debug.trace(null, String.format("New download request received, adding to queue in position %s.", getDownloadQueue().length()+1));
            downloadQueue.put(queueItem);

        }
    }

    public synchronized boolean downloadsAccessible() {

        // Should allow access to downloads if there is: download history or downloads in progress
        return downloadHistory.length() > 0 || downloadQueue.length() > 0 || downloadObject.has("metadata");

    }

    public void deleteHistory(JSONObject targetDeletion) {
        JSONArray newDownloadHistory = new JSONArray();

        // Adding all to history except history item to remove
        try {
            for (int i = 0; i < downloadHistory.length(); i++)
                if (!downloadHistory.getJSONObject(i).toString().equals(targetDeletion.toString()))
                    newDownloadHistory.put(downloadHistory.getJSONObject(i));
        } catch (JSONException e) {
            debug.error(null, "Failed to validate download history to remove element.", e);
        }

        // Rewriting the new history
        try {
            setDownloadHistory(newDownloadHistory);
        } catch (IOException e) {
            debug.error(null, "Error writing new download history.", e);
        }

    }

    public synchronized void setDownloadHistory(JSONArray downloadHistory) throws IOException{
        gzip.compressData(new ByteArrayInputStream(downloadHistory.toString().getBytes()), new File(System.getenv("APPDATA") + "\\MusicDownloader\\json\\downloads.gz"));
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

            this.downloadHistory = new JSONArray(
                    gzip.decompressFile(
                            new File(System.getenv("APPDATA") + "\\MusicDownloader\\json\\downloads.gz")
                    ).toString()
            );

        } catch (IOException | JSONException e) {
            try {
                if (!Files.exists(Paths.get(System.getenv("APPDATA") + "\\MusicDownloader\\json\\downloads.gz")))
                    if (!new File(System.getenv("APPDATA") + "\\MusicDownloader\\json\\downloads.gz").createNewFile())
                        throw new IOException();
            } catch (IOException er) {
                debug.warn(null, "Failed to create new downloads history file.");
            }
        }
    }
}