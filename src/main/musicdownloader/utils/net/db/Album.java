package musicdownloader.utils.net.db;

import musicdownloader.utils.net.db.sites.Allmusic;

import java.io.IOException;
import java.util.ArrayList;

public interface Album {

    void load() throws IOException;

    String getAlbum();

    ArrayList<Allmusic.Album.song> getSongs();

    int getPlaytime();

    boolean getIsLoaded();

}
