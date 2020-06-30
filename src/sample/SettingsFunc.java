package sample;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class SettingsFunc {

    public SettingsFunc() {}

    // Not moved to latest, but should be?
    public synchronized static JSONObject getSettings() {

        try {

            return new JSONObject(new Scanner(new File("resources\\json\\config.json")).useDelimiter("\\Z").next());

        } catch (IOException | JSONException e) {

            // Generate the file with default datasets
            Debug.trace(null, "Error loading settings, resetting.");

            if (resetSettings()) {

                return getSettings();

            } else {

                Debug.error(null, "Error resetting settings", new IOException().getStackTrace());
                System.exit(-1);

            }


        }

        return new JSONObject();

    }

    // Not moved to latest, but should be?
    public synchronized static String getVersion() {

        try {
            return (String) new JSONObject(new Scanner(new File("resources\\json\\meta.json")).useDelimiter("\\Z").next()).get("version");

        } catch (IOException | JSONException e) {
            return null;
        }

    }

    // Not moved to latest, but should be?
    public synchronized static boolean resetSettings() {

        try {
            FileWriter newConfig = new FileWriter("resources\\json\\config.json");
            newConfig.write("{\"output_directory\":\"\",\"save_album_art\":1,\"music_format\":0, \"album_art\":1, \"album_title\":1, \"song_title\":1, \"artist\":1, \"year\":1, \"track\":1,\"theme\":0, \"data_saver\":0}");
            newConfig.close();

            return true;

        } catch (IOException e) {
            e.printStackTrace();

            return false;

        }

    }

    // TODO: Only used in old, new is built into thread class
    public synchronized static String getLatestVersion() {

        try {

            Document githubRequestLatestVersion = Jsoup.connect("https://raw.githubusercontent.com/ByronFiler/MusicDownloader/master/resources/json/meta.json").get();
            JSONObject jsonData = new JSONObject(githubRequestLatestVersion.text());
            return (String) jsonData.get("version");

        } catch (IOException | JSONException e) {
            Debug.trace(null, "Failed to get latest version");
            return null;
        }

    }

    // TODO: Only used in old, new is built into thread class
    public synchronized static boolean checkYouTubeDl() {

        try {
            // Will throw an error if not bound
            Runtime.getRuntime().exec(new String[]{"youtube-dl"});
            return true;

        } catch (IOException e) {
            return false;
        }

    }

    // TODO: Only used in old, new is built into thread class
    public synchronized static boolean checkFFMPEG() {

        try {

            // Will throw an error if not bound to command
            Runtime.getRuntime().exec(new String[]{"ffmpeg"});
            return true;

        } catch (IOException e) {

            return false;

        }

    }

    // Not moved to latest, but should be?
    public synchronized static void saveSettings(String output_directory, int music_format, int save_album_art, int album_art, int album_title, int song_title, int artist, int year, int track, int theme, int data_saver) throws JSONException {

        // Could move it to just formatting a string
        JSONObject newSettings = new JSONObject();
        newSettings.put("output_directory", output_directory);
        newSettings.put("music_format", music_format);
        newSettings.put("save_album_art", save_album_art);
        newSettings.put("album_art", album_art);
        newSettings.put("album_title", album_title);
        newSettings.put("song_title", song_title);
        newSettings.put("artist", artist);
        newSettings.put("year", year);
        newSettings.put("track", track);
        newSettings.put("theme", theme);
        newSettings.put("data_saver", data_saver);

        try {
            FileWriter configFile = new FileWriter("resources\\json\\config.json");
            configFile.write(newSettings.toString());
            configFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

}