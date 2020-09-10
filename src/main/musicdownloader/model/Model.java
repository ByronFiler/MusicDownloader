package musicdownloader.model;

import javafx.stage.Stage;
import musicdownloader.utils.io.CacheManager;

public class Model {
    private final static Model instance = new Model();

    public final Settings settings = new Settings();
    public final Download download = new Download();
    public final Search search = new Search();

    private Stage primaryStage = null;

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
}