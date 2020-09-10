package musicdownloader.model;

import musicdownloader.controllers.Downloads;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import musicdownloader.utils.io.HistoryValidator;
import musicdownloader.utils.io.Downloader;
import musicdownloader.utils.io.Gzip;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// TODO: Download history should contain song position
public class Download {

    private volatile JSONArray downloadQueue = new JSONArray();
    private volatile JSONObject downloadObject = new JSONObject();
    private JSONArray downloadHistory = new JSONArray();

    private Downloads downloadsView = null;

    public Download() {
        refreshDownloadHistory();

        /*
        try {
            downloadObject = new JSONObject("{\"metadata\":{\"artId\":\"17907049007993958\",\"art\":\"https://rovimusic.rovicorp.com/image.jpg?c=jrUeNdQgYctZeknQGUHTvASijaXJlYnq0St31qpAJWo=&f=2\",\"artist\":\"The Rolling Stones\",\"year\":\"1971\",\"album\":\"Sticky Fingers\",\"genre\":\"Pop/Rock\",\"playtime\":2783,\"directory\":\"C:\\\\Users\\\\byron\\\\Downloads/Sticky Fingers\"},\"songs\":[{\"position\":1,\"id\":\"33711399776129547\",\"completed\":true,\"source\":[\"59K2kF6o9Tk\",\"3B0Y3LUqr1Q\",\"DOfDD2OYOZE\",\"M9hcAA93N8c\",\"Jusuz5IHxKs\",\"aJQhCpLB05w\",\"hxXV2UftL7Q\",\"GuVax7iMM6Y\",\"Fmfi3UbDPnQ\"],\"playtime\":229,\"title\":\"Brown Sugar\",\"sample\":\"7-6M8VDJIXFNm9pb_GQdRB_TZlp6n_cq-Emr2zx15tU\"},{\"position\":2,\"id\":\"3454861335571846\",\"completed\":true,\"source\":[\"KIlVba5jtQE\",\"hZ8Bc6jz-W8\",\"ryWIbjsnndo\",\"wk0crTGCwfE\",\"Rp45d9HF6-8\",\"1Xk8ZLU6bYM\",\"Fswhc8ZvBcU\",\"Lez49-z3-3w\",\"GL3C7ZORsXE\",\"fj8Obro0cyo\",\"y0xbggIRxF8\",\"ftk32FKD4Io\"],\"playtime\":233,\"title\":\"Sway\",\"sample\":\"5naAaOTESjFJBKhSEU-p9lWnbEN5fCjifro6xhIBuB4\"},{\"position\":3,\"id\":\"09511176574144486\",\"completed\":true,\"source\":[\"UFLJFl7ws_0\",\"SQTHB4jM-KQ\",\"hXjUl4R_15s\",\"jpc3V6VRQ9c\",\"OPfG2aU1MpI\",\"nY_GgPi5eCM\",\"l0QATz8aEAc\",\"_8hiJypuuPQ\",\"wM5ph_4CiJI\",\"ZNaqBBjrIZw\",\"I7wip0eYQCU\"],\"playtime\":344,\"title\":\"Wild Horses\",\"sample\":\"Ia8qr-wZG82gw1tYGntlzoAf7E_1E-2MlBBPmPAXRBU\"},{\"position\":4,\"id\":\"8043682929131708\",\"completed\":true,\"source\":[\"3fa4HUiFJ6c\",\"Gz5mI6tqm_Q\",\"CnxTj1HUKL8\",\"ElnnE49absc\",\"Q5uNKKjGpg8\",\"D6327WbFVe0\",\"RRY1rqqoPBg\",\"2J-i9_SkdiI\",\"CWE6LPCs4ns\",\"xxgJ5USKaZ8\",\"C1w4b7Kaexw\"],\"playtime\":436,\"title\":\"Can't You Hear Me Knocking\",\"sample\":\"ZcdZrf1Jx4SBuOBVFgoz2phUoDg0hsvx4F4sL4oO-nA\"},{\"position\":5,\"id\":\"28296725835868275\",\"completed\":false,\"source\":[\"mUCoQryE7-k\",\"RfibGc-VvAU\",\"n_zEDsRoUAM\",\"xCbAGsWMvZs\",\"R4Uq6oTVqcs\",\"EILVwE4PCyE\",\"enCEVCYwXMQ\",\"58nT88kHj8Y\"],\"playtime\":153,\"title\":\"You Gotta Move\",\"sample\":\"TGPWSoUZS1TKCXWyduxilphUoDg0hsvx4F4sL4oO-nA\"},{\"position\":6,\"id\":\"20171001542798683\",\"completed\":false,\"source\":[\"a4g8PxsG_j4\",\"To7q0cn0Fk4\",\"7cRfaaWMSPQ\",\"8fAzIxGPLN8\",\"qtnxvpIEg8w\",\"8A_xiUBRlEs\",\"q4VtSLVTbUM\",\"RliRCSTI7Qo\"],\"playtime\":217,\"title\":\"Bitch\",\"sample\":\"Ukvk35Q8RfONmbp7nQFeiz6KsMttLlyBmmVTZ6_CLs0\"},{\"position\":7,\"id\":\"1931005384016522\",\"completed\":false,\"source\":[\"lR5DbMQgIOQ\",\"c0j2fKiB9VM\",\"XyV-jkj0EDI\",\"OT8ES0wHPH8\",\"6VPrVV9ejIo\",\"womdnUZy2Ho\",\"PZwnYTt1kHk\",\"GaovG9IDn-E\",\"3kbwC0b1vsY\",\"XeOuBXZeDZw\",\"bKhCgMNsfAM\",\"Wy8fqr2JHB0\"],\"playtime\":235,\"title\":\"I Got the Blues\",\"sample\":\"GaJbiAtRCWElHtSyNHaU4FWnbEN5fCjifro6xhIBuB4\"},{\"position\":8,\"id\":\"5477198851560452\",\"completed\":false,\"source\":[\"C39kQoprfP0\",\"-TP9AvRIJsE\",\"iCnx2kjk8T4\",\"Y9eJQ9xo8DA\",\"vqaC_p9km-Q\",\"CdKoviNMmNE\",\"Sfq_nFo2zg4\",\"gtz8qZz6s8s\",\"7YQOznLd8Sk\",\"OpIqe5iCxmw\",\"dsv--5JTyxQ\",\"OHlmDBSqoR8\",\"sgCjOWGlVMs\",\"Sgnrl0npF_U\",\"rdtM2YGaJ4k\",\"eKBzXxM3ODM\",\"fJGF8MRDMqo\"],\"playtime\":334,\"title\":\"Sister Morphine\",\"sample\":\"CjuS1W1cuJ-qAVQIsWyFV1WnbEN5fCjifro6xhIBuB4\"},{\"position\":9,\"id\":\"25298944228711406\",\"completed\":false,\"source\":[\"8YRdxHHFKvQ\",\"RS_yyRk_dj8\",\"1KV4vuZiJGo\",\"-Aa5rfV3cuY\",\"yy0gRWUC2L4\",\"uw6MCL9jOwY\",\"_emq_NzO7H8\",\"D5f75EgYIhc\",\"bll8M_vFbzM\",\"uJCHlbi7Z1I\",\"MMnkeQ-Z5PA\",\"gRBNY6fubck\",\"4oPInSfh6H4\",\"wrsToLa3XEo\"],\"playtime\":245,\"title\":\"Dead Flowers\",\"sample\":\"dCSR3jyXO7sapun7YOA2XgSijaXJlYnq0St31qpAJWo\"},{\"position\":10,\"id\":\"2576752732293798\",\"completed\":false,\"source\":[\"Bq4Q69_mdZw\",\"ugYzDqQtdHU\",\"7PXOHpBneEU\",\"uuwP9gCe1Zk\",\"hJxZoQqEn7Y\",\"aGEw5B850a4\",\"moQmARaiOEU\",\"GUXMUenqQj0\",\"K77xzmZhqAY\",\"2VFpxC6tn40\",\"FHIR-wvkw0I\"],\"playtime\":357,\"title\":\"Moonlight Mile\",\"sample\":\"eQmmrIlyf8206julNblTLphUoDg0hsvx4F4sL4oO-nA\"}]}");
            downloadQueue = new JSONArray("[{\"metadata\":{\"artId\":\"45227701965436196\",\"art\":\"https://rovimusic.rovicorp.com/image.jpg?c=EUokLyfMctShPK4In1fAA1WnbEN5fCjifro6xhIBuB4=&f=2\",\"artist\":\"Pink Floyd\",\"year\":\"1971\",\"album\":\"Meddle\",\"genre\":\"Pop/Rock\",\"playtime\":2806,\"directory\":\"C:\\\\Users\\\\byron\\\\Downloads/Meddle\"},\"songs\":[{\"position\":1,\"id\":\"9693683330653847\",\"completed\":null,\"source\":[\"48PJGVf4xqk\",\"ikMAH7k3pz4\",\"s-CSfikHGsY\",\"_KXZRcZaQJ4\",\"D1kZ6M2aMvw\",\"DQvG2SMVl84\",\"YZCqh0beUwM\",\"YDVAQI-4lto\",\"TOa7IYotms8\",\"Vb7JhpD2g4k\",\"BgVYyQD05uc\",\"ABPQdXertbs\",\"nXaXKfyI7tQ\"],\"playtime\":355,\"title\":\"One of These Days\",\"sample\":\"sIW9QEIYIf4dHCbefpGhTj6KsMttLlyBmmVTZ6_CLs0\"},{\"position\":2,\"id\":\"1820687573487858\",\"completed\":null,\"source\":[\"uThZ1uuLLWM\",\"wFt99jcazHs\",\"wzqbesV1Z_o\",\"1a2hvqLPbSE\",\"4f6-akNjeto\",\"wQ712S4LYpU\",\"sctJuNYo59s\",\"oa07CsSwMqs\",\"Rj3kDOIFnMI\",\"xAiFyim8YnQ\",\"8dLuKGpdHqc\",\"rKMmMclXo2g\",\"7Ozh3-DjJNA\",\"9eAB4H74vfw\"],\"playtime\":312,\"title\":\"A Pillow of Winds\",\"sample\":\"sIW9QEIYIf4dHCbefpGhTh_TZlp6n_cq-Emr2zx15tU\"},{\"position\":3,\"id\":\"23970366658240128\",\"completed\":null,\"source\":[\"TeyHPAdxuy0\",\"sl_apx8JoMw\",\"t3J_2R9rAp8\",\"kAhbGzvB2gU\",\"UvPiv7F1lfE\",\"L8GjXdizFs4\",\"uCgQuj8v2gg\",\"1b8T2keXcCI\",\"hE_SCDY5DhY\",\"MEemQey1iew\",\"cPL7chgv0Ig\"],\"playtime\":368,\"title\":\"Fearless\",\"sample\":\"sIW9QEIYIf4dHCbefpGhTg4Q1ghY8VaPylnm7PwcKNY\"},{\"position\":4,\"id\":\"5521330515946635\",\"completed\":null,\"source\":[\"Z7cdxlMnCIw\",\"Cv5uuhkS4j8\",\"28JgVyUh3wI\",\"KkRcwqxLEuk\",\"Hjvhl3co5ao\",\"S7YQBX7lAk0\",\"w38dkc4dFIQ\",\"ZO_Dbu-owZ4\",\"TuYjN1Hpcwg\",\"v4NA0NYyFOU\"],\"playtime\":223,\"title\":\"San Tropez\",\"sample\":\"sIW9QEIYIf4dHCbefpGhThyhM-OFI8zG4l-qVpXXB1I\"},{\"position\":5,\"id\":\"3745484598396195\",\"completed\":null,\"source\":[\"k3u5E8XKPjg\",\"NFa6c-eP8KM\",\"a90vXD9Kllk\",\"5Wrhz1AbFI0\",\"UdEiH6BbiDQ\",\"bncUXjKA_QE\",\"jC1OTUbV3Sc\"],\"playtime\":134,\"title\":\"Seamus\",\"sample\":\"sIW9QEIYIf4dHCbefpGhTt_M69_UI9rrJSVvWL2-yAg\"},{\"position\":6,\"id\":\"9850610028411921\",\"completed\":null,\"source\":[\"KBca3xf-j3o\",\"DtJgNvwmsRA\",\"53N99Nim6WE\",\"TL6AJcjijts\",\"rn7MmS3vazU\",\"_Xn_s8Wh8jQ\",\"WxXgzwhHCuU\",\"BW9Kts3fo98\",\"zrProK5R7ms\",\"EMneCi9F_UQ\"],\"playtime\":1414,\"title\":\"Echoes\",\"sample\":\"sIW9QEIYIf4dHCbefpGhToAf7E_1E-2MlBBPmPAXRBU\"}]},{\"metadata\":{\"artId\":\"6987353904274337\",\"art\":\"https://rovimusic.rovicorp.com/image.jpg?c=E84Hy2uHRw_tf4V3aaADCR_TZlp6n_cq-Emr2zx15tU=&f=2\",\"artist\":\"The Beatles\",\"year\":\"1966\",\"album\":\"Revolver\",\"genre\":\"Pop/Rock\",\"playtime\":2080,\"directory\":\"C:\\\\Users\\\\byron\\\\Downloads/Revolver\"},\"songs\":[{\"position\":1,\"id\":\"22132307792280614\",\"completed\":null,\"source\":[\"l0zaebtU-CA\",\"CAcp83s1KN8\",\"MvtPf_YfOUM\",\"0DGn7eUU4kA\",\"MbQiVQuiu04\",\"pg7hggiyQAA\",\"yYvkICbTZIQ\",\"Itux8wt0Pew\"],\"playtime\":158,\"title\":\"Taxman\",\"sample\":\"EbBKNySS87jT53x6jGbREphUoDg0hsvx4F4sL4oO-nA\"},{\"position\":2,\"id\":\"3940474646870702\",\"completed\":null,\"source\":[\"HuS5NuXRb5Y\",\"6gluNoLVKiQ\",\"O5eMM2yjg98\",\"fZA6jtxtTfQ\",\"3nZ3QIKOMlk\",\"QSO09M4UVY0\",\"wMXdJhac6LQ\",\"VzzN4hvHXho\",\"twFbweJfUUo\",\"eT98yd84iCE\",\"ojpSiNZA5_0\"],\"playtime\":126,\"title\":\"Eleanor Rigby\",\"sample\":\"nXtvv1aaNyqf439yerywNR_TZlp6n_cq-Emr2zx15tU\"},{\"position\":3,\"id\":\"34290919618683346\",\"completed\":null,\"source\":[\"BT5j9OQ7Sh0\",\"bfDrF9s27ec\",\"vv9S5guPWJw\",\"GiOEXpbsSSk\",\"FjeMxLm0vCA\",\"gRbim4Qmmw4\",\"FQdNuGW_Q6A\",\"Wu1VzWLJC6A\",\"cBHAdhTu_3o\",\"vQfNk4FR5Ng\",\"9Cep4U8SgZI\",\"6pAAnpVr9mw\"],\"playtime\":180,\"title\":\"I'm Only Sleeping\",\"sample\":\"nXtvv1aaNyqf439yerywNQ4Q1ghY8VaPylnm7PwcKNY\"},{\"position\":4,\"id\":\"3171104005810299\",\"completed\":null,\"source\":[\"s1X-q7MweIc\",\"f2XNWIM0zWc\",\"Mki1p2Fbr28\",\"cY3Jn5twwSA\",\"Kgwun99jdG4\"],\"playtime\":179,\"title\":\"Love You To\",\"sample\":\"nXtvv1aaNyqf439yerywNRyhM-OFI8zG4l-qVpXXB1I\"},{\"position\":5,\"id\":\"40194464338542346\",\"completed\":null,\"source\":[\"xdcSFVXd3MU\",\"CHLQs6u9wXw\",\"iELGhAGwBdc\",\"Mrmm7Bk8rMM\",\"HMAf4Uq9mrs\",\"0Whz1jIEBI0\",\"4CwPRyVXy_k\",\"hUv0x4iM_QU\",\"oMO38zIaUDg\",\"sTJJIBuqs9o\"],\"playtime\":144,\"title\":\"Here, There and Everywhere\",\"sample\":\"nXtvv1aaNyqf439yerywNd_M69_UI9rrJSVvWL2-yAg\"},{\"position\":6,\"id\":\"8135686835344816\",\"completed\":null,\"source\":[\"ZhxJAxa77sE\",\"ztHxu-13UqI\",\"7i1XD2yN4Ug\",\"m2uTFF_3MaA\",\"FZLIcQf47TQ\",\"vRuPcNJ-cwg\",\"2Q_ZzBGPdqE\"],\"playtime\":158,\"title\":\"Yellow Submarine\",\"sample\":\"nXtvv1aaNyqf439yerywNYAf7E_1E-2MlBBPmPAXRBU\"},{\"position\":7,\"id\":\"5544818255318288\",\"completed\":null,\"source\":[\"rLzfo59AdEc\",\"nsd6JVAXI3Q\",\"7gNAg24OQYY\",\"NleTIoDZZok\",\"Ru_A68I9rtY\",\"J0wap653lh8\",\"WrAV5EVI4tU\",\"OIejhPfOIbw\"],\"playtime\":156,\"title\":\"She Said She Said\",\"sample\":\"nXtvv1aaNyqf439yerywNQSijaXJlYnq0St31qpAJWo\"},{\"position\":8,\"id\":\"9342991202704771\",\"completed\":null,\"source\":[\"6e01nNA02vw\",\"CXbCt4qrEiY\",\"NvPNYs7Baps\",\"90Ia3MRv8xk\",\"plmnpVBszj4\",\"SL049AnA2FY\",\"7A60mE1l-No\",\"B2RhkxrJlw4\",\"04AEXrIgx9Q\",\"ApabZjssy9w\"],\"playtime\":129,\"title\":\"Good Day Sunshine\",\"sample\":\"nXtvv1aaNyqf439yerywNVWnbEN5fCjifro6xhIBuB4\"},{\"position\":9,\"id\":\"7058203361130494\",\"completed\":null,\"source\":[\"Uq0aeEYLkIE\",\"fH-3y_Ike5E\",\"6-u4G4sDEGg\",\"Wxf9IZmtDYE\",\"QNLI4mj9VYs\",\"XgV6gXKI3zQ\",\"sSDQ8rShTlo\",\"148BZKiVaBg\",\"oSR3iEiBeKI\",\"bf22VR71ags\"],\"playtime\":120,\"title\":\"And Your Bird Can Sing\",\"sample\":\"nXtvv1aaNyqf439yerywNZhUoDg0hsvx4F4sL4oO-nA\"},{\"position\":10,\"id\":\"44204014383012613\",\"completed\":null,\"source\":[\"ELlLIwhvknk\",\"HuphFPEqJqw\",\"HolwxNyJKhY\",\"fNahNa6qrNw\",\"cbJrXqrLtMw\",\"h47dYMwlvjg\",\"pOzXnC5ogVk\",\"OsITn49Ybw0\"],\"playtime\":119,\"title\":\"For No One\",\"sample\":\"HbzL5GxSy1NVDc_JLCqJxSpQg_7iAU1wjqLgK_xGXts\"},{\"position\":11,\"id\":\"9647114947879768\",\"completed\":null,\"source\":[\"Tb9L3iAUhc0\",\"niuNlPo1q9M\",\"Pfjs0fdTf4E\",\"ErVHzW2pgfg\",\"ysUeRMuH-WU\",\"KYnV6910Ktk\",\"8AKk7V7tuFs\",\"M88Q7Py-Mkw\",\"LPLnytJjIHM\",\"qPbtVlQCf-c\",\"KVrohyEDKKg\",\"PtdRJNkNtUQ\",\"IFCchgn5FW8\"],\"playtime\":134,\"title\":\"Doctor Robert\",\"sample\":\"HbzL5GxSy1NVDc_JLCqJxR_TZlp6n_cq-Emr2zx15tU\"},{\"position\":12,\"id\":\"913168297891899\",\"completed\":null,\"source\":[\"7kXusIyqQ2o\",\"qg9xSo-WEGc\",\"1r4QsLUGS4M\",\"f6nsMPKaLtw\",\"v1HDt1tknTc\",\"oZP0eypvV9I\",\"SlMkvfFRVqk\",\"jenWdylTtzs\",\"bztiAcsATyI\",\"YBxFyzSE1pc\"],\"playtime\":147,\"title\":\"I Want to Tell You\",\"sample\":\"aE1qmMYDnUDzgaggZp95ST6KsMttLlyBmmVTZ6_CLs0\"},{\"position\":13,\"id\":\"7584619471944563\",\"completed\":null,\"source\":[\"r95-7zfgtLw\",\"yJ8WI3Q9jm4\",\"QKMAZxaFuS0\",\"CSL73EupKHQ\",\"aGdyvj58B_U\",\"5HNNPkdbr9s\",\"4I3q75jicm4\",\"BshpGqesjAk\",\"cbkgciF3KiY\"],\"playtime\":149,\"title\":\"Got to Get You into My Life\",\"sample\":\"nXtvv1aaNyqf439yerywNSpQg_7iAU1wjqLgK_xGXts\"},{\"position\":14,\"id\":\"9785788308289151\",\"completed\":null,\"source\":[\"pHNbHn3i9S4\",\"7UjvdZm-Tu8\",\"zoD-llVXosc\",\"Ag58k2elaYs\",\"W-gVpNJ2Qts\",\"AvLIgGxM_so\",\"Ah2ckzXgrx4\",\"T8k0fYZ3uzU\",\"YtmukKLZQUw\"],\"playtime\":181,\"title\":\"Tomorrow Never Knows\",\"sample\":\"nXtvv1aaNyqf439yerywNT6KsMttLlyBmmVTZ6_CLs0\"}]}\n" + "]\n");
            downloadHistory = new JSONArray("[{\"metadata\":{\"artId\":\"9640223051641675\",\"art\":\"https://rovimusic.rovicorp.com/image.jpg?c=Z0qKJXmWdBsymli0PdKW5oAf7E_1E-2MlBBPmPAXRBU=&f=2\",\"artist\":\"Cream\",\"year\":\"1968\",\"album\":\"Wheels of Fire\",\"genre\":\"Pop/Rock, Blues\",\"playtime\":4982,\"directory\":\"C:\\\\Users\\\\byron\\\\Downloads/Wheels of Fire\",\"downloadStarted\":1598132423010, \"format\": \"mp3\"},\"songs\":[{\"id\":\"8288321405064201\",\"title\":\"White Room\"},{\"id\":\"10525361208743511\",\"title\":\"Sitting on Top of the World\"},{\"id\":\"5042087168379529\",\"title\":\"Passing the Time (Long Version)\"},{\"id\":\"7812204949292395\",\"title\":\"As You Said\"},{\"id\":\"029288185574550485\",\"title\":\"Pressed Rat and Warthog\"},{\"id\":\"21299825946086792\",\"title\":\"Politician\"},{\"id\":\"9688650012665181\",\"title\":\"Those Were the Days\"},{\"id\":\"7760383678888054\",\"title\":\"Born Under a Bad Sign\"},{\"id\":\"11470523539574473\",\"title\":\"Deserted Cities of the Heart\"},{\"id\":\"2186954897778961\",\"title\":\"Anyone For Tennis\"},{\"id\":\"3040495740471101\",\"title\":\"Crossroads\"},{\"id\":\"525300232924241\",\"title\":\"Spoonful\"},{\"id\":\"5451748413773023\",\"title\":\"Traintime\"},{\"id\":\"4344354967993683\",\"title\":\"Toad\"}]}]\n");

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
            new Downloader();

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

    public synchronized void deleteQueue(JSONObject targetDeletion) {
        // Adding all to history except history item to remove
        try {

            this.downloadQueue = jsonArrayRemoval(targetDeletion, downloadQueue);

        } catch (JSONException e) {
            Debug.error("Failed to validate download queue to remove element.", e);
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
        Gzip.compressData(
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
                    Gzip.decompressFile(
                            new File(
                                    Resources.getInstance().getApplicationData() + "json/downloads.gz"
                            )
                    ).toString()
            );

            if (diskHistory.toString().equals(new JSONObject().toString())) {
                this.downloadHistory = new JSONArray();
            } else {
                HistoryValidator historyValidator = new HistoryValidator(diskHistory);

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
}