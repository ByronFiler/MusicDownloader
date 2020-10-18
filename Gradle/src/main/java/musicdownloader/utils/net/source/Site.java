package musicdownloader.utils.net.source;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.concurrent.CountDownLatch;

public abstract class Site implements Runnable {

    protected final String query;
    protected final int targetTime;

    protected final JSONObject results = new JSONObject();
    protected int retries = 5;

    protected Document requestedPage = null;
    protected CountDownLatch latch;

    public Site(String query, int targetTime) {
        try {
            results.put("primary", new JSONArray());
            results.put("secondary", new JSONArray());
        } catch (JSONException ignored) {}

        this.query = query;
        this.targetTime = targetTime;

    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public JSONObject getResults() {
        return results;
    }

    protected int timeConversion(String stringTime) {
        String[] songDataBreak = stringTime.split(":");

        int songLenSec = 0;

        for (int i = songDataBreak.length-1; i >= 0; i--)
            songLenSec += (Double.parseDouble(songDataBreak[Math.abs(i - songDataBreak.length + 1)]) * Math.pow(60, i));

        return songLenSec;
    }

}
