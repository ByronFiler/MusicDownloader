package musicdownloader.utils.app;

import musicdownloader.model.Model;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Debug {

    // Debugging configuration, as it stands volatile is not needed but in future could be
    private static boolean debug = true;
    private static final boolean advancedDebug = true;
    private static final boolean advancedDebugConcise = true;

    // Console colours
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";

    private static final List<String> errorTrace = new ArrayList<>();

    private static final boolean useColours = true; //!System.getProperty("os.name").startsWith("Windows");

    @SuppressWarnings("unused")
    public synchronized void set(boolean state)
    {
        debug = state;
    }

    public static void log(String msg) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String traceMessage;

        if (debug) {
            synchronized ( Debug.class )
            {

                traceMessage = String.format(
                        "LOG%s[%s: %d] @ %s: %s",
                        advancedDebug ? prettyExecutionTrace() : " ",
                        Thread.currentThread().getName(),
                        Thread.currentThread().getId(),
                        formatter.format(new Date()),
                        msg
                );

                errorTrace.add(traceMessage);
            }
        }

    }

    public static void trace(String msg) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String traceMessage;

        if (debug){
            synchronized ( Debug.class )
            {
                traceMessage = String.format(
                        "DEBUG%s[%s: %d] @ %s: %s",
                        advancedDebug ? prettyExecutionTrace() : " ",
                        Thread.currentThread().getName(),
                        Thread.currentThread().getId(),
                        formatter.format(new Date()),
                        msg
                );

                System.out.println(traceMessage);
                errorTrace.add(traceMessage);
            }
        }
    }

    public static synchronized void warn(String msg) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String traceMessage;

        if (debug){
            synchronized ( Debug.class )
            {

                traceMessage = String.format(
                        "WARNING%s[%s: %d] @ %s: %s",
                        advancedDebug ? prettyExecutionTrace() : " ",
                        Thread.currentThread().getName(),
                        Thread.currentThread().getId(),
                        formatter.format(new Date()),
                        msg
                );

                if (useColours) System.out.println(ANSI_YELLOW + traceMessage + ANSI_RESET);

                else System.out.println(traceMessage);

                errorTrace.add(traceMessage);
            }
        }

    }

    public static synchronized void success(String msg) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String traceMessage;

        if (debug){
            synchronized ( Debug.class )
            {

                traceMessage = String.format(
                        "DEBUG%s[%s: %d] @ %s: %s",
                        advancedDebug ? prettyExecutionTrace() : " ",
                        Thread.currentThread().getName(),
                        Thread.currentThread().getId(),
                        formatter.format(new Date()),
                        msg
                );

                if (useColours) System.out.println(ANSI_GREEN + traceMessage + ANSI_RESET);

                else System.out.println(traceMessage);

                errorTrace.add(traceMessage);
            }
        }

    }

    public static synchronized void error(String msg, Exception cause) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String traceMessage = String.format(
                "ERROR%s[%s: %d] @ %s: %s",
                advancedDebug ? prettyExecutionTrace() : " ",
                Thread.currentThread().getName(),
                Thread.currentThread().getId(),
                formatter.format(new Date()),
                msg
        );

        errorTrace.add(traceMessage);

        // Print
        if (useColours)
            System.out.println(ANSI_RED + traceMessage + ANSI_RESET);
        else
            System.out.println(traceMessage);

        cause.printStackTrace();

        boolean youtubeReachable;
        boolean allmusicReachable;
        boolean youtubeDlConfigured;
        boolean ffmpegConfigured;

        try {
            youtubeReachable = InetAddress.getByName("youtube.com").isReachable(1000);
        } catch (IOException ignored) { youtubeReachable = false; }

        try {
            allmusicReachable = InetAddress.getByName("allmusic.com").isReachable(1000);
        } catch (IOException ignored) { allmusicReachable = false; }

        try {
            Runtime.getRuntime().exec(new String[]{"youtube-dl"});
            youtubeDlConfigured = true;
        } catch (IOException ignored) { youtubeDlConfigured = false; }

        try {
            Runtime.getRuntime().exec(new String[]{"ffmpeg"});
            ffmpegConfigured = true;
        } catch (IOException ignored) { ffmpegConfigured = false; }


        String systemInformation = String.format("Hardware\nProcessor: %s (%s Core%s)\nMemory %s used of %s Available JVM\n\nSoftware\nOperating System: %s\nFFMPEG Configured: %s\nYoutube-DL Configured: %s\n\nConnections\nYoutube Reachable: %s\nAllMusic Reachable: %s\n",
                System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() == 1 ? "" : "s",
                FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()),
                FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory()),
                System.getProperty("os.name"),
                youtubeDlConfigured ? "Yes" : "No",
                ffmpegConfigured ? "Yes" : "No",
                youtubeReachable ? "Yes" : "No",
                allmusicReachable ? "Yes" : "No"
        );

        if (!Files.exists(Paths.get(Resources.getInstance().getApplicationData() + "crashes"))) {
            if (!new File(Resources.getInstance().getApplicationData() + "crashes").mkdirs()) {
                warn("Failed to create directory to save crashes");
                System.exit(-2);
            }
        }

        StringWriter sw = new StringWriter();
        cause.printStackTrace(new PrintWriter(sw));

        try {
            TarArchiveOutputStream tarArchive = new TarArchiveOutputStream(
                    new GZIPOutputStream(
                        new BufferedOutputStream(
                                new FileOutputStream(
                                        String.format(
                                                "%scrashes/%s_crash.tar.gz",
                                                Resources.getInstance().getApplicationData(),
                                                Instant.now().toEpochMilli()
                                        )
                                )
                        )
                    )
            );

            for (String[] fileData: new String[][]{
                    {"system_info.txt", systemInformation},
                    {"MusicDownloader.log", String.join("\n", errorTrace)},
                    {"error.txt", sw.toString()},
                    {"download.json", Model.getInstance().download.getDownloadObject().toString()},
                    {"queue.json",  Model.getInstance().download.getDownloadQueue().toString()},
                    {"history.json", Model.getInstance().download.getDownloadHistory().toString()}
            }) {
                TarArchiveEntry entry = new TarArchiveEntry(fileData[0]);
                entry.setSize(fileData[1].getBytes().length);

                tarArchive.putArchiveEntry(entry);

                BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(fileData[1].getBytes()));

                IOUtils.copy(bis, tarArchive);
                tarArchive.closeArchiveEntry();

                bis.close();
            }

            tarArchive.close();

        } catch (IOException e) {

            warn("Failed to write crash log.");

        }

        System.exit(-1);

    }

    private static synchronized String prettyExecutionTrace() {

        //Advanced Debugs, Gets Where the Debug is called from, expensive
        ArrayList<String> advancedDebugData = new ArrayList<>();
        StackTraceElement[] executionDetails = new Throwable().getStackTrace();

        // String[] debugFiles = new String[]{"Controller.java", "Settings.java", "Utils.java", "View.java", "Main.java"};
        String[] debugFiles = new String[]{"Main.java", "Model.java", "search.java", "settings.java", "results.java"};

        for (StackTraceElement executionDetail: executionDetails) {

            String[] traceDetail = executionDetail.toString().replaceAll(".*\\(|\\).*", "").split(":");

            if (Arrays.asList(debugFiles).contains(traceDetail[0])) {
                advancedDebugData.add(String.join(":", traceDetail));
            }
        }

        try {
            if (advancedDebugConcise) {
                return String.format(" [%s] ", advancedDebugData.get(0));
            } else {
                return String.format(" [%s] ", String.join(" from ", advancedDebugData));
            }
        } catch (IndexOutOfBoundsException ignored) {
            return " ";
        }

    }
}
