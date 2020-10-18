package musicdownloader.model;

import musicdownloader.utils.io.ConnectionWatcher;

public class Model {
    private final static Model instance = new Model();

    public final Settings settings = new Settings();
    public final Download download = new Download();
    public final Search search = new Search();
    public final View view = new View();

    public final ConnectionWatcher connectionWatcher = new ConnectionWatcher();

    public static Model getInstance() {
        return instance;
    }
}