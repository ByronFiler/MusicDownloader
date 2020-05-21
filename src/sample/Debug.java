package sample;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Debug {

    private static boolean debug = true;

    public synchronized void set(boolean state)
    {
        debug = state;
    }

    public static void trace(String msg)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        if (debug){
            synchronized ( Debug.class )
            {
                System.out.println("DEBUG [" + formatter.format(new Date()) + "] " + msg);
            }
        }
    }

    public static synchronized void error(String fmt, Object... params )
    {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.printf("[ERROR " + formatter.format(new Date()) + "] " + fmt, params );
        System.out.println();
    }

}
