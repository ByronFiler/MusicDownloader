package sample.model;

import javafx.scene.layout.BorderPane;
import org.json.JSONArray;

public class search {

    private BorderPane[] searchResults;
    private JSONArray searchResultsJson;

    public BorderPane[] getSearchResults(){
        return searchResults;
    }

    public void setSearchResults(BorderPane[] searchResults) {
        this.searchResults = searchResults;
    }

    public JSONArray getSearchResultsJson() {return searchResultsJson; }

    public void setSearchResultsJson(JSONArray searchResultsJson) {
        this.searchResultsJson = searchResultsJson;
    }

}
