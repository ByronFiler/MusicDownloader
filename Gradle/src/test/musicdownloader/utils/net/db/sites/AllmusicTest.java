package musicdownloader.utils.net.db.sites;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class AllmusicTest {

    @Test
    void validSearch() {
        Allmusic.Search validSearch = new Allmusic.Search("The Dark Side of the Moon");
        try {
            validSearch.query(true);
            assert validSearch.getResults().getJSONArray("songs").length() > 0;
        } catch (IOException | JSONException e) {
            assert false;
        }
    }

    @Test
    void invalidSearch() {
        Allmusic.Search invalidSearch = new Allmusic.Search("%%%%%%%%");
        try {
            invalidSearch.query(true);
        } catch (IOException e) {
            assert false;
        }
    }

    @Test
    void noResultsSearch() {
        Allmusic.Search noResultsSearch = new Allmusic.Search("This is a valid search, but it contains no results.");
        try {
            noResultsSearch.query(true);
            assert noResultsSearch.getResults().getJSONArray("songs").length() == 0;
        } catch (IOException | JSONException e) {
            assert false;
        }
    }

    @Test
    void validSong() {

        Allmusic.Song validRequest = new Allmusic.Song("time-mt0042078650");
        try {
            validRequest.load();
            assert (!validRequest.getAlbumArt().isEmpty()) && (!validRequest.getAlbumId().isEmpty()) && (!validRequest.getYear().isEmpty());
        } catch (IOException e) {
            assert false;
        }
    }

    @Test
    void invalidSong() {

        Allmusic.Song invalidRequest = new Allmusic.Song("%%%%%%%");
        try {
            invalidRequest.load();
        } catch (IOException e) {
            assert false;
        }

    }

    @Test
    void validAlbum() {

        Allmusic.Album validAlbum = new Allmusic.Album("the-dark-side-of-the-moon-mw0000191308");
        try {
            validAlbum.load();
            assert validAlbum.getSongs().size() == 10 && validAlbum.getPlaytime() > 0 && !validAlbum.getAlbum().isEmpty();
        } catch (IOException e) {
            assert false;
        }
    }

    @Test
    void invalidAlbum() {

        Allmusic.Album invalidAlbum = new Allmusic.Album("%%%%%%%");
        try {
            invalidAlbum.load();
        } catch (IOException e) {
            assert false;
        }

    }

}