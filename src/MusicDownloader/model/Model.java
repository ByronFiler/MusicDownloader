package MusicDownloader.model;

import MusicDownloader.utils.app.debug;
import MusicDownloader.utils.app.resources;
import MusicDownloader.utils.io.cacheOptimizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Model {
    private final static Model instance = new Model();

    public final MusicDownloader.model.settings settings = new settings();
    public final MusicDownloader.model.download download = new download();
    public final MusicDownloader.model.search search = new search();

    public Model() {
        new cacheOptimizer(download.getDownloadHistory());
    }

    public static Model getInstance() {
        return instance;
    }
}