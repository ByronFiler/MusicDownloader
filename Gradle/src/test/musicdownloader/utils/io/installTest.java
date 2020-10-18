package musicdownloader.utils.io;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

class installTest {

    @Test
    void getYoutubeDl() {

        if (new File(System.getenv("ProgramFiles(X86)") + "/test/").mkdir() && new File(System.getenv("ProgramFiles(X86)") + "/test/").delete()) {

            File youtubeDlDownloaded = new File(System.getenv("ProgramFiles(X86)") + "/youtube-dl/youtube-dl.exe");
            assert !youtubeDlDownloaded.exists() || youtubeDlDownloaded.delete();

            try {
                assert Install.getYoutubeDl();
            } catch (IOException e) {
                assert false;
            }

        } else assert false;
    }

    @Test
    void getFFMPEG() {

        if (new File(System.getenv("ProgramFiles(X86)") + "/test/").mkdir() && new File(System.getenv("ProgramFiles(X86)") + "/test/").delete()) {

            Arrays.stream(new File(System.getenv("ProgramFiles(X86)") + "/youtube-dl/").listFiles()).forEach(e -> {
                assert e.getName().equals("youtube-dl.exe") || e.delete();
            });

            try {
                assert Install.getFFMPEG();
            } catch (IOException e) {
                assert false;
            }

        } else assert false;
    }
}