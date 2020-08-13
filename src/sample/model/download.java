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

        /*
        // TODO: TEST CODE ONLY DO NOT LEAVE IN COMMIT
        try {
            downloadObject = new JSONObject("{\"metadata\":{\"art\":\"https://rovimusic.rovicorp.com/image.jpg?c=EUokLyfMctShPK4In1fAA4Af7E_1E-2MlBBPmPAXRBU=&f=2\",\"artId\":\"2633949778814103\",\"artist\":\"Pink Floyd\",\"year\":\"1973\",\"album\":\"The Dark Side of the Moon\",\"genre\":\"Pop/Rock\",\"playtime\":2574,\"directory\":\"C:\\\\Users\\\\byron\\\\Documents\\\\Dev\\\\The Dark Side of the Moon\"},\"songs\":[{\"position\":1,\"id\":\"6827486541840608\",\"source\":[\"xCbzkW5wero\",\"HW-lXjOyUWo\"],\"completed\":true,\"playtime\":67,\"title\":\"Speak to Me\",\"sample\":\"hTIxMWJqyil71qaGizxidhyhM-OFI8zG4l-qVpXXB1I\"},{\"position\":2,\"id\":\"9413816142670984\",\"source\":[\"y1i8RoAQW-8\",\"UOW2pfZKF4Y\",\"gnLLuzS2Ofw\"],\"completed\":true,\"playtime\":169,\"title\":\"Breathe (In the Air)\",\"sample\":\"hIzYGfNje1cR64cquskMmCpQg_7iAU1wjqLgK_xGXts\"},{\"position\":3,\"id\":\"5018704478839862\",\"source\":[\"vVooyS4mG4w\",\"2sUyk5zSbhM\",\"POKIpg6NGzQ\",\"OYwzqc1YOKE\",\"VouHPeO4Gls\",\"usEByUDs_7k\",\"GZB6mGGuUIQ\",\"mrojrDCI02k\"],\"completed\":true,\"playtime\":225,\"title\":\"On the Run\",\"sample\":\"ES4UL38AjoK_6hpwCBKoog4Q1ghY8VaPylnm7PwcKNY\"},{\"position\":4,\"id\":\"7685339846329113\",\"source\":[\"_FrOQC-zEog\",\"T2LUl9C_Yfk\",\"pgXozIma-Oc\",\"JwYX52BP2Sk\",\"-EzURpTF5c8\",\"oEGL7j2LN84\",\"F_VjVqe3KJ0\",\"GG2tZNOQWAA\",\"A7pI96Osp9c\",\"Z-OytmtYoOI\",\"LNBRBTDBUxQ\",\"z67FsTNpexg\",\"YR5ApYxkU-U\"],\"completed\":true,\"playtime\":413,\"title\":\"Time\",\"sample\":\"GGyQUL4DuzbMe8ua6RRqRgSijaXJlYnq0St31qpAJWo\"},{\"position\":5,\"id\":\"9912466365158121\",\"source\":[\"mPGv8L3a_sY\",\"T13se_2A7c8\",\"cVBCE3gaNxc\",\"kK0rpKOEAt0\",\"2-DvI9Ljeg4\",\"dTfHYnSF29Q\",\"sxo0OJkbaMY\",\"qanO3qf9-rE\",\"gryCFevszRQ\",\"vWZ6hmHj2MA\"],\"completed\":true,\"playtime\":284,\"title\":\"The Great Gig in the Sky\",\"sample\":\"hIzYGfNje1cR64cquskMmB_TZlp6n_cq-Emr2zx15tU\"},{\"position\":6,\"id\":\"5023478936040369\",\"source\":[\"JkhX5W7JoWI\",\"rwPM01cbQBc\",\"Z0ftw7tMfOM\",\"z3cg3IQzSqw\",\"cpbbuaIA3Ds\",\"Kjgwjh4H7wg\",\"8oPq1-ymSVY\",\"YR5ApYxkU-U\",\"JwYX52BP2Sk\",\"_FrOQC-zEog\"],\"completed\":true,\"playtime\":383,\"title\":\"Money\",\"sample\":\"GGyQUL4DuzbMe8ua6RRqRphUoDg0hsvx4F4sL4oO-nA\"},{\"position\":7,\"id\":\"5122579136747598\",\"source\":[\"rbL0r_Lbd_4\",\"GKiLEgAzFDQ\",\"nDbeqj-1XOo\",\"h90j3lOXNvU\",\"s_Yayz5o-l0\",\"I3OdanjBYoM\",\"eGwtXfIH3bc\",\"sf1JN1lLN2I\",\"JbGyNtKKK5I\",\"Sd4ihZVgSE0\",\"1DzsPJYWeCw\",\"deU_uwlNpOo\",\"wzRYUpBHXNk\",\"LezoMi3yftM\"],\"completed\":false,\"playtime\":469,\"title\":\"Us and Them\",\"sample\":\"hTIxMWJqyil71qaGizxidoAf7E_1E-2MlBBPmPAXRBU\"},{\"position\":8,\"id\":\"5844234730871897\",\"source\":[\"udBV26MMvSc\",\"bK7HJvmgFnM\",\"_83urK9rO4U\",\"i3ioG1-JipA\",\"yXumgSaFPpA\",\"KW2UwELSE3M\",\"A7UcDfTct1o\"],\"completed\":false,\"playtime\":206,\"title\":\"Any Colour You Like\",\"sample\":\"uevnophJKve4T7V89NdIbD6KsMttLlyBmmVTZ6_CLs0\"},{\"position\":9,\"id\":\"7189874229911899\",\"source\":[\"JC5O9ZHXmTs\",\"BhYKN21olBw\",\"pE_Q0ohfeyE\",\"uPnlZB6XiM8\",\"LDxF80pyVDo\",\"ohMRfCxUKkA\"],\"completed\":false,\"playtime\":226,\"title\":\"Brain Damage\",\"sample\":\"hTIxMWJqyil71qaGizxidgSijaXJlYnq0St31qpAJWo\"},{\"position\":10,\"id\":\"6854918609282694\",\"source\":[\"jIC5MtVVzos\",\"WZtfsfoKSB0\",\"9wjZrswriz0\",\"YmCA4Y8fUZo\",\"n9xOl8qZ7tc\"],\"completed\":false,\"playtime\":132,\"title\":\"Eclipse\",\"sample\":\"hTIxMWJqyil71qaGizxidt_M69_UI9rrJSVvWL2-yAg\"}]}\n");
            downloadQueue = new JSONArray("[{\"metadata\":{\"art\":\"https://rovimusic.rovicorp.com/image.jpg?c=EUokLyfMctShPK4In1fAA1WnbEN5fCjifro6xhIBuB4=&f=2\",\"artId\":\"22551850439852816\",\"artist\":\"Pink Floyd\",\"year\":\"1971\",\"album\":\"Meddle\",\"genre\":\"Pop/Rock\",\"playtime\":2806,\"directory\":\"C:\\\\Users\\\\byron\\\\Downloads\\\\Meddle\"},\"songs\":[{\"position\":1,\"id\":\"14254256869539628\",\"source\":[\"48PJGVf4xqk\",\"s-CSfikHGsY\",\"MrmGRxrvODI\",\"ikMAH7k3pz4\",\"_KXZRcZaQJ4\",\"Go_OFriHOEI\",\"YZCqh0beUwM\",\"218_-Lze8N0\",\"TOa7IYotms8\",\"BgVYyQD05uc\",\"Vb7JhpD2g4k\",\"ABPQdXertbs\",\"nXaXKfyI7tQ\"],\"completed\":null,\"playtime\":355,\"title\":\"One of These Days\",\"sample\":\"sIW9QEIYIf4dHCbefpGhTj6KsMttLlyBmmVTZ6_CLs0\"},{\"position\":2,\"id\":\"8022527185651916\",\"source\":[\"yaTvVXycP94\",\"sQ02i8elJu8\",\"sctJuNYo59s\",\"Rj3kDOIFnMI\",\"uThZ1uuLLWM\",\"wzqbesV1Z_o\",\"4f6-akNjeto\",\"wFt99jcazHs\",\"1a2hvqLPbSE\",\"wQ712S4LYpU\",\"xAiFyim8YnQ\",\"8dLuKGpdHqc\",\"rKMmMclXo2g\",\"7Ozh3-DjJNA\"],\"completed\":null,\"playtime\":312,\"title\":\"A Pillow of Winds\",\"sample\":\"sIW9QEIYIf4dHCbefpGhTh_TZlp6n_cq-Emr2zx15tU\"},{\"position\":3,\"id\":\"5641053232853068\",\"source\":[\"UvPiv7F1lfE\",\"TeyHPAdxuy0\",\"t3J_2R9rAp8\",\"kAhbGzvB2gU\",\"uCgQuj8v2gg\",\"9M23zjNrG9M\",\"TOCW403Dm9g\",\"sl_apx8JoMw\",\"L8GjXdizFs4\",\"1b8T2keXcCI\",\"hE_SCDY5DhY\",\"MEemQey1iew\",\"cPL7chgv0Ig\"],\"completed\":null,\"playtime\":368,\"title\":\"Fearless\",\"sample\":\"sIW9QEIYIf4dHCbefpGhTg4Q1ghY8VaPylnm7PwcKNY\"},{\"position\":4,\"id\":\"17443501950977247\",\"source\":[\"FwITUMl5UKw\",\"KkRcwqxLEuk\",\"Cv5uuhkS4j8\",\"28JgVyUh3wI\",\"ZO_Dbu-owZ4\",\"Hjvhl3co5ao\",\"Z7cdxlMnCIw\",\"S7YQBX7lAk0\",\"TuYjN1Hpcwg\",\"nu5eT_ZbeQ8\",\"v4NA0NYyFOU\",\"4XeWngFZaa8\"],\"completed\":null,\"playtime\":223,\"title\":\"San Tropez\",\"sample\":\"sIW9QEIYIf4dHCbefpGhThyhM-OFI8zG4l-qVpXXB1I\"},{\"position\":5,\"id\":\"7299491314038468\",\"source\":[\"k3u5E8XKPjg\",\"UdEiH6BbiDQ\",\"NFa6c-eP8KM\",\"ArhM-sp4JpM\",\"hNp5US9fueU\",\"VqGXPpk7BgY\",\"a90vXD9Kllk\",\"bncUXjKA_QE\",\"jC1OTUbV3Sc\"],\"completed\":null,\"playtime\":134,\"title\":\"Seamus\",\"sample\":\"sIW9QEIYIf4dHCbefpGhTt_M69_UI9rrJSVvWL2-yAg\"},{\"position\":6,\"id\":\"13770043937124232\",\"source\":[\"rn7MmS3vazU\",\"KBca3xf-j3o\",\"53N99Nim6WE\",\"DtJgNvwmsRA\",\"whq0NBf4bDg\",\"BW9Kts3fo98\",\"zrProK5R7ms\",\"EMneCi9F_UQ\",\"XBd_ORFDjqE\"],\"completed\":null,\"playtime\":1414,\"title\":\"Echoes\",\"sample\":\"sIW9QEIYIf4dHCbefpGhToAf7E_1E-2MlBBPmPAXRBU\"}]}\n" + ", {\"metadata\":{\"art\":\"https://rovimusic.rovicorp.com/image.jpg?c=jrUeNdQgYctZeknQGUHTvASijaXJlYnq0St31qpAJWo=&f=2\",\"artId\":\"22257500795355378\",\"artist\":\"The Rolling Stones\",\"year\":\"1971\",\"album\":\"Sticky Fingers\",\"genre\":\"Pop/Rock\",\"playtime\":2783,\"directory\":\"C:\\\\Users\\\\byron\\\\Downloads\\\\Sticky Fingers\"},\"songs\":[{\"position\":1,\"id\":\"8214542415455898\",\"source\":[\"M9hcAA93N8c\",\"DOfDD2OYOZE\",\"59K2kF6o9Tk\",\"3B0Y3LUqr1Q\",\"E54l27TsvuU\",\"JmYx4lD0HCI\",\"aJQhCpLB05w\",\"hxXV2UftL7Q\",\"GuVax7iMM6Y\",\"Fmfi3UbDPnQ\"],\"completed\":null,\"playtime\":229,\"title\":\"Brown Sugar\",\"sample\":\"7-6M8VDJIXFNm9pb_GQdRB_TZlp6n_cq-Emr2zx15tU\"},{\"position\":2,\"id\":\"4904939135580537\",\"source\":[\"KIlVba5jtQE\",\"Rp45d9HF6-8\",\"hZ8Bc6jz-W8\",\"1Xk8ZLU6bYM\",\"wk0crTGCwfE\",\"ryWIbjsnndo\",\"Fswhc8ZvBcU\",\"Lez49-z3-3w\",\"GL3C7ZORsXE\",\"fj8Obro0cyo\",\"y0xbggIRxF8\"],\"completed\":null,\"playtime\":233,\"title\":\"Sway\",\"sample\":\"5naAaOTESjFJBKhSEU-p9lWnbEN5fCjifro6xhIBuB4\"},{\"position\":3,\"id\":\"6513209956026061\",\"source\":[\"jpc3V6VRQ9c\",\"hXjUl4R_15s\",\"OPfG2aU1MpI\",\"nY_GgPi5eCM\",\"UFLJFl7ws_0\",\"SQTHB4jM-KQ\",\"l0QATz8aEAc\",\"ZNaqBBjrIZw\",\"I7wip0eYQCU\"],\"completed\":null,\"playtime\":344,\"title\":\"Wild Horses\",\"sample\":\"Ia8qr-wZG82gw1tYGntlzoAf7E_1E-2MlBBPmPAXRBU\"},{\"position\":4,\"id\":\"8752068469603079\",\"source\":[\"ElnnE49absc\",\"3fa4HUiFJ6c\",\"Gz5mI6tqm_Q\",\"GxSIqb529s8\",\"CWE6LPCs4ns\",\"Q5uNKKjGpg8\",\"D6327WbFVe0\",\"CnxTj1HUKL8\",\"odsTngjJp-4\",\"2J-i9_SkdiI\",\"xxgJ5USKaZ8\",\"C1w4b7Kaexw\"],\"completed\":null,\"playtime\":436,\"title\":\"Can't You Hear Me Knocking\",\"sample\":\"ZcdZrf1Jx4SBuOBVFgoz2phUoDg0hsvx4F4sL4oO-nA\"},{\"position\":5,\"id\":\"7082588193354098\",\"source\":[\"RfibGc-VvAU\",\"n_zEDsRoUAM\",\"mUCoQryE7-k\",\"R4Uq6oTVqcs\",\"EILVwE4PCyE\",\"xCbAGsWMvZs\",\"58nT88kHj8Y\",\"HVwJILNmU80\"],\"completed\":null,\"playtime\":153,\"title\":\"You Gotta Move\",\"sample\":\"TGPWSoUZS1TKCXWyduxilphUoDg0hsvx4F4sL4oO-nA\"},{\"position\":6,\"id\":\"04353058846728597\",\"source\":[\"7cRfaaWMSPQ\",\"qtnxvpIEg8w\",\"To7q0cn0Fk4\",\"8fAzIxGPLN8\",\"NVfZYqYbkIw\",\"gTe-ylu2AuE\",\"a4g8PxsG_j4\",\"8A_xiUBRlEs\",\"RliRCSTI7Qo\"],\"completed\":null,\"playtime\":217,\"title\":\"Bitch\",\"sample\":\"Ukvk35Q8RfONmbp7nQFeiz6KsMttLlyBmmVTZ6_CLs0\"},{\"position\":7,\"id\":\"7483916513804753\",\"source\":[\"womdnUZy2Ho\",\"lR5DbMQgIOQ\",\"6VPrVV9ejIo\",\"c0j2fKiB9VM\",\"OT8ES0wHPH8\",\"lRRgXISnF00\",\"XyV-jkj0EDI\",\"CGrEbxe4KBQ\",\"QgWn1-7Y1rM\",\"PZwnYTt1kHk\",\"GaovG9IDn-E\",\"XeOuBXZeDZw\"],\"completed\":null,\"playtime\":235,\"title\":\"I Got the Blues\",\"sample\":\"GaJbiAtRCWElHtSyNHaU4FWnbEN5fCjifro6xhIBuB4\"},{\"position\":8,\"id\":\"519424803583297\",\"source\":[\"iCnx2kjk8T4\",\"lRVzlE_lPc8\",\"-TP9AvRIJsE\",\"Sfq_nFo2zg4\",\"C39kQoprfP0\",\"vqaC_p9km-Q\",\"gtz8qZz6s8s\",\"CdKoviNMmNE\",\"OpIqe5iCxmw\",\"VLri0DHk6A0\",\"Sgnrl0npF_U\",\"rdtM2YGaJ4k\",\"eKBzXxM3ODM\",\"fJGF8MRDMqo\"],\"completed\":null,\"playtime\":334,\"title\":\"Sister Morphine\",\"sample\":\"CjuS1W1cuJ-qAVQIsWyFV1WnbEN5fCjifro6xhIBuB4\"},{\"position\":9,\"id\":\"902038495465784\",\"source\":[\"-Aa5rfV3cuY\",\"MMnkeQ-Z5PA\",\"1KV4vuZiJGo\",\"_emq_NzO7H8\",\"D5f75EgYIhc\",\"yy0gRWUC2L4\",\"8YRdxHHFKvQ\",\"gRBNY6fubck\",\"uw6MCL9jOwY\",\"RS_yyRk_dj8\",\"4oPInSfh6H4\",\"wrsToLa3XEo\",\"-UaXI0Zaquw\",\"6Nd_l6kbxhU\",\"Rbl8i7PkS6M\"],\"completed\":null,\"playtime\":245,\"title\":\"Dead Flowers\",\"sample\":\"dCSR3jyXO7sapun7YOA2XgSijaXJlYnq0St31qpAJWo\"},{\"position\":10,\"id\":\"2610161105898329\",\"source\":[\"GUXMUenqQj0\",\"moQmARaiOEU\",\"Bq4Q69_mdZw\",\"hJxZoQqEn7Y\",\"7PXOHpBneEU\",\"S36wteMNUgA\",\"2VFpxC6tn40\",\"uuwP9gCe1Zk\",\"ugYzDqQtdHU\",\"K77xzmZhqAY\",\"aGEw5B850a4\",\"FHIR-wvkw0I\",\"Di6si3jd5cs\"],\"completed\":null,\"playtime\":357,\"title\":\"Moonlight Mile\",\"sample\":\"eQmmrIlyf8206julNblTLphUoDg0hsvx4F4sL4oO-nA\"}]}\n" + "]");
        } catch (JSONException e) {
            e.printStackTrace();
            System.exit(-1);
        }
         */
        if (downloadHistory.length() > 0) {

            double songCount = 0;
            try {
                for (int i = 0; i < downloadHistory.length(); i++)
                    songCount += downloadHistory.getJSONObject(i).getJSONArray("songs").length();
            } catch (JSONException e) {
                debug.error(Thread.currentThread(), "Failed to process JSON to calculate downloads song count.", e);
            }

            debug.trace(
                    Thread.currentThread(),
                    String.format(
                            "Found a download history of %s album%s and %.0f song%s.",
                            downloadHistory.length(),
                            downloadHistory.length() == 1 ? "" : "s",
                            songCount,
                            songCount == 1 ? "" : "s"
                    )
            );
        } else {
            debug.trace(Thread.currentThread(), "Did not any existing download history.");
        }
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
            for (int i = 0; i < downloadHistory.length(); i++) {

                JSONObject history = new JSONObject(downloadHistory.getJSONObject(i).toString());

                history.put("songs", new JSONArray());

                for (int j = 0; j < downloadHistory.getJSONObject(i).getJSONArray("songs").length(); j++) {

                    if (!downloadHistory.getJSONObject(i).getJSONArray("songs").getJSONObject(j).toString().equals(targetDeletion.toString()))
                        history.getJSONArray("songs").put(downloadHistory.getJSONObject(i).getJSONArray("songs").getJSONObject(j));

                }
                // Only save history provided it actually has songs, otherwise wasted space
                if (history.getJSONArray("songs").length() > 0)
                    newDownloadHistory.put(history);

            }

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