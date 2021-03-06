package musicdownloader.utils.io;

import com.musicg.fingerprint.FingerprintManager;
import com.musicg.fingerprint.FingerprintSimilarityComputer;
import com.musicg.wave.Wave;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;
import musicdownloader.utils.app.Debug;
import musicdownloader.utils.app.Resources;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class AudioAnalysis {

    private final byte[] sample;
    private double sampleAmplitude = -1;

    private byte[] comparisonCache = new byte[]{};

    public AudioAnalysis(String sampleSource) throws IOException, JavaLayerException, UnsupportedAudioFileException {

        InputStream mp3Stream = new URL(sampleSource).openStream();
        this.sample = convertMp3(mp3Stream.readAllBytes());
        mp3Stream.close();

        this.sampleAmplitude = getAmplitude(AudioSystem.getAudioInputStream(new ByteArrayInputStream(sample)));
    }

    public float compare(String downloadedFile) throws IOException, JavaLayerException {

        byte[] wavComparison = new byte[]{};
        switch (FilenameUtils.getExtension(downloadedFile)) {

            case "mp3":
                wavComparison = convertMp3(FileUtils.readFileToByteArray(new File(downloadedFile)));
                break;

            case "ogg":
                Debug.error("Called for comparison on unsupported file type",new IllegalArgumentException());

            case "aac":
                Debug.error("Called for comparison on unsupported file type",new IllegalArgumentException());
        }

        try {
            this.comparisonCache = wavComparison;
            return new FingerprintSimilarityComputer(
                    new FingerprintManager().extractFingerprint(
                            new Wave(
                                    new ByteArrayInputStream(sample)
                            )
                    ),
                    new FingerprintManager().extractFingerprint(
                            new Wave(
                                    new ByteArrayInputStream(wavComparison)
                            )
                    )
            ).getFingerprintsSimilarity().getScore();
        } catch (ArrayIndexOutOfBoundsException e) {
            Debug.warn("File is too large to be validated, should've been trimmed.");
            return 1;
        }

    }

    public void correctAmplitude(String targetFile) {
        if (comparisonCache.length == 0) {

            // Need to build a comparison cache of this
            try {
                compare(targetFile);
            } catch (IOException | JavaLayerException e) {
                Debug.error("Failed to correct amplitude, error in attempting to compare downloaded to remote to build cache.", e);
            }
        }

        try {
            double downloadedAmplitude = getAmplitude(
                    AudioSystem.getAudioInputStream(
                            new ByteArrayInputStream(comparisonCache)
                    )
            );

            double amplitudeCorrection = Math.abs(sampleAmplitude - downloadedAmplitude) / ((sampleAmplitude + downloadedAmplitude) / 2);


            double correctedAmplitude = (amplitudeCorrection * 100);

            double amplitudeCorrectionCalculated =  1 + amplitudeCorrection;

            ProcessBuilder converter = new ProcessBuilder(Resources.getInstance().getFfmpegExecutable());
            converter.command(
                    "ffmpeg",
                    "-y",
                    "-i",
                    targetFile,
                    "-filter:a",
                    "\"volume=" + amplitudeCorrectionCalculated + "\"",
                    targetFile
            );
            converter.directory(new File(Resources.getInstance().getApplicationData() + "\\temp\\"));

            ProcessDebugger debugger = new ProcessDebugger(converter);
            try {
                debugger.getAwaiter().await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Debug.trace(String.format("Corrected an amplitude difference of %.2f%%.", correctedAmplitude));

        } catch (UnsupportedAudioFileException | IOException e) {
            Debug.error("Failed to get amplitude for a pre-converted cached file, this is very unexpected, check conversion.", e);
        }
    }

    private double getAmplitude(AudioInputStream source) throws IOException {

        final ArrayList<Float> amps = new ArrayList<>();
        final int bufferByteSize = 2048;
        final byte[] buf = new byte[bufferByteSize];
        final float[] samples = new float[bufferByteSize / 2];

        for (int b; (b = source.read(buf, 0, buf.length)) > -1;) {

            // convert bytes to samples here
            for (int i = 0, s = 0; i < b; ) {
                int sample = 0;

                sample |= buf[i++] & 0xFF;
                sample |= buf[i++] << 8;

                samples[s++] = sample / 32768f;
            }

            float rms = 0f;
            for (float sample : samples) rms += sample * sample;

            amps.add((float) Math.sqrt(rms / samples.length));
        }


        return amps.stream().mapToDouble(e -> e).sum() / amps.size();

    }

    // Samples only, doesn't trim, marginally faster to convert a sample like 1 to 5%
    private synchronized byte[] convertMp3(byte[] mp3Data) throws JavaLayerException, IOException {

        if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "temp")))
            if (!new File(Resources.getInstance().getApplicationData() + "temp").mkdirs())
                Debug.error("Failed to create temp file directory for validation.", new IOException());

        String wavPath = Resources.getInstance().getApplicationData() + "temp/wav_conversion.wav";
        new Converter().convert(
                new ByteArrayInputStream(mp3Data),
                wavPath,
                null,
                null
        );
        try {
            byte[] wavData = Files.readAllBytes(Paths.get(wavPath));
            Files.delete(Paths.get(wavPath));

            return wavData;
        } catch (ClosedByInterruptException e) {
            return new byte[]{};
        }

    }

    private synchronized byte[] convert(String sourceFile) throws IOException {

        if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "temp")))
            if (!new File(Resources.getInstance().getApplicationData() + "temp").mkdirs())
                Debug.error("Failed to create temp file directory for validation.", new IOException());

        ProcessBuilder converter = new ProcessBuilder(Resources.getInstance().getFfmpegExecutable());
        converter.command(
                "ffmpeg",
                "-y",
                "-i",
                sourceFile,
                "-ss",
                "0",
                "-t",
                "30",
                Resources.getInstance().getApplicationData() + "\\temp\\" + "converter.wav"
        );
        converter.directory(new File(Resources.getInstance().getApplicationData() + "\\temp\\"));

        ProcessDebugger debugger = new ProcessDebugger(converter);
        try {
            debugger.getAwaiter().await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        File convertedFile = new File(Resources.getInstance().getApplicationData() + "\\temp\\" + "converter.wav");

        byte[] convertedData = FileUtils.readFileToByteArray(convertedFile);

        if (!convertedFile.delete()) Debug.warn("Failed to delete temporary converted file.");

        return convertedData;

    }

}
