package musicdownloader.utils.io;

import musicdownloader.utils.app.Debug;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class HistoryValidator {

    private static final String[] metadataEssential = new String[]{"artId", "artist", "album", "format", "directory"};
    private static final String[] metadataExtra = new String[]{"art", "year", "genre", "playtime", "downloadStarted"};
    private static final String[] songEssential = new String[]{"id", "source", "title", "position"};

    private final JSONArray validatedHistory = new JSONArray();

    private final int modifiedRemovedHistories;
    private int historiesWithPartialMetadata = 0;
    private int modifiedRemovedSongs = 0;

    public HistoryValidator(JSONArray history) throws JSONException {

        JSONObject validatedHistory;

        for (int i = 0; i < history.length(); i++) {

            validatedHistory = downloadHistory(history.getJSONObject(i));

            if (!validatedHistory.toString().equals(new JSONObject().toString())) this.validatedHistory.put(validatedHistory);

        }

        this.modifiedRemovedHistories = history.length() - this.validatedHistory.length();



    }

    public synchronized int getModifiedRemovedHistories() {
        return modifiedRemovedHistories;
    }

    public synchronized int getHistoriesWithPartialMetadata() {
        return historiesWithPartialMetadata;
    }

    public synchronized int getModifiedRemovedSongs() {
        return modifiedRemovedSongs;
    }

    public synchronized JSONArray getValidatedHistory() {
        return validatedHistory;
    }

    private JSONObject downloadHistory(JSONObject history) {

        if (history.has("metadata")) {

            try {
                if (Arrays.stream(metadataEssential).mapToInt(e -> {
                    try {
                        return history.getJSONObject("metadata").has(e) ? 1 : 0;
                    } catch (JSONException er) {
                        Debug.error("Metadata exists in JSONObject but is not accessible.", er);
                        return 1;
                    }
                }).sum() == metadataEssential.length) {

                    JSONArray songs = new JSONArray();
                    for (int i = 0; i < history.getJSONArray("songs").length(); i++) {

                        int finalI = i;

                        if (Arrays.stream(songEssential).mapToInt(e -> {
                            try {
                                return history.getJSONArray("songs").getJSONObject(finalI).has(e) ? 1 : 0;
                            } catch (JSONException er) {
                                Debug.error("Failed to parse songs when iterate through.", er);
                                return 0;
                            }
                        }).sum() == songEssential.length) songs.put(history.getJSONArray("songs").getJSONObject(i));


                    }

                    if (
                            Arrays.stream(metadataExtra).mapToInt(e -> {
                                try {
                                    return history.getJSONObject("metadata").has(e) ? 0 : 1;
                                } catch (JSONException er) {
                                    Debug.error("Failed to parse metadata to determine non essential metadata.", er);
                                    return 0;
                                }
                            }).sum() > 0
                    ) historiesWithPartialMetadata++;

                    modifiedRemovedSongs += history.getJSONArray("songs").length() - songs.length();

                    if (songs.length() == 0) return new JSONObject();


                } else {Debug.trace("failed to validate meta"); return new JSONObject();}

            } catch (JSONException e) {
                Debug.warn("History object in validation has metadata but accessing this threw a JSONException, review: " + history.toString());
                return new JSONObject();
            }

        } else return new JSONObject();

        return history;

    }

}
