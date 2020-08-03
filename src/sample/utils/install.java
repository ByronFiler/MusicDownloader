package sample.utils;

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

@SuppressWarnings("unused")
public class install {

    public static void getYoutubeDl() {

        debug.trace(null, "Attempting to acquire youtube-dl.");

        // Windows only as of now
        if (!System.getProperty("os.name").startsWith("Windows")) {
            debug.warn(null, "Youtube-DL installation only supported on windows currently.");
            return;
        }

        try {

            // Need to check OS to determine where to install it and to install
            FileUtils.copyURLToFile(
                    new URL(resources.YOUTUBE_DL_SOURCE),
                    new File(System.getenv("ProgramFiles(X86)") + "\\youtube-dl\\youtube-dl.exe")
            );

        } catch (IOException e) {
            debug.warn(null, "Youtube-DL must be installed with elevated permissions, please restart and try again.");
        }

        // Should catch network exception

        // Testing executable to confirm installation
        try {

            // Will throw an error if not setup
            Runtime.getRuntime().exec(new String[]{"youtube-dl"});
            debug.trace(null, "Youtube-DL successfully configured.");

        } catch (IOException ignored) {

            debug.warn(null, "Failed to verify executable.");

        }

    }

    public static void getFFMPEG() throws IOException {

        debug.trace(null, "Attempting to acquire FFMPEG.");

        String packageVersion = Jsoup.connect(resources.FFMPEG_SOURCE).get().select("a").get(1).attr("href");

        try {
            // Downloading zip
            FileUtils.copyURLToFile(
                    new URL(resources.FFMPEG_SOURCE + packageVersion),
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
                debug.warn(null, "Failed to delete: " + System.getenv("ProgramFiles(X86)") + "\\youtube-dl\\ffmpeg.zip");

        } catch (IOException e) {
            debug.warn(null, "FFMPEG must be installed with elevated permissions, please restart and try again.");
        }

        // Test

    }

    private static synchronized void configure(String source) {



    }

}
