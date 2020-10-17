package musicdownloader.utils.io;

import musicdownloader.utils.app.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ProcessDebugger implements Runnable{

    private final ProcessBuilder builder;

    private final CountDownLatch awaiter = new CountDownLatch(1);
    private Process process;

    public ProcessDebugger(ProcessBuilder builder) {

        this.builder = builder;

        final Thread processThread = new Thread(this, "process-handler");
        processThread.setDaemon(true);
        processThread.start();

    }

    @Override
    public void run() {

        try {
            builder.redirectErrorStream(true);

            process = builder.start();

            while (true) {

                boolean safeQuit = true;
                InputStream is = process.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = reader.readLine()) != null) {
                    Debug.log(line);
                    if (line.contains("ERROR")) {
                        Debug.warn("Bad Youtube-DL Response: " + line);

                        // Retry
                        safeQuit = false;
                    }
                }

                if (safeQuit) break;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        awaiter.countDown();

    }

    public CountDownLatch getAwaiter() {
        return awaiter;
    }

    public void kill() {

        process.destroy();

        try {
            awaiter.await(200, TimeUnit.MILLISECONDS);

            if (process.isAlive()) {
                process.destroyForcibly();
                awaiter.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}
