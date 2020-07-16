package sample;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

//TODO
//Still seems to provide the wrong source file in debugging

public class Debug {

    // Debugging configuration, as it stands volatile is not needed but in future could be
    private static boolean debug = true;
    private static boolean advancedDebug = true;
    private static boolean advancedDebugConcise = true;

    // Console colours
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

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
                    System.out.println(
                            String.format(
                                    "DEBUG%s[%s: %d] @ %s: %s",
                                        advancedDebug ? prettyExecutionTrace() : " ",
                                        t.getName(),
                                        t.getId(),
                                        formatter.format(new Date()),
                                        msg
                            )
                    );

                } else {

                    // Debugging main thread
                    System.out.println(
                            String.format(
                                    "DEBUG%s@ %s: %s",
                                    advancedDebug ? prettyExecutionTrace() : " ",
                                    formatter.format(new Date()),
                                    msg
                            )
                    );
                }
            }
        }
    }

    public static synchronized void warn(Thread t, String msg) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        if (debug){
            synchronized ( Debug.class )
            {
                if (t != null) {

                    // Debugging a thread
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

                } else {

                    // Debugging main thread
                    System.out.println(
                            String.format(
                                    ANSI_YELLOW + "WARNING%s @ %s: %s" + ANSI_RESET,
                                    advancedDebug ? prettyExecutionTrace() : " ",
                                    formatter.format(new Date()),
                                    msg
                            )
                    );
                }
            }
        }

    }

    public static synchronized void error(Thread t, String msg, Throwable cause) {
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
                            ANSI_RED + "ERROR%s @ %s: %s" + ANSI_RESET,
                            advancedDebug ? prettyExecutionTrace() : " ",
                            formatter.format(new Date()),
                            msg
                    )
            );

        }

        System.out.println("    Cause -> " + cause);
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
