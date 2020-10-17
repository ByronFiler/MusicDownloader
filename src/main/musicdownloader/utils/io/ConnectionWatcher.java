package musicdownloader.utils.io;

import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import musicdownloader.controllers.Downloads;
import musicdownloader.controllers.Results;
import musicdownloader.controllers.Search;
import musicdownloader.controllers.Settings;
import musicdownloader.utils.app.Debug;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionWatcher extends TimerTask {

    private volatile boolean online = true;
    private BorderPane offlineNotification;
    private String[] hosts = new String[]{};

    private final Timer checkerHandler = new Timer(true);
    private final AtomicInteger fails = new AtomicInteger(0);

    private Settings settings;
    private Search search;
    private Results results;
    private Downloads downloads;

    private final String[] settingsHosts = new String[]{"github.com"};
    private final String[] searchHosts = new String[]{"allmusic.com"};
    private final String[] resultsHosts = new String[]{"allmusic.com", "youtube.com", "vimeo.com"};
    private final String[] downloadsHosts = new String[]{"allmusic.com", "youtube.com", "vimeo.com"};

    private String mode;

    private CountDownLatch latch = new CountDownLatch(1);

    public ConnectionWatcher() {
        checkerHandler.schedule(this, 0, 50);
    }

    public void switchMode(Settings settings) {
        this.settings = settings;
        this.offlineNotification = settings.getOfflineNotification();
        this.hosts = settingsHosts;

        mode = "settings";
    }

    public void switchMode(Search search) {
        this.search = search;
        this.offlineNotification = search.getOfflineNotification();
        this.hosts = searchHosts;

        mode = "search";
    }

    public void switchMode(Results results) {
        this.results = results;
        this.offlineNotification = results.getOfflineNotification();
        this.hosts = resultsHosts;

        mode = "results";
    }

    public void switchMode(Downloads downloads) {

        this.downloads = downloads;
        this.offlineNotification = downloads.getOfflineNotification();
        this.hosts = downloadsHosts;

        mode = "downloads";
    }

    @Override
    public void run() {

        boolean stateChanged = false;

        try {

            int reachableSites = 0;
            for (String host: hosts) {
                if (InetAddress.getByName(host).isReachable(1000)) reachableSites++;
            }

            if (reachableSites < hosts.length && online) {

                throw new UnknownHostException();

            } else if (reachableSites == hosts.length) {

                synchronized (this) {
                    fails.set(0);

                    if (!online) {

                        online = true;
                        latch.countDown();
                        stateChanged = true;

                        Platform.runLater(() -> {
                            offlineNotification.setVisible(false);
                            offlineNotification.setManaged(false);
                        });
                    }
                }

            }

        } catch (UnknownHostException e) {

            synchronized (this) {
                final int reattemptsLimit = 10;
                if (fails.get() == reattemptsLimit && online) {

                    online = false;
                    latch = new CountDownLatch(1);
                    stateChanged = true;

                    Platform.runLater(() -> {
                        offlineNotification.setVisible(true);
                        offlineNotification.setManaged(true);
                    });

                } else if (fails.get() < reattemptsLimit) fails.incrementAndGet();
            }

        } catch (IOException e) {

            Debug.error("Failed to check site, unknown fatal error.", e);

        } finally {

            if (stateChanged) {

                switch (mode) {

                    case "settings":
                        if (online) settings.versionCheck();
                        break;

                    case "search":
                        Platform.runLater(() ->
                            search.getSearch().setDisable(!online)
                        );
                        break;

                    case "results":
                        Platform.runLater(() ->
                            results.setDisabled(!online)
                        );
                        break;

                    case "downloads":
                        Platform.runLater(() ->
                            downloads.markDownloads(online)
                        );
                        break;

                    default:
                        Debug.warn("Unknown state passed: " + mode);

                }

            }

        }

    }

    public synchronized boolean isOffline() {
        return !online;
    }

    public synchronized void close() {
        checkerHandler.cancel();
    }

    public synchronized CountDownLatch getLatch() {
        return latch;
    }

}
