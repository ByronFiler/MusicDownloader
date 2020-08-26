package musicdownloader.model;

import javafx.scene.layout.BorderPane;
import org.json.JSONObject;

public class Search {

    private BorderPane[] searchResults;
    private JSONObject searchResultsJson;

    public BorderPane[] getSearchResults(){
        return searchResults;
    }

    public void setSearchResults(BorderPane[] searchResults) {
        this.searchResults = searchResults;
    }

    public JSONObject getSearchResultsJson() {return searchResultsJson; }

    public void setSearchResultsJson(JSONObject searchResultsJson) {
        this.searchResultsJson = searchResultsJson;
    }

}
