package MusicDownloader.utils.io;

import MusicDownloader.utils.app.debug;
import MusicDownloader.utils.app.resources;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;

public class install {

    public static final String FFMPEG_SOURCE = "https://ffmpeg.zeranoe.com/builds/win64/static/";

    private static final String os = System.getProperty("os.name").toLowerCase();

    public static boolean getYoutubeDl() throws IOException {

        if (os.contains("win")) {
            FileUtils.copyURLToFile(
                    new URL("https://youtube-dl.org/downloads/latest/youtube-dl.exe"),
                    new File(resources.getInstance().getYoutubeDlExecutable())
            );

        } else if (os.contains("mac")) {
            // Run console commands to install, doesn't need admin

        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            debug.warn("Youtube-DL install not currently supported on Linux.");

        } else debug.warn("Install is not supported on this operating system.");

        return verifyExecutable(resources.getInstance().getYoutubeDlExecutable());
    }

    public static boolean getFFMPEG() throws IOException {

        if (os.contains("win")) {
            String packageVersion = Jsoup.connect(FFMPEG_SOURCE).get().select("a").get(1).attr("href");

            FileUtils.copyURLToFile(
                    new URL(FFMPEG_SOURCE + packageVersion),
                    new File(System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.zip")
            );

            // Unzipping and extract relevant files
            for (String file : new String[]{"ffmpeg.exe", "ffplay.exe", "ffprobe.exe"})
                try (FileSystem fileSystem = FileSystems.newFileSystem(Paths.get(System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.zip"), null)) {
                    Files.copy(
                            fileSystem.getPath(FilenameUtils.removeExtension(packageVersion) + "/bin/" + file),
                            Paths.get(System.getenv("ProgramFiles(X86)") + "/youtube-dl/" + file)
                    );
                }

            if (!new File(System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.zip").delete())
                debug.warn("Failed to delete: " + System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.zip");

        } else if (os.contains("mac")) {

            // Install homebrew install ffmpeg


        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {

            // Linux

        } else debug.warn("Install is not supported on this operating system.");

        return verifyExecutable(resources.getInstance().getFfmpegExecutable());

    }

    private static boolean verifyExecutable(String executable) {

        try {

            // Will throw an error if not setup
            Runtime.getRuntime().exec(new String[]{executable});
            return true;

        } catch (IOException ignored) {
            return false;
        }

    }

    @SuppressWarnings("unused")
    private static synchronized void configure(String source) {



    }

}
