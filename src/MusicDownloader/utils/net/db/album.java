package MusicDownloader.utils.net.db;

import MusicDownloader.utils.net.db.sites.allmusic;

import java.io.IOException;
import java.util.ArrayList;

public interface album {

    void load() throws IOException;

    String getAlbum();

    ArrayList<allmusic.album.song> getSongs();

    int getPlaytime();

}
