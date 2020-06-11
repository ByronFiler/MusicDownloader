package sample;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static java.util.Map.entry;

public class Debug {

    // Debugging configuration, as it stands volatile is not needed but in future could be
    private static boolean debug = true;
    private static boolean debugThreads = true;
    private static boolean advancedDebug = true;
    private static boolean advancedDebugConcise = true;

    private static final Map<String, Boolean> threadDebugging = Map.ofEntries(
            entry("generateAutocomplete", true),
            entry("autoCompleteWeb", true),
            entry("allMusicQuery", true),
            entry("addToTable", true),
            entry("download-handler", true),
            entry("youtubeQueryThread", true),
            entry("getLatestVersion", true),
            entry("selectFolder", true),
            entry("smart-quit", true),
            entry("youtubeDlVerification", true),
            entry("ffmpegVerificationThread", true),
            entry("timerCountdown", true),
            entry("download", true)
    );

    public synchronized void set(boolean state)
    {
        debug = state;
    }

    public static void trace(Thread t, String msg) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        if (debug){
            synchronized ( Debug.class )
            {
                if (t != null) {

                    // Debugging a thread
                    try {
                        if (debugThreads && threadDebugging.get(t.getName())) {
                            System.out.println(
                                    String.format(
                                            "DEBUG%s[%s: %d] @ %s: %s",
                                                advancedDebug ? prettyExecutionTrace() : " ",
                                                t.getName(),
                                                t.getId(),
                                                formatter.format(
                                                        new Date()
                                                ),
                                                msg
                                    )
                            );
                        }
                    } catch (NullPointerException e) {
                        error( t, "Failed to find known thread: " + t.getName(), e.getStackTrace());
                    }

                } else {

                    // Debugging main thread
                    System.out.println(
                            String.format(
                                    "DEBUG%s[MAIN] @ %s: %s",
                                    advancedDebug ? prettyExecutionTrace() : " ",
                                    formatter.format(
                                            new Date()
                                    ),
                                    msg
                            )
                    );
                }
            }
        }
    }

    public static synchronized void error(Thread t, String msg, StackTraceElement[] errorMessage) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        if (t != null) {

            System.out.println(
                    String.format(
                            "ERROR%s[%s: %d] @ %s: %s",
                            advancedDebug ? prettyExecutionTrace() : " ",
                            t.getName(),
                            t.getId(),
                            formatter,
                            msg
                    )
            );

        } else {

            System.out.println(
                    String.format(
                            "ERROR%s[MAIN] @ %s: %s",
                            advancedDebug ? prettyExecutionTrace() : " ",
                            formatter,
                            msg
                    )
            );

        }

        for (StackTraceElement error: errorMessage) {
            System.out.println(error);
        }
    }

    private static synchronized String prettyExecutionTrace() {

        //Advanced Debugs, Gets Where the Debug is called from, expensive
        ArrayList<String> advancedDebugData = new ArrayList<>();
        StackTraceElement[] executionDetails = new Throwable().getStackTrace();

        if (advancedDebugConcise) {

            StackTraceElement executionDetail = executionDetails[3];

            String processedDetail = executionDetail.toString();
            processedDetail = processedDetail.substring(processedDetail.indexOf("(") + 1);
            processedDetail = processedDetail.substring(0, processedDetail.indexOf(")"));

            return " [" + processedDetail + "] ";

        } else {

            executionDetails = Arrays.copyOfRange(executionDetails, 2, executionDetails.length - 1);

            for (StackTraceElement detail : executionDetails) {

                String processedDetail = detail.toString();
                processedDetail = processedDetail.substring(processedDetail.indexOf("(") + 1);
                processedDetail = processedDetail.substring(0, processedDetail.indexOf(")"));

                advancedDebugData.add(processedDetail);
            }

            return " [" + String.join(" in ", advancedDebugData) + "] ";

        }

    }

}
