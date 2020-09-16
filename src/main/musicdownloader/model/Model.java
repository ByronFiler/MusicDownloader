package musicdownloader.model;

import javafx.stage.Stage;
import musicdownloader.utils.io.CacheManager;

// TODO: Move the view stuff into a separate class
public class Model {
    private final static Model instance = new Model();

    public final Settings settings = new Settings();
    public final Download download = new Download();
    public final Search search = new Search();

    private Stage primaryStage = null;
    private boolean stageClosed = false;

    public Model() {
        new CacheManager(download.getDownloadHistory());
    }

    public static Model getInstance() {
        return instance;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setStageClosed(boolean stageClosed) {
        this.stageClosed = stageClosed;
    }

    public boolean isStageClosed() {
        return stageClosed;
    }
}