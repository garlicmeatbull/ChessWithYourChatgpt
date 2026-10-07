package com.chesscoach.app;
import java.util.concurrent.*;
/** Keep native initialization/search/cleanup below input and rendering priority. */
public final class EngineWork {
    private static Thread worker(Runnable work){return new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);work.run();},"chess-engine");}
    public static ExecutorService queue(){return Executors.newSingleThreadExecutor(EngineWork::worker);}
    public static void close(Runnable work){worker(work).start();}
}
