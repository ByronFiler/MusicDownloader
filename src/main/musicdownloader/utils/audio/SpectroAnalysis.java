package musicdownloader.utils.audio;

import com.musicg.fingerprint.FingerprintManager;
import com.musicg.fingerprint.FingerprintSimilarityComputer;
import com.musicg.wave.Wave;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

@SuppressWarnings("all")
public class SpectroAnalysis {

    // Should internally cache the mp3 stream upon initialisation allowing for repeated check calls to use the same stream instead of each time streaming a mp3

    private final InputStream mp3Stream;

    public SpectroAnalysis(String sampleSource) throws IOException {

        this.mp3Stream = new URL(sampleSource).openStream();

    }

    public float Comparison(String downloadedFile) {

        if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "temp")))
            if (!new File(Resources.getInstance().getApplicationData() + "temp").mkdirs())
                Debug.error("Failed to create temp file directory for validation.", new IOException());

        // Preparing sample: Downloading sample & Converting
        try {
            new Converter().convert(
                    mp3Stream,
                    Resources.getInstance().getApplicationData() + "temp/sample.wav",
                    null,
                    null
            );
            new Converter().convert(
                    downloadedFile,
                    Resources.getInstance().getApplicationData() + "temp/source.wav"
            );

        } catch (JavaLayerException e) {
            Debug.warn("Error processing given data for conversion.");
        } finally {
            try {
                if (mp3Stream != null) mp3Stream.close();
            } catch (IOException e) {
                Debug.error("Failed to close remote sample source stream.", e);
            }
        }

        // Checking if the download file needs to be converted
        byte[] sampleData;
        byte[] downloadData;

        try {
            downloadData = new FingerprintManager().extractFingerprint(new Wave(Resources.getInstance().getApplicationData() + "temp/source.wav"));
            sampleData = new FingerprintManager().extractFingerprint(new Wave(Resources.getInstance().getApplicationData() + "temp/sample.wav"));
        } catch (ArrayIndexOutOfBoundsException ignored) {
            Debug.warn("File is too large to be checked.");
            return 1;
        }

        // Deleting temporary files
        if (!new File(Resources.getInstance().getApplicationData() + "temp/source.wav").delete())
            Debug.warn("Failed to delete source.wav");
        if (!new File(Resources.getInstance().getApplicationData() + "temp/sample.wav").delete())
            Debug.warn("Failed to delete sample.wav");

        FingerprintSimilarityComputer fingerprint = new FingerprintSimilarityComputer(sampleData, downloadData);
        return fingerprint.getFingerprintsSimilarity().getScore();

    }

    public void close() throws IOException {
        mp3Stream.close();
    }



}
