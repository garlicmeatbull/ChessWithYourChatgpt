package com.chesscoach.app;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** One network request at a time; interactive explanations precede queued bulk review work. */
final class CoachQueue {
    private final AtomicLong order=new AtomicLong();
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new PriorityBlockingQueue<>());
    private static final class Job extends FutureTask<Void> implements Comparable<Job>{
        final boolean foreground;final long order;
        Job(Runnable task,boolean foreground,long order){super(task,null);this.foreground=foreground;this.order=order;}
        public int compareTo(Job other){if(foreground!=other.foreground)return foreground?-1:1;return foreground?Long.compare(other.order,order):Long.compare(order,other.order);}
    }
    void submit(Runnable task){submit(task,true);}
    void submit(Runnable task,boolean foreground){worker.execute(new Job(task,foreground,order.incrementAndGet()));}
    void shutdownNow(){worker.shutdownNow();}
}
