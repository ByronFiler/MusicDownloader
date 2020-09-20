package musicdownloader.model;

import javafx.stage.Stage;

public class View {

    private Stage primaryStage = null;
    private boolean stageClosed = false;

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
