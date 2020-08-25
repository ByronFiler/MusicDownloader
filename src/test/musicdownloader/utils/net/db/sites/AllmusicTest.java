package musicdownloader.utils.net.db.sites;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class AllmusicTest {

    @Test
    void validSearch() {
        Allmusic.search validSearch = new Allmusic.search("The Dark Side of the Moon");
        try {
            validSearch.query(true);
            assert validSearch.getSearchResultsData().length() > 0;
        } catch (IOException e) {
            assert false;
        }
    }

    @Test
    void invalidSearch() {
        Allmusic.search invalidSearch = new Allmusic.search("%%%%%%%%");
        try {
            invalidSearch.query(true);
        } catch (IOException e) {
            assert false;
        }
    }

    @Test
    void noResultsSearch() {
        Allmusic.search noResultsSearch = new Allmusic.search("This is a valid search, but it contains no results.");
        try {
            noResultsSearch.query(true);
            assert noResultsSearch.getSearchResultsData().length() == 0;
        } catch (IOException e) {
            assert false;
        }
    }

    @Test
    void validSong() {

        Allmusic.song validRequest = new Allmusic.song("time-mt0042078650");
        try {
            validRequest.load();
            assert (!validRequest.getAlbumArt().isEmpty()) && (!validRequest.getAlbumId().isEmpty()) && (!validRequest.getYear().isEmpty());
        } catch (IOException e) {
            assert false;
        }
    }

    @Test
    void invalidSong() {

        Allmusic.song invalidRequest = new Allmusic.song("%%%%%%%");
        try {
            invalidRequest.load();
        } catch (IOException e) {
            assert false;
        }

    }

    @Test
    void validAlbum() {

        Allmusic.album validAlbum = new Allmusic.album("the-dark-side-of-the-moon-mw0000191308");
        try {
            validAlbum.load();
            assert validAlbum.getSongs().size() == 10 && validAlbum.getPlaytime() > 0 && !validAlbum.getAlbum().isEmpty();
        } catch (IOException e) {
            assert false;
        }
    }

    @Test
    void invalidAlbum() {

        Allmusic.album invalidAlbum = new Allmusic.album("%%%%%%%");
        try {
            invalidAlbum.load();
        } catch (IOException e) {
            assert false;
        }

    }

}