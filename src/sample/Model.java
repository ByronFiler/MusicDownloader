package sample;

import org.json.JSONObject;

public class Model {

    private final static Model instance = new Model();
    private JSONObject settings = SettingsFunc.getSettings();

    public static Model getInstance() {
        return instance;
    }

    // Settings: Settings & Model
    public JSONObject getSettings() {
        return settings;
    }
    public void setSettings(JSONObject newSettings) {
        settings = newSettings;
    }

}