package musicdownloader.utils.io;

import org.junit.jupiter.api.Test;

class cacheManagerTest {

    @Test
    void backupExisting() {
        /*
        try {

            // Creating temp directory to save user data
            if (!Files.exists(Paths.get(getInstance().getApplicationData() + "\\temp\\")))
                if (!new File(getInstance().getApplicationData() + "\\temp\\").mkdirs())
                    debug.error("Failed to create temp files for backup.", new IOException());

            if (!Files.exists(Paths.get(getInstance().getApplicationData() + "\\cached\\")))
                if (!new File(getInstance().getApplicationData() + "\\cached\\").mkdirs())
                    debug.error("Failed to create temp files for backup.", new IOException());

            // Moving user data to a separate temp folder
            Files.move(
                    Paths.get(getInstance().getApplicationData() + "\\json\\downloads.gz"),
                    Paths.get(getInstance().getApplicationData() + "\\temp\\downloads.gz")
            );
            FileUtils.moveDirectory(
                    new File(getInstance().getApplicationData() + "\\cached\\"),
                    new File(getInstance().getApplicationData() + "\\temp\\cached\\")
            );

            // Moving test data into user data section
            Files.copy(
                Paths.get("tests/downloads1.gz"),
                Paths.get(getInstance().getApplicationData() + "\\json\\downloads.gz")
            );

            Files.copy(
                    Paths.get("tests/416791769641077.jpg"),
                    Paths.get(getInstance().getApplicationData() + "\\cached\\416791769641077.jpg")
            );
            Files.copy(
                    Paths.get("tests/808553218544595.jpg"),
                    Paths.get(getInstance().getApplicationData() + "\\cached\\808553218544595.jpg")
            );
            Files.copy(
                    Paths.get("tests/4619245642105123.jpg"),
                    Paths.get(getInstance().getApplicationData() + "\\cached\\4619245642105123.jpg")
            );

            assert true;

        } catch (IOException e) {
            e.printStackTrace();
            debug.warn("Failed to backup ");
            assert false;
        }

         */
    }

    @Test
    void checkRemoveIrrelevantFiles() {

    }

    @Test
    void checkRemoveInvalidJPGs() {

    }

    @Test
    void downloadMissingFiles() {

    }

    @Test
    void testStandard() {

    }

    @Test
    void restoreBackup() {

        /*
        try {
            for (File cachedTestFile: Objects.requireNonNull(new File(getInstance().getApplicationData() + "\\cached\\").listFiles()))
                if (!cachedTestFile.delete()) {
                    debug.warn("Failed to delete: " + cachedTestFile.getAbsolutePath());
                    assert false;
                }

            if (!new File(getInstance().getApplicationData() + "\\json\\downloads.gz").delete()) {
                debug.warn("Failed to delete temporary downloads file.");
                assert false;
            }

            Files.copy(
                    Paths.get(getInstance().getApplicationData() + "\\temp\\downloads.gz"),
                    Paths.get(getInstance().getApplicationData() + "\\json\\downloads.gz")
            );
            FileUtils.copyDirectory(
                    new File(getInstance().getApplicationData() + "\\temp\\cached\\"),
                    new File(getInstance().getApplicationData() + "\\cached\\")
            );

            FileUtils.deleteDirectory(new File(getInstance().getApplicationData() + "\\temp\\"));
            assert true;

        } catch (IOException e) {
            debug.warn("Failed to restore files.");
            assert false;

        }

         */


    }

}