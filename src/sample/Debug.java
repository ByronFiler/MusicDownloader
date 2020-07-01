package sample;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static java.util.Map.entry;

// TODO
// Error should print full stack trace and kill program
// Error should throw a new stack trace, and instead pass a message parameter
// Shouldn't show MAIN for not a thread, just showing nothing

public class Debug {

    // Debugging configuration, as it stands volatile is not needed but in future could be
    private static boolean debug = true;
    private static boolean debugThreads = true;
    private static boolean advancedDebug = true;
    private static boolean advancedDebugConcise = true;
    private static boolean stackTrace = false;

    // Console colours
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

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
            entry("youtube-dl-verification", true),
            entry("ffmpeg-verification", true),
            entry("timer-countdown", true),
            entry("download", true),
            entry("output-directory-verification", true),
            entry("output-directory-listener", true),
            entry("downloads-listener", true),
            entry("cache-optimisation", true)
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
                            ANSI_RED + "ERROR%s[%s: %d] @ %s: %s" + ANSI_RESET,
                            advancedDebug ? prettyExecutionTrace() : " ",
                            t.getName(),
                            t.getId(),
                            formatter.format(new Date()),
                            msg
                    )
            );

        } else {

            System.out.println(
                    String.format(
                            ANSI_RED + "ERROR%s[MAIN] @ %s: %s" + ANSI_RESET,
                            advancedDebug ? prettyExecutionTrace() : " ",
                            formatter.format(new Date()),
                            msg
                    )
            );

        }

        if (stackTrace)
            Arrays.stream(errorMessage).forEachOrdered(System.out::println);
    }

    public static synchronized void warn(Thread t, String msg) {

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
                                            ANSI_YELLOW + "WARNING%s[%s: %d] @ %s: %s" + ANSI_RESET,
                                            advancedDebug ? prettyExecutionTrace() : " ",
                                            t.getName(),
                                            t.getId(),
                                            formatter.format(new Date()),
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
                                    ANSI_YELLOW + "WARNING%s[MAIN] @ %s: %s" + ANSI_RESET,
                                    advancedDebug ? prettyExecutionTrace() : " ",
                                    formatter.format(new Date()),
                                    msg
                            )
                    );
                }
            }
        }

    }

    private static synchronized String prettyExecutionTrace() {


        //Advanced Debugs, Gets Where the Debug is called from, expensive
        ArrayList<String> advancedDebugData = new ArrayList<>();
        StackTraceElement[] executionDetails = new Throwable().getStackTrace();


        // String[] debugFiles = new String[]{"Controller.java", "Settings.java", "Utils.java", "View.java", "Main.java"};
        String[] debugFiles = new String[]{"Main.java", "Model.java", "search.java", "settings.java", "results.java"};

        for (StackTraceElement executionDetail: executionDetails) {

            String processedDetail = executionDetail.toString();
            processedDetail = processedDetail.substring(processedDetail.indexOf("(") + 1);
            processedDetail = processedDetail.substring(0, processedDetail.indexOf(")"));

            if (Arrays.asList(debugFiles).contains(processedDetail.split(":")[0])){
                advancedDebugData.add(processedDetail);
            }
        }

        if (advancedDebugConcise) {
            return String.format(" [%s] ", advancedDebugData.get(advancedDebugData.size()-1));
        } else {
            return String.format(" [%s] ", String.join(" from ", advancedDebugData));
        }

    }

}
