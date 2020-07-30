package sample.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

//TODO
// Error should dump all messages to file without ASCII colours
// Ascii colours should only be visible if the terminal allows it

public class debug {

    // Debugging configuration, as it stands volatile is not needed but in future could be
    private static boolean debug = true;
    private static boolean advancedDebug = true;
    private static boolean advancedDebugConcise = true;

    // Console colours
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    private static final List<String> errorTrace = new ArrayList<>();

    private static final boolean useColours = !System.getProperty("os.name").startsWith("Windows");

    public synchronized void set(boolean state)
    {
        debug = state;
    }

    public static void trace(Thread t, String msg) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String traceMessage;

        if (debug){
            synchronized ( sample.utils.debug.class )
            {
                if (t != null) {

                    traceMessage = String.format(
                            "DEBUG%s[%s: %d] @ %s: %s",
                            advancedDebug ? prettyExecutionTrace() : " ",
                            t.getName(),
                            t.getId(),
                            formatter.format(new Date()),
                            msg
                    );

                } else {

                    // Debugging main thread
                    traceMessage = String.format(
                        "DEBUG%s@ %s: %s",
                        advancedDebug ? prettyExecutionTrace() : " ",
                        formatter.format(new Date()),
                        msg
                    );
                }

                System.out.println(traceMessage);
                errorTrace.add(traceMessage);
            }
        }
    }

    public static synchronized void warn(Thread t, String msg) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String traceMessage;

        if (debug){
            synchronized ( sample.utils.debug.class )
            {
                if (t != null) {

                    traceMessage = String.format(
                            "WARNING%s[%s: %d] @ %s: %s",
                            advancedDebug ? prettyExecutionTrace() : " ",
                            t.getName(),
                            t.getId(),
                            formatter.format(new Date()),
                            msg
                    );

                } else {

                    traceMessage = String.format(
                            "WARNING%s @ %s: %s",
                            advancedDebug ? prettyExecutionTrace() : " ",
                            formatter.format(new Date()),
                            msg
                    );
                }

                if (useColours)
                    System.out.println(ANSI_YELLOW + traceMessage + ANSI_RESET);
                else
                    System.out.println(traceMessage);

                errorTrace.add(traceMessage);
            }
        }

    }

    public static synchronized void error(Thread t, String msg, Exception cause) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String traceMessage;

        if (t != null) {

            traceMessage = String.format(
                    "ERROR%s[%s: %d] @ %s: %s",
                    advancedDebug ? prettyExecutionTrace() : " ",
                    t.getName(),
                    t.getId(),
                    formatter.format(new Date()),
                    msg
            );

        } else {

            traceMessage = String.format(
                    "ERROR%s @ %s: %s",
                    advancedDebug ? prettyExecutionTrace() : " ",
                    formatter.format(new Date()),
                    msg
            );

        }

        // Print
        if (useColours)
            System.out.println(ANSI_RED + traceMessage + ANSI_RESET);
        else
            System.out.println(traceMessage);

        cause.printStackTrace();
        System.out.println("    Cause -> " + cause);

        // Files
        try {
            FileWriter dump = new FileWriter(Instant.now().toEpochMilli() + "_crash.log");

            StringWriter sw = new StringWriter();
            cause.printStackTrace(new PrintWriter(sw));

            dump.write(String.join("\n", errorTrace));
            dump.write("\n" + traceMessage + "\n");
            dump.write(sw.toString());
            dump.write("\n    Cause -> " + cause);

            dump.close();

        } catch (IOException e) {
            warn(t, "Failed to save trace.");
        }

        System.exit(-1);

    }

    public static synchronized String prettyExecutionTrace() {

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
            return "";
        }

    }

}
