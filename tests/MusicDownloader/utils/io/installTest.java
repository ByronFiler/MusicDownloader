package MusicDownloader.utils.io;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class installTest {

    @Test
    void getYoutubeDl() {

        if (new File(System.getenv("ProgramFiles(X86)") + "/test/").mkdir() && new File(System.getenv("ProgramFiles(X86)") + "/test/").delete()) {

            try {
                assert install.getYoutubeDl();
            } catch (IOException e) {
                assert false;
            }

        } else
            assert false;
    }

    @Test
    void getFFMPEG() {

        if (new File(System.getenv("ProgramFiles(X86)") + "/test/").mkdir() && new File(System.getenv("ProgramFiles(X86)") + "/test/").delete()) {

            try {
                assert install.getFFMPEG();
            } catch (IOException e) {
                assert false;
            }

        } else
            assert false;
    }
}