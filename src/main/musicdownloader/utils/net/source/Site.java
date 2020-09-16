package musicdownloader.utils.net.source;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class Site {

    protected final String query;
    protected final int targetTime;

    protected final JSONObject results = new JSONObject("{\"primary\": [], \"secondary\": []}");
    protected int retries = 5;

    protected Document requestedPage = null;

    public Site(String query, int targetTime) throws JSONException {

        this.query = query;
        this.targetTime = targetTime;

    }

    protected int timeConversion(String stringTime) {
        String[] songDataBreak = stringTime.split(":");

        int songLenSec = 0;

        for (int i = songDataBreak.length-1; i >= 0; i--)
            songLenSec += (Double.parseDouble(songDataBreak[Math.abs(i - songDataBreak.length + 1)]) * Math.pow(60, i));

        return songLenSec;
    }

    public JSONObject getResults() {
        return results;
    }

}
