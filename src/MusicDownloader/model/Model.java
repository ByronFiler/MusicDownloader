package MusicDownloader.model;

import MusicDownloader.utils.io.cacheOptimizer;

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