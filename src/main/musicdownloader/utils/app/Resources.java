package musicdownloader.utils.app;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarException;

// TODO: Could auto create directories in constructor (if they don't exist)

public class Resources {
    private final static Resources instance = new Resources();

    public static final List<String> songReferences = Arrays.asList("mp3", "wav", "ogg", "aac");
    public static final List<String> albumArtOptions = Arrays.asList("Never", "Albums Only", "Songs Only", "Never");
    public static final List<Integer> invalidSearchTypes = Arrays.asList(1969736551, 73174740, 932275414);

    public static final String remoteVersionUrl = "https://raw.githubusercontent.com/ByronFiler/MusicDownloader/master/src/main/resources/meta.json";
    public static final String mp3Source = "https://rovimusic.rovicorp.com/playback.mp3?c=%s=&f=I";
    public static final String youtubeVideoSource = "https://www.youtube.com/watch?v=";

    private String applicationData = null;
    private String youtubeDlExecutable = null;
    private String ffmpegExecutable = null;

    public Resources() {

        String os = System.getProperty("os.name").toLowerCase();

        // Windows
        if (os.contains("win")) {
            applicationData = System.getenv("APPDATA") + "/MusicDownloader/";
            youtubeDlExecutable = System.getenv("ProgramFiles(X86)") + "/youtube-dl/youtube-dl.exe";
            ffmpegExecutable = System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.exe";
        }

        // MacOS
        else if (os.contains("mac")) {
            applicationData = System.getProperty("user.home") + "/Library/MusicDownloader/";

            youtubeDlExecutable = "/usr/local/bin/youtube-dl";
            ffmpegExecutable = "/usr/local/bin/ffmpeg";
        }

        // Unix Based
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) Debug.error("Linux is currently unsupported.", new JarException());

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

    public static Resources getInstance() {
        return instance;
    }
}
