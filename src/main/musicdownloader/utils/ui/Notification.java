package musicdownloader.utils.ui;

import musicdownloader.utils.app.Debug;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class Notification {

    File directory;

    public Notification(String albumTitle, String meta, File directory) {

        this.directory = directory;

        if (SystemTray.isSupported()) {

            try {
                SystemTray tray = SystemTray.getSystemTray();
                TrayIcon ti = new TrayIcon(Toolkit.getDefaultToolkit().createImage(""), "View Files");
                ti.setImageAutoSize(true);

                ti.addActionListener(this::openFiles);

                ti.setToolTip("View Downloaded Files");
                tray.add(ti);

                ti.displayMessage("Finished Downloading " + albumTitle, meta, TrayIcon.MessageType.ERROR);
            } catch (AWTException e) {
                Debug.error("Failed to send notification.", e);
            }

        }

    }

    private void openFiles(ActionEvent e) {

        try {
            if (directory.exists()) Desktop.getDesktop().open(directory);
        } catch (IOException er) {
            Debug.warn("Failed to open directory from notification, despite directory existing.");
        }

    }


}
