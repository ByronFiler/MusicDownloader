package musicdownloader.utils.ui;

import musicdownloader.utils.app.Debug;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class Notification {

    File directory;

    public Notification(String title , String subtitle, File directory, TrayIcon.MessageType messageType) {

        this.directory = directory;

        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                TrayIcon ti = new TrayIcon(
                        Toolkit.getDefaultToolkit().createImage(getClass().getClassLoader().getResource("resources/img/icons/icon.png")),
                        directory == null ? "" : "View Files"
                );
                ti.setImageAutoSize(true);
                if (directory != null) {
                    ti.addActionListener(this::openFiles);
                    ti.setToolTip("View Downloaded Files");
                }
                tray.add(ti);

                ti.displayMessage(title, subtitle, messageType);
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
