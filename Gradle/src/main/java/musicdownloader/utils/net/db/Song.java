package musicdownloader.utils.net.db;

import java.io.IOException;

public interface Song {

    void load() throws IOException;

    String getYear();

    String getAlbumArt();

    String getGenre();

    String getAlbumId();

}
