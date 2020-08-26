package musicdownloader.utils.io;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

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
                    817978763,
                    Gzip.decompressFile(new File("src/test/resources/downloads0.gz")).hashCode()
            );

        } catch (IOException e) {
            assert false;
        }

    }

    @Test
    void compressData() {

        try {

            Gzip.compressData(
                    new ByteArrayInputStream("Example test".getBytes()),
                    new File("src/test/resources/test.gz")
            );
            String md5Hash = DigestUtils.md5Hex(Files.newInputStream(Paths.get("src/test/resources/test.gz")));
            Files.delete(Paths.get("src/test/resources/test.gz"));

            System.out.println(md5Hash);

            Assert.assertEquals(
                    "860c806c3db0f3540d99a7be8366442f",
                    md5Hash
            );

        } catch (IOException e) {
            assert false;
        }

    }
}