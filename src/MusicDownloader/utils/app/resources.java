package MusicDownloader.utils.app;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarException;

public class resources {
    private final static resources instance = new resources();
    public static final List<String> songReferences = Arrays.asList("mp3", "wav", "ogg", "aac");

    private String applicationData = null;

    public resources() {

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win"))
            applicationData = System.getenv("APPDATA") + "/MusicDownloader/";

        else if (os.contains("mac"))
            applicationData = "./Library/Preferences/MusicDownloader/";

        else if (os.contains("nix") || os.contains("nux") || os.contains("aix"))
            debug.error("Linux is currently, unsupported.", new JarException());

        else debug.error("Unsupported operating system.", new JarException());

    }

    public String getApplicationData() {
        return Objects.requireNonNull(applicationData);
    }

    public static resources getInstance() {
        return instance;
    }

}
