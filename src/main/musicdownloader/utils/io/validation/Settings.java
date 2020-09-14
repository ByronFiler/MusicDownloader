package musicdownloader.utils.io.validation;

import musicdownloader.utils.app.Debug;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Settings {

    private static final String[] validBooleans = new String[]{
            "data_saver",
            "artist",
            "year",
            "dark_theme",
            "album_art",
            "advanced_validation",
            "song_title",
            "volume_correction",
            "track",
            "album_title"
    };

    private static final String[] validStrings = new String[]{
            "output_directory"
    };

    private static JSONArray validInts;
    static {
        try {
            validInts = new JSONArray(
                    "[" +
                            "{\"var\": \"music_format\", \"min\": 0, \"max\": 3}," +
                            "{\"var\": \"save_album_art\", \"min\": 0, \"max:\": 3}" +
                    "]"
            );
        } catch (JSONException e) {
            Debug.error("Failed to create default valid int settings.", e);
        }
    }

    // TODO: Does not work
    public static boolean validate(JSONObject settings) {

        if (settings.length() != (validBooleans.length + validStrings.length + validInts.length())) return false;

        try {
            for (String validBoolean : validBooleans) {
                if (!settings.has(validBoolean) || (settings.has(validBoolean) && settings.get(validBoolean) == null)) {
                    return false;
                }
            }

            for (String validString: validStrings) if (!settings.has(validString) || (settings.has(validString) && !settings.get(validString).getClass().equals(String.class))) return false;

            for (int i = 0; i < validInts.length(); i++)
                if (!settings.has(validInts.getJSONObject(i).getString("var"))
                        || settings.getInt(validInts.getJSONObject(i).getString("var")) < validInts.getJSONObject(i).getInt("min")
                        || settings.getInt(validInts.getJSONObject(i).getString("var")) > validInts.getJSONObject(i).getInt("max")
                ) return false;

        } catch (JSONException e) {
            return false;
        }

        return true;

    }

}
