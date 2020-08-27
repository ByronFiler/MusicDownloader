package musicdownloader.utils.app;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarException;

// TODO: Could auto create directories in constructor (if they don't exist)

public class Resources {
    private final static Resources instance = new Resources();

    public static final List<String> songReferences = Arrays.asList("mp3", "wav", "ogg", "aac");
    public static final String remoteVersionUrl = "https://raw.githubusercontent.com/ByronFiler/MusicDownloader/master/src/main/resources/meta.json";
    public static final String mp3Source = "https://rovimusic.rovicorp.com/playback.mp3?c=%s=&f=I";

    private String applicationData = null;
    private String youtubeDlExecutable = null;
    private String ffmpegExecutable = null;

    // Windows makes the windows slightly different sizes after each new scene
    private int windowResizeWidth = 0;
    private int windowResizeHeight = 0;

    public Resources() {

        String os = System.getProperty("os.name").toLowerCase();

        // Windows
        if (os.contains("win")) {
            applicationData = System.getenv("APPDATA") + "/MusicDownloader/";
            youtubeDlExecutable = System.getenv("ProgramFiles(X86)") + "/youtube-dl/youtube-dl.exe";
            ffmpegExecutable = System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.exe";

            windowResizeWidth = 16;
            windowResizeHeight = 39;
        }

        // MacOS
        else if (os.contains("mac")) {
            applicationData = System.getProperty("user.home") + "/Library/MusicDownloader/";
            youtubeDlExecutable = "/usr/local/bin/youtube-dl";
            ffmpegExecutable = "/usr/local/bin/ffmpeg";

            windowResizeWidth = 0;
            windowResizeHeight = 22;
        }

        // Unix Based
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix"))
            Debug.error("Linux is currently unsupported.", new JarException());

        // SunOS & Unknown
        else Debug.error("Unsupported operating system.", new JarException());

    }

    public String getApplicationData() {
        return Objects.requireNonNull(applicationData);
    }

    public String getYoutubeDlExecutable() {
        return Objects.requireNonNull(youtubeDlExecutable);
    }

    public String getFfmpegExecutable() {
        return Objects.requireNonNull(ffmpegExecutable);
    }

    public int getWindowResizeWidth() {
        return windowResizeWidth;
    }

    public int getWindowResizeHeight() {
        return windowResizeHeight;
    }

    public static Resources getInstance() {
        return instance;
    }
}
