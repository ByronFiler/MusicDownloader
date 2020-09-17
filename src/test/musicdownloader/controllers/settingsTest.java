package musicdownloader.controllers;

import javafx.fxml.FXMLLoader;
import musicdownloader.Main;
import musicdownloader.model.Settings;
import musicdownloader.utils.app.Resources;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class settingsTest {

    private final String version = new Settings().getVersion();

    // Validate Version
    @Test
    void testVersion() {
        assertNotEquals(version, null);
    }

    // Validate Latest Version
    @Test
    void testLatestVersion() {

        try {
            assertEquals(
                    new JSONObject(
                            Jsoup.connect(Resources.remoteVersionUrl)
                                    .get()
                                    .text()
                    ).getString("version"),
                    version
            );
        } catch (IOException | JSONException e) {
            assert false;
        }

    }

    @Test
    void testInitialisation() {
        new FXMLLoader(getClass().getClassLoader().getResource("resources/fxml/settings.fxml"));
    }

}