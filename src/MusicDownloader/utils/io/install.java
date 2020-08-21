package MusicDownloader.utils.io;

import MusicDownloader.utils.app.debug;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class install {

    public static final String YOUTUBE_DL_SOURCE = "https://youtube-dl.org/downloads/latest/youtube-dl.exe";
    public static final String FFMPEG_SOURCE = "https://ffmpeg.zeranoe.com/builds/win64/static/";

    public static boolean getYoutubeDl() throws IOException {

        // Windows only as of now
        if (!System.getProperty("os.name").startsWith("Windows")) {
            debug.warn("Youtube-DL installation only supported on windows currently.");
            return false;
        }

        // Need to check OS to determine where to install it and to install
        FileUtils.copyURLToFile(
                new URL(YOUTUBE_DL_SOURCE),
                new File(System.getenv("ProgramFiles(X86)") + "\\youtube-dl\\youtube-dl.exe")
        );

        // Should catch network exception

        // Testing executable to confirm installation
        return verifyExecutable("youtube-dl");

    }

    public static boolean getFFMPEG() throws IOException {
        String packageVersion = Jsoup.connect(FFMPEG_SOURCE).get().select("a").get(1).attr("href");

        // Downloading zip
        FileUtils.copyURLToFile(
                new URL(FFMPEG_SOURCE + packageVersion),
                new File(System.getenv("ProgramFiles(X86)") + "\\youtube-dl\\ffmpeg.zip")
        );

        // Unzipping and extract relevant files
        Path zipFile = Paths.get(System.getenv("ProgramFiles(X86)") + "\\youtube-dl\\ffmpeg.zip");

        for (String file: new String[]{"ffmpeg.exe", "ffplay.exe", "ffprobe.exe"})
            try (java.nio.file.FileSystem fileSystem = FileSystems.newFileSystem(zipFile, null)) {
                Files.copy(
                        fileSystem.getPath(FilenameUtils.removeExtension(packageVersion) + "\\bin\\" + file),
                        Paths.get(System.getenv("ProgramFiles(X86)") + "\\youtube-dl\\" + file)
                );
            }

        // Delete irrelevant files
        if (!new File(System.getenv("ProgramFiles(X86)") + "\\youtube-dl\\ffmpeg.zip").delete())
            debug.warn("Failed to delete: " + System.getenv("ProgramFiles(X86)") + "\\youtube-dl\\ffmpeg.zip");


        // Test
        return verifyExecutable("ffmpeg");

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
