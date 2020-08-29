package musicdownloader.utils.net.source;

import java.io.IOException;
import java.util.ArrayList;

public interface Source {

    void load() throws IOException;

    ArrayList<String> getResults();
}
