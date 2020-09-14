package musicdownloader.model;

import musicdownloader.Main;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

// TODO: Should have a settings validator that checks settings or resets
public class Settings {

    private JSONObject settings;
    private JSONObject defaultSettings;
    private String version;

    public Settings() {

        // Declare default settings for reference
        try{
            defaultSettings = new JSONObject("{\"advanced_validation\": true, \"output_directory\":\"\",\"save_album_art\":0,\"music_format\":0, \"album_art\":true, \"album_title\":true, \"song_title\":true, \"artist\":true, \"year\":true, \"track\":true,\"dark_theme\":false, \"data_saver\":false, \"volume_correction\": true}");
        } catch (JSONException e) {
            Debug.error("Default settings are invalid.", e);
        }

        // Check for app directory, make if it doesn't exist
        if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData())))
            if (!new File(Resources.getInstance().getApplicationData()).mkdirs())
                Debug.error("Failed to create application files.", new IOException());

        // Load users actual settings
        try {

            JSONObject potentialSettings = new JSONObject(new Scanner(new File(Resources.getInstance().getApplicationData() + "json/config.json")).useDelimiter("\\Z").next());

            if (musicdownloader.utils.io.validation.Settings.validate(potentialSettings)) {
                settings = new JSONObject(potentialSettings);
            } else {
                Debug.warn("Settings were found but were not found to be valid and have been reset.");
                settings = defaultSettings;
                resetSettings();
            }

            settings = new JSONObject(new Scanner(new File(Resources.getInstance().getApplicationData() + "json/config.json")).useDelimiter("\\Z").next());

            Debug.trace("Found user settings.");

        } catch (FileNotFoundException | JSONException ignored) {
            Debug.warn("Failed to load user settings.");
            settings = defaultSettings;
            resetSettings();
        }

        // Load the version
        try {

            version = new JSONObject(
                    new Scanner(Main.class.getResourceAsStream("resources/meta.json"))
                            .useDelimiter("\\Z")
                            .next()
            )
                    .get("version")
                    .toString();

        } catch (JSONException e) {
            Debug.warn("Failed to locate version.");
            version = null;
        }

    }

    private void resetSettings() {

        try {

            FileWriter newConfig = new FileWriter(Resources.getInstance().getApplicationData() + "json/config.json");
            newConfig.write(defaultSettings.toString());
            newConfig.close();

        } catch (IOException e) {

            // Attempt to see if this was due to folders not working or a system IO error
            if (resetDirectories()) resetSettings();

            else Debug.error("Failed to reset settings.", e);
        }
    }

    private boolean resetDirectories() {

        boolean wasUseful = false;

        // Checking for non existing folders
        if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "cached"))) {
            wasUseful = true;
            if (!new File(Resources.getInstance().getApplicationData() + "cached").mkdirs())
                Debug.error("Failed to create non existing directory: " + Resources.getInstance().getApplicationData() + "cached", new IOException());
        }

        if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "json"))) {
            wasUseful = true;
            if (!new File(Resources.getInstance().getApplicationData() + "json").mkdirs())
                Debug.error("Failed to create non existing directory: " + Resources.getInstance().getApplicationData() + "json", new IOException());
        }

        return wasUseful;
    }

    public void saveSettings(JSONObject settings) {
        try {
            if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "json")))
                if (!new File(Resources.getInstance().getApplicationData() + "json").mkdirs())
                    Debug.error("Failed to create json folder in user data.", new IOException());

            FileWriter settingsFile = new FileWriter(Resources.getInstance().getApplicationData() + "json/config.json");
            settingsFile.write(settings.toString());
            settingsFile.close();

            this.settings = settings;

        } catch (IOException e) {
            Debug.error("Failed to update settings file.", e);
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
                Debug.warn("Failed to load correct settings, resetting settings.");
                resetSettings();
            } else {
                Debug.error("Invalid key specified in settings: " + key, e);
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