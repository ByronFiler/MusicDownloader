package musicdownloader.utils.io;

import musicdownloader.utils.app.Debug;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class QuickSort {

    private final JSONArray searchDataExtracted;

    public QuickSort(JSONArray searchDataExtracted, int low, int high) throws JSONException {
        this.searchDataExtracted = searchDataExtracted;
        sort(this.searchDataExtracted, low, high);
    }

    int partition(JSONArray searchData, int low, int high) throws JSONException{
        int pivot = searchData.getJSONObject(high).getInt("difference");
        int i = low-1; // index of smaller element
        for (int j=low; j<high; j++)
        {
            // If current element is smaller than the pivot
            if (searchData.getJSONObject(j).getInt("difference") < pivot)
            {
                i++;

                // swap arr[i] and arr[j]
                //int temp = arr[i];
                int temp = searchData.getJSONObject(i).getInt("difference");
                String temp0 = searchData.getJSONObject(i).getString("watch_id");

                searchData.getJSONObject(i).put("difference", searchData.getJSONObject(j).getInt("difference"));
                searchData.getJSONObject(i).put("watch_id", searchData.getJSONObject(j).getString("watch_id"));

                searchData.getJSONObject(j).put("difference", temp);
                searchData.getJSONObject(j).put("watch_id", temp0);
            }
        }

        // swap arr[i+1] and arr[high] (or pivot)
        int temp = searchData.getJSONObject(i+1).getInt("difference");
        String temp0 = searchData.getJSONObject(i+1).getString("watch_id");

        searchData.getJSONObject(i+1).put("difference", searchData.getJSONObject(high).getInt("difference"));
        searchData.getJSONObject(i+1).put("watch_id", searchData.getJSONObject(high).getString("watch_id"));

        searchData.getJSONObject(high).put("difference", temp);
        searchData.getJSONObject(high).put("watch_id", temp0);

        return i+1;
    }

    void sort(JSONArray searchData, int low, int high) throws JSONException {
        if (low < high) {
            int pi = partition(searchData, low, high);

            // Recursively sort elements
            sort(searchData, low, pi-1);
            sort(searchData, pi+1, high);
        }
    }

    public ArrayList<String> getSorted() {
        ArrayList<String> watchIds = new ArrayList<>();
        try {
            for (int i = 0; i < searchDataExtracted.length(); i++) {
                watchIds.add(searchDataExtracted.getJSONObject(i).getString("watch_id"));
            }
        } catch (JSONException e) {
            Debug.error("Failed to get sorted results.", e);
        }
        return watchIds;
    }

}
