package MusicDownloader.utils.app;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarException;

// TODO: Could auto create directories in constructor (if they don't exist)

public class resources {
    private final static resources instance = new resources();
    public static final List<String> songReferences = Arrays.asList("mp3", "wav", "ogg", "aac");

    private String applicationData = null;

    public resources() {

        String os = System.getProperty("os.name").toLowerCase();

        // Windows
        if (os.contains("win")) applicationData = System.getenv("APPDATA") + "/MusicDownloader/";

        // MacOS
        else if (os.contains("mac")) applicationData = System.getProperty("user.home") + "/Library/MusicDownloader/";

        // Unix Based
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix"))
            debug.error("Linux is currently unsupported.", new JarException());

        // SunOS & Unknown
        else debug.error("Unsupported operating system.", new JarException());

    }

    public String getApplicationData() {
        return Objects.requireNonNull(applicationData);
    }

    public static resources getInstance() {
        return instance;
    }

}
