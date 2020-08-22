package MusicDownloader.utils.io;

import org.junit.jupiter.api.Test;

class cacheOptimizerTest {

    @Test
    void backupExisting() {
        /*
        try {

            // Creating temp directory to save user data
            if (!Files.exists(Paths.get(resources.getInstance().getApplicationData() + "\\temp\\")))
                if (!new File(resources.getInstance().getApplicationData() + "\\temp\\").mkdirs())
                    debug.error("Failed to create temp files for backup.", new IOException());

            if (!Files.exists(Paths.get(resources.getInstance().getApplicationData() + "\\cached\\")))
                if (!new File(resources.getInstance().getApplicationData() + "\\cached\\").mkdirs())
                    debug.error("Failed to create temp files for backup.", new IOException());

            // Moving user data to a separate temp folder
            Files.move(
                    Paths.get(resources.getInstance().getApplicationData() + "\\json\\downloads.gz"),
                    Paths.get(resources.getInstance().getApplicationData() + "\\temp\\downloads.gz")
            );
            FileUtils.moveDirectory(
                    new File(resources.getInstance().getApplicationData() + "\\cached\\"),
                    new File(resources.getInstance().getApplicationData() + "\\temp\\cached\\")
            );

            // Moving test data into user data section
            Files.copy(
                Paths.get("tests/resources/downloads1.gz"),
                Paths.get(resources.getInstance().getApplicationData() + "\\json\\downloads.gz")
            );

            Files.copy(
                    Paths.get("tests/resources/416791769641077.jpg"),
                    Paths.get(resources.getInstance().getApplicationData() + "\\cached\\416791769641077.jpg")
            );
            Files.copy(
                    Paths.get("tests/resources/808553218544595.jpg"),
                    Paths.get(resources.getInstance().getApplicationData() + "\\cached\\808553218544595.jpg")
            );
            Files.copy(
                    Paths.get("tests/resources/4619245642105123.jpg"),
                    Paths.get(resources.getInstance().getApplicationData() + "\\cached\\4619245642105123.jpg")
            );

            assert true;

        } catch (IOException e) {
            e.printStackTrace();
            debug.warn("Failed to backup resources.");
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
            for (File cachedTestFile: Objects.requireNonNull(new File(resources.getInstance().getApplicationData() + "\\cached\\").listFiles()))
                if (!cachedTestFile.delete()) {
                    debug.warn("Failed to delete: " + cachedTestFile.getAbsolutePath());
                    assert false;
                }

            if (!new File(resources.getInstance().getApplicationData() + "\\json\\downloads.gz").delete()) {
                debug.warn("Failed to delete temporary downloads file.");
                assert false;
            }

            Files.copy(
                    Paths.get(resources.getInstance().getApplicationData() + "\\temp\\downloads.gz"),
                    Paths.get(resources.getInstance().getApplicationData() + "\\json\\downloads.gz")
            );
            FileUtils.copyDirectory(
                    new File(resources.getInstance().getApplicationData() + "\\temp\\cached\\"),
                    new File(resources.getInstance().getApplicationData() + "\\cached\\")
            );

            FileUtils.deleteDirectory(new File(resources.getInstance().getApplicationData() + "\\temp\\"));
            assert true;

        } catch (IOException e) {
            debug.warn("Failed to restore files.");
            assert false;

        }

         */


    }

}