package sample;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;

public class Settings {

    public Settings() {}

    public synchronized JSONObject getSettings() {

        try {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(new FileReader("resources\\json\\config.json"));

        } catch (IOException | ParseException e) {
            // Generate the file with default datasets

            if (!resetSettings()) {
                e.printStackTrace();
                System.exit(-1);
            }

        }

        JSONObject defaultResponse = new JSONObject();
        defaultResponse.put("output_directory", "");
        defaultResponse.put("music_format", 0);
        defaultResponse.put("save_album_art", 0);

        return defaultResponse;

    }

    public synchronized String getVersion() throws IOException, ParseException {

        try {
            JSONParser parser = new JSONParser();
            JSONObject jo = (JSONObject) parser.parse(new FileReader("resources\\json\\config.json"));
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

    public synchronized boolean resetSettings() {

        try {
            FileWriter newConfig = new FileWriter("resources\\json\\config.json");
            newConfig.write("{\"output_directory\": \"\", \"music_format\": 0, \"save_album_art\": 0}");
            newConfig.close();

            return true;

        } catch (IOException e) {
            e.printStackTrace();

            return false;

        }

    }

}
