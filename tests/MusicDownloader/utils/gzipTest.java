package MusicDownloader.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import MusicDownloader.utils.io.gzip;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class gzipTest {

    @Test
    void decompressFile() {

        try {

            Assert.assertEquals(
                    1636506029,
                    gzip.decompressFile(new File("tests/resources/downloads.gz")).hashCode()
            );

        } catch (IOException e) {
            assert false;
        }

    }

    @Test
    void compressData() {

        try {

            gzip.compressData(new ByteArrayInputStream("Example test".getBytes()), new File("tests/resources/test.gz"));
            String md5Hash = DigestUtils.md5Hex(Files.newInputStream(Paths.get("tests/resources/test.gz")));
            Files.delete(Paths.get("tests/resources/test.gz"));

            Assert.assertEquals(
                    "860c806c3db0f3540d99a7be8366442f",
                    md5Hash
            );

        } catch (IOException e) {
            assert false;
        }

    }
}