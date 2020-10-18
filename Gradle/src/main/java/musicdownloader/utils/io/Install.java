package musicdownloader.utils.io;

import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;

public class Install {

    private static final String os = System.getProperty("os.name").toLowerCase();

    public static boolean getYoutubeDl() throws IOException {

        if (os.contains("win")) {
            // Windows
            FileUtils.copyURLToFile(
                    new URL("https://youtube-dl.org/downloads/latest/youtube-dl.exe"),
                    new File(Resources.getInstance().getYoutubeDlExecutable())
            );

        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {

            // Unix
            new ProcessBuilder("curl", "-L", "https://yt-dl.org/downloads/latest/youtube-dl", "-o", "/usr/local/bin/youtube-dl").start();
            new ProcessBuilder("chmod", "a+rx", "/usr/local/bin/youtube-dl").start();

        }  else Debug.warn("Install is not supported on this operating system.");

        return verifyExecutable(Resources.getInstance().getYoutubeDlExecutable());
    }

    public static boolean getFFMPEG() throws IOException {

        // Detecting the operating system
        if (os.contains("win")) {

            // Windows
            final String FFMPEG_SOURCE = "https://ffmpeg.zeranoe.com/builds/win64/static/";

            String packageVersion = Jsoup.connect(FFMPEG_SOURCE).get().select("a").get(1).attr("href");
            FileUtils.copyURLToFile(
                    new URL(FFMPEG_SOURCE + packageVersion),
                    new File(System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.zip")
            );

            // Unzipping and extract relevant files
            for (String file : new String[]{"ffmpeg.exe", "ffplay.exe", "ffprobe.exe"})
                try (FileSystem fileSystem = FileSystems.newFileSystem(Paths.get(System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.zip"), (ClassLoader) null)) {
                    Files.copy(
                            fileSystem.getPath(FilenameUtils.removeExtension(packageVersion) + "/bin/" + file),
                            Paths.get(System.getenv("ProgramFiles(X86)") + "/youtube-dl/" + file)
                    );
                }

            if (!new File(System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.zip").delete())
                Debug.warn("Failed to delete: " + System.getenv("ProgramFiles(X86)") + "/youtube-dl/ffmpeg.zip");

        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {

            // Unix
            new ProcessBuilder("/bin/bash", "-c", "\"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)\"").start();
            new ProcessBuilder("brew", "install", "ffmpeg").start();

        }  else Debug.warn("Install is not supported on this operating system.");

        return verifyExecutable(Resources.getInstance().getFfmpegExecutable());

    }

    private static boolean verifyExecutable(String executable) {

        try {
            new ProcessBuilder(executable, "--version").start();
            return true;

        } catch (IOException ignored) {
            return false;
        }

    }
}
