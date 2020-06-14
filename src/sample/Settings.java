package sample;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Settings {

    public Settings() {}

    public synchronized static JSONObject getSettings() {

        try {

            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(new FileReader("resources\\json\\config.json"));

        } catch (IOException | ParseException e) {

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

    public synchronized static String getVersion() {

        try {
            JSONParser parser = new JSONParser();
            JSONObject jo = (JSONObject) parser.parse(new FileReader("resources\\json\\meta.json"));

            return (String) jo.get("version");

        } catch (IOException e) {
            // Verify hash to versions and calculate that
            System.exit(-1);

            return "-1";

        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(-1);

            return "-1";
        }

    }

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

    public synchronized static String getLatestVersion() {

        try {
            JSONParser parser = new JSONParser();
            Document githubRequestLatestVersion = Jsoup.connect("https://raw.githubusercontent.com/ByronFiler/MusicDownloader/master/resources/json/meta.json").get();
            JSONObject jsonData = (JSONObject) parser.parse(githubRequestLatestVersion.text());

            return (String) jsonData.get("version");

        } catch (IOException | ParseException e) {
            return "-1";
        }

    }

    public synchronized static boolean checkYouTubeDl() {

        try {

            // Will throw an error if not bound
            Runtime.getRuntime().exec(new String[]{"youtube-dl"});
            return true;

        } catch (IOException e) {
            return false;
        }

    }

    public synchronized static boolean checkFFMPEG() {

        try {

            // Will throw an error if not bound to command
            Runtime.getRuntime().exec(new String[]{"ffmpeg"});
            return true;

        } catch (IOException e) {

            return false;

        }

    }

    public synchronized static void saveSettings(String output_directory, int music_format, int save_album_art, int album_art, int album_title, int song_title, int artist, int year, int track, int theme, int data_saver) {
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
            configFile.write(newSettings.toJSONString());
            configFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

}