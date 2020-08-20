package MusicDownloader.utils.net.db;

import java.io.IOException;

public interface song {

    void load() throws IOException;

    String getYear();

    String getAlbumArt();

    String getGenre();

    String getAlbumId();

}
