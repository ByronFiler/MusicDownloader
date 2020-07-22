package sample.model;

import org.json.JSONException;
import org.json.JSONObject;
import sample.Debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class settings {

    private JSONObject settings;
    private JSONObject defaultSettings;
    private String version;

    public settings() {

        // Declare default settings for reference
        try{
            defaultSettings = new JSONObject("{\"advanced_validation\": true, \"output_directory\":\"\",\"save_album_art\":0,\"music_format\":0, \"album_art\":true, \"album_title\":true, \"song_title\":true, \"artist\":true, \"year\":true, \"track\":true,\"dark_theme\":false, \"data_saver\":false}");
        } catch (JSONException ignored) {}

        // Load users actual settings
        try {
            settings = new JSONObject(new Scanner(new File("usr\\json\\config.json")).useDelimiter("\\Z").next());
        } catch (FileNotFoundException | JSONException ignored) {
            Debug.warn(null, "Failed to load user settings.");
            settings = defaultSettings;
            resetSettings();
        }

        // Load the version
        try {
            version = new JSONObject(new Scanner(new File("src/sample/app/meta.json")).useDelimiter("\\Z").next()).get("version").toString();
        } catch (JSONException | FileNotFoundException e) {
            Debug.warn(null, "Failed to locate version.");
            version = null;
        }

    }

    private void resetSettings() {

        try {

            FileWriter newConfig = new FileWriter("usr\\json\\config.json");
            newConfig.write(defaultSettings.toString());
            newConfig.close();

        } catch (IOException e) {

            // Attempt to see if this was due to folders not working or a system IO error
            if (resetDirectories())
                resetSettings();
            else
                Debug.error(null, "Failed to reset settings.", e.getCause());
        }
    }

    private boolean resetDirectories() {

        boolean wasUseful = false;

        // Checking for non existing folders
        if (!Files.exists(Paths.get("usr\\cached"))) {
            wasUseful = true;
            if (!new File("usr\\cached").mkdirs())
                Debug.error(null, "Failed to create non existing directory: usr\\cached", null);
        }

        if (!Files.exists(Paths.get("usr\\json"))) {
            wasUseful = true;
            if (!new File("usr\\json").mkdirs())
                Debug.error(null, "Failed to create non existing directory: usr\\json", null);
        }

        return wasUseful;
    }

    public void saveSettings(JSONObject settings) {
        try {
            FileWriter settingsFile = new FileWriter("usr/json/config.json");
            settingsFile.write(settings.toString());
            settingsFile.close();

            this.settings = settings;

        } catch (IOException e) {
            Debug.error(null, "Failed to update settings file.", e.getCause());
        }
    }

    public synchronized boolean getSettingBool(String key) {
        return Boolean.parseBoolean(getSetting(key));
    }

    public synchronized int getSettingInt(String key) {
        return Integer.parseInt(getSetting(key));
    }

    public synchronized String getSetting(String key) {

        try {
            return settings.get(key).toString();

        } catch (JSONException e) {

            // Determine if it was my fault for using a bad key or settings for having bad data
            if (defaultSettings.has(key)) {
                Debug.warn(null, "Failed to load correct settings, resetting settings.");
                resetSettings();
            } else {
                Debug.error(null, "Invalid key specified in settings: " + key, e.getCause());
            }

            return null;

        }

    }

    public synchronized JSONObject getSettings() {
        return settings;
    }

    public String getVersion() {
        return version;
    }

}