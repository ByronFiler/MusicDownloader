package MusicDownloader.model;

import MusicDownloader.Main;
import MusicDownloader.utils.app.debug;
import MusicDownloader.utils.app.resources;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
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

        // Check for app directory, make if it doesn't exist
        if (!Files.exists(Paths.get(resources.getInstance().getApplicationData()))) {

            System.out.println(new File(resources.getInstance().getApplicationData()).canWrite());

            if (!new File(resources.getInstance().getApplicationData()).mkdirs())
                debug.error("Failed to create application files.", new IOException());

        }

        // Load users actual settings
        try {
            settings = new JSONObject(new Scanner(new File(resources.getInstance().getApplicationData() + "json/config.json")).useDelimiter("\\Z").next());
            debug.trace("Found user settings.");
            System.out.println(settings.toString());
        } catch (FileNotFoundException | JSONException ignored) {
            debug.warn("Failed to load user settings.");
            settings = defaultSettings;
            resetSettings();
        }

        // Load the version
        try {

            version = new JSONObject(
                    new Scanner(Main.class.getResourceAsStream("app/meta.json"))
                            .useDelimiter("\\Z")
                            .next()
            )
                    .get("version")
                    .toString();

        } catch (JSONException e) {
            debug.warn("Failed to locate version.");
            version = null;
        }

    }

    private void resetSettings() {

        try {

            FileWriter newConfig = new FileWriter(resources.getInstance().getApplicationData() + "json/config.json");
            newConfig.write(defaultSettings.toString());
            newConfig.close();

        } catch (IOException e) {

            // Attempt to see if this was due to folders not working or a system IO error
            if (resetDirectories()) resetSettings();

            else debug.error("Failed to reset settings.", e);
        }
    }

    private boolean resetDirectories() {

        boolean wasUseful = false;

        // Checking for non existing folders
        if (!Files.exists(Paths.get(resources.getInstance().getApplicationData() + "cached"))) {
            wasUseful = true;
            if (!new File(resources.getInstance().getApplicationData() + "cached").mkdirs())
                debug.error("Failed to create non existing directory: " + resources.getInstance().getApplicationData() + "cached", new IOException());
        }

        if (!Files.exists(Paths.get(resources.getInstance().getApplicationData() + "json"))) {
            wasUseful = true;
            if (!new File(resources.getInstance().getApplicationData() + "json").mkdirs())
                debug.error("Failed to create non existing directory: " + resources.getInstance().getApplicationData() + "json", new IOException());
        }

        return wasUseful;
    }

    public void saveSettings(JSONObject settings) {
        try {
            if (!Files.exists(Paths.get(resources.getInstance().getApplicationData() + "json")))
                if (!new File(resources.getInstance().getApplicationData() + "json").mkdirs())
                    debug.error("Failed to create json folder in user data.", new IOException());

            System.out.println(settings);

            System.out.println(
                    new Scanner(new FileReader(resources.getInstance().getApplicationData() + "json/config.json"))
                        .useDelimiter("\\Z")
                        .next()
            );

            FileWriter settingsFile = new FileWriter(resources.getInstance().getApplicationData() + "json/config.json");
            settingsFile.write(settings.toString());
            settingsFile.close();

            this.settings = settings;

        } catch (IOException e) {
            debug.error("Failed to update settings file.", e);
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
                debug.warn("Failed to load correct settings, resetting settings.");
                resetSettings();
            } else {
                debug.error("Invalid key specified in settings: " + key, e);
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