package musicdownloader.model;

import musicdownloader.utils.io.CacheManager;

public class Model {
    private final static Model instance = new Model();

    public final Settings settings = new Settings();
    public final Download download = new Download();
    public final Search search = new Search();

    public Model() {
        new CacheManager(download.getDownloadHistory());
    }

    public static Model getInstance() {
        return instance;
    }
}