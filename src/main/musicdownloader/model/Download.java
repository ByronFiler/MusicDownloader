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

        /*
        try {
            downloadObject = new JSONObject("{\"metadata\":{\"artId\":\"42892976263314553\",\"art\":\"https://rovimusic.rovicorp.com/image.jpg?c=EUokLyfMctShPK4In1fAA4Af7E_1E-2MlBBPmPAXRBU=&f=2\",\"artist\":\"Pink Floyd\",\"year\":\"1973\",\"album\":\"The Dark Side of the Moon\",\"is_album\":true,\"genre\":\"Pop/Rock\",\"format\":\"mp3\",\"playtime\":2574,\"directory\":\"C:\\\\Users\\\\byron\\\\Downloads/The Dark Side of the Moon\"},\"songs\":[{\"position\":1,\"id\":\"5199187936030318\",\"completed\":true,\"playtime\":67,\"source\":[\"https://www.youtube.com/watch?v=HW-lXjOyUWo\"],\"title\":\"Speak to Me\",\"sample\":\"hTIxMWJqyil71qaGizxidhyhM-OFI8zG4l-qVpXXB1I\"},{\"position\":2,\"id\":\"4382816882987679\",\"completed\":true,\"playtime\":169,\"source\":[\"https://www.youtube.com/watch?v=y1i8RoAQW-8\",\"https://www.youtube.com/watch?v=9ZdyzAKU3cY\",\"https://www.youtube.com/watch?v=1G0cQuGyULk\",\"https://vimeo.com/50255789\",\"https://vimeo.com/136548380\",\"https://vimeo.com/72008821\",\"https://www.youtube.com/watch?v=gnLLuzS2Ofw\",\"https://vimeo.com/93248853\",\"https://vimeo.com/51361798\"],\"title\":\"Breathe (In the Air)\",\"sample\":\"hIzYGfNje1cR64cquskMmCpQg_7iAU1wjqLgK_xGXts\"},{\"position\":3,\"id\":\"4580327733286168\",\"completed\":true,\"playtime\":225,\"source\":[\"https://www.youtube.com/watch?v=2sUyk5zSbhM\",\"https://www.youtube.com/watch?v=vVooyS4mG4w\",\"https://www.youtube.com/watch?v=VouHPeO4Gls\",\"https://www.youtube.com/watch?v=POKIpg6NGzQ\",\"https://www.youtube.com/watch?v=GZB6mGGuUIQ\",\"https://www.youtube.com/watch?v=OYwzqc1YOKE\",\"https://www.youtube.com/watch?v=ynd7TLJXl0E\",\"https://www.youtube.com/watch?v=yq3VYrASrSI\",\"https://vimeo.com/254978995\",\"https://vimeo.com/222565831\",\"https://vimeo.com/166050133\"],\"title\":\"On the Run\",\"sample\":\"ES4UL38AjoK_6hpwCBKoog4Q1ghY8VaPylnm7PwcKNY\"},{\"position\":4,\"id\":\"6390972562255287\",\"completed\":true,\"playtime\":413,\"source\":[\"https://www.youtube.com/watch?v=JwYX52BP2Sk\",\"https://www.youtube.com/watch?v=F_VjVqe3KJ0\",\"https://www.youtube.com/watch?v=pgXozIma-Oc\",\"https://www.youtube.com/watch?v=oEGL7j2LN84\",\"https://www.youtube.com/watch?v=T2LUl9C_Yfk\",\"https://www.youtube.com/watch?v=GG2tZNOQWAA\",\"https://www.youtube.com/watch?v=-EzURpTF5c8\",\"https://www.youtube.com/watch?v=xYxY_P8Vn3k\",\"https://www.youtube.com/watch?v=_FrOQC-zEog\",\"https://www.youtube.com/watch?v=XVE8Tqc1WCA\",\"https://vimeo.com/85409684\",\"https://vimeo.com/222563539\",\"https://vimeo.com/85612882\",\"https://vimeo.com/223972689\",\"https://vimeo.com/90546532\",\"https://www.youtube.com/watch?v=A7pI96Osp9c\",\"https://www.youtube.com/watch?v=Z-OytmtYoOI\",\"https://www.youtube.com/watch?v=LNBRBTDBUxQ\",\"https://www.youtube.com/watch?v=lke-uABclNk\",\"https://vimeo.com/364261280\",\"https://vimeo.com/213229407\",\"https://vimeo.com/101141597\"],\"title\":\"Time\",\"sample\":\"GGyQUL4DuzbMe8ua6RRqRgSijaXJlYnq0St31qpAJWo\"},{\"position\":5,\"id\":\"20283191996754235\",\"completed\":false,\"playtime\":284,\"source\":[\"https://www.youtube.com/watch?v=cVBCE3gaNxc\",\"https://www.youtube.com/watch?v=mPGv8L3a_sY\",\"https://www.youtube.com/watch?v=sxo0OJkbaMY\",\"https://www.youtube.com/watch?v=dTfHYnSF29Q\",\"https://www.youtube.com/watch?v=ITSKb0uX3Qw\",\"https://www.youtube.com/watch?v=kK0rpKOEAt0\",\"https://www.youtube.com/watch?v=bLRi_R_QiBU\",\"https://www.youtube.com/watch?v=khIjh-zIm_c\",\"https://www.youtube.com/watch?v=zBWbpFz3wac\",\"https://www.youtube.com/watch?v=T13se_2A7c8\",\"https://www.youtube.com/watch?v=J3xc5H_N8ds\",\"https://vimeo.com/videocomposer/greatestgig\",\"https://vimeo.com/170377228\",\"https://vimeo.com/222560452\",\"https://vimeo.com/174991043\",\"https://vimeo.com/15786034\",\"https://vimeo.com/290082431\",\"https://vimeo.com/23394793\",\"https://vimeo.com/305527614\",\"https://vimeo.com/47814457\",\"https://www.youtube.com/watch?v=CVWHItGgrdE\",\"https://www.youtube.com/watch?v=qanO3qf9-rE\",\"https://www.youtube.com/watch?v=gryCFevszRQ\",\"https://www.youtube.com/watch?v=vWZ6hmHj2MA\",\"https://vimeo.com/29835877\",\"https://vimeo.com/thalesrios/thegreatgiginthesky\",\"https://vimeo.com/306648718\"],\"title\":\"The Great Gig in the Sky\",\"sample\":\"hIzYGfNje1cR64cquskMmB_TZlp6n_cq-Emr2zx15tU\"},{\"position\":6,\"id\":\"7649601078491408\",\"completed\":false,\"playtime\":383,\"source\":[\"https://www.youtube.com/watch?v=cpbbuaIA3Ds\",\"https://www.youtube.com/watch?v=Kjgwjh4H7wg\",\"https://www.youtube.com/watch?v=z3cg3IQzSqw\",\"https://www.youtube.com/watch?v=JkhX5W7JoWI\",\"https://www.youtube.com/watch?v=8oPq1-ymSVY\",\"https://www.youtube.com/watch?v=sndo_wdc384\",\"https://www.youtube.com/watch?v=T2KiJGJq_pk\",\"https://www.youtube.com/watch?v=-5y99_rwhBY\",\"https://www.youtube.com/watch?v=Z0ftw7tMfOM\",\"https://www.youtube.com/watch?v=wMQ5UBYGwnY\",\"https://vimeo.com/222558083\",\"https://vimeo.com/54787709\",\"https://vimeo.com/274292285\",\"https://vimeo.com/10801089\",\"https://vimeo.com/219443227\",\"https://www.youtube.com/watch?v=YR5ApYxkU-U\",\"https://www.youtube.com/watch?v=JwYX52BP2Sk\",\"https://vimeo.com/36873764\"],\"title\":\"Money\",\"sample\":\"GGyQUL4DuzbMe8ua6RRqRphUoDg0hsvx4F4sL4oO-nA\"},{\"position\":7,\"id\":\"0667559730713927\",\"completed\":false,\"playtime\":469,\"source\":[\"https://www.youtube.com/watch?v=nDbeqj-1XOo\",\"https://www.youtube.com/watch?v=I3OdanjBYoM\",\"https://www.youtube.com/watch?v=GKiLEgAzFDQ\",\"https://www.youtube.com/watch?v=s_Yayz5o-l0\",\"https://www.youtube.com/watch?v=Sd4ihZVgSE0\",\"https://www.youtube.com/watch?v=O7w765-TbjY\",\"https://www.youtube.com/watch?v=eGwtXfIH3bc\",\"https://www.youtube.com/watch?v=h90j3lOXNvU\",\"https://www.youtube.com/watch?v=GIoJ_ihR7SA\",\"https://www.youtube.com/watch?v=zVpvhzMYGpM\",\"https://www.youtube.com/watch?v=nXEvZXIseb4\",\"https://vimeo.com/29481456\",\"https://vimeo.com/222533985\",\"https://vimeo.com/149968430\",\"https://vimeo.com/426025549\",\"https://www.youtube.com/watch?v=IouJU_EDTYU\",\"https://www.youtube.com/watch?v=deU_uwlNpOo\",\"https://www.youtube.com/watch?v=wzRYUpBHXNk\",\"https://www.youtube.com/watch?v=LezoMi3yftM\",\"https://vimeo.com/5002330\",\"https://vimeo.com/62262714\",\"https://vimeo.com/17967739\",\"https://vimeo.com/412076300\"],\"title\":\"Us and Them\",\"sample\":\"hTIxMWJqyil71qaGizxidoAf7E_1E-2MlBBPmPAXRBU\"},{\"position\":8,\"id\":\"7147675884914265\",\"completed\":false,\"playtime\":206,\"source\":[\"https://www.youtube.com/watch?v=bK7HJvmgFnM\",\"https://www.youtube.com/watch?v=_83urK9rO4U\",\"https://www.youtube.com/watch?v=yXumgSaFPpA\",\"https://www.youtube.com/watch?v=i3ioG1-JipA\",\"https://vimeo.com/222533103\",\"https://vimeo.com/77477152\",\"https://vimeo.com/128092257\",\"https://vimeo.com/15994514\",\"https://www.youtube.com/watch?v=KW2UwELSE3M\",\"https://vimeo.com/kingrecordings/any-colour-you-like\",\"https://vimeo.com/119162598\"],\"title\":\"Any Colour You Like\",\"sample\":\"uevnophJKve4T7V89NdIbD6KsMttLlyBmmVTZ6_CLs0\"},{\"position\":9,\"id\":\"6265485740109712\",\"completed\":false,\"playtime\":226,\"source\":[\"https://www.youtube.com/watch?v=BhYKN21olBw\",\"https://www.youtube.com/watch?v=LDxF80pyVDo\",\"https://www.youtube.com/watch?v=uPnlZB6XiM8\",\"https://www.youtube.com/watch?v=pE_Q0ohfeyE\",\"https://www.youtube.com/watch?v=ohMRfCxUKkA\",\"https://vimeo.com/2380630\",\"https://www.youtube.com/watch?v=pQJo2p2_ncw\",\"https://vimeo.com/d3cstudios/braindamage\"],\"title\":\"Brain Damage\",\"sample\":\"hTIxMWJqyil71qaGizxidgSijaXJlYnq0St31qpAJWo\"},{\"position\":10,\"id\":\"5613302638103108\",\"completed\":false,\"playtime\":132,\"source\":[\"https://www.youtube.com/watch?v=9wjZrswriz0\",\"https://www.youtube.com/watch?v=jIC5MtVVzos\",\"https://www.youtube.com/watch?v=YmCA4Y8fUZo\",\"https://www.youtube.com/watch?v=xkOt3UAEEC0\"],\"title\":\"Eclipse\",\"sample\":\"hTIxMWJqyil71qaGizxidt_M69_UI9rrJSVvWL2-yAg\"}]}\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
         */

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
    }

    public void markCancelled(int songIndex) {

        if (downloadsView != null) {

            downloadsView.markCancelled(songIndex);

        }

    }

}