package sample.model;

import sample.utils.cacheOptimizer;

public class Model {
    private final static Model instance = new Model();

    public final sample.model.settings settings = new settings();
    public final sample.model.download download = new download();
    public final sample.model.search search = new search();

    public Model() {
        new cacheOptimizer(download.getDownloadHistory());
    }

    public static Model getInstance() {
        return instance;
    }
}