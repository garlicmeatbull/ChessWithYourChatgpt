package com.chesscoach.app;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CoachModeTest {
    @Test public void manualModeRejectsAllAutomaticEntryPoints(){
        CoachMode mode=new CoachMode(false);
        // Move analysis, model changes, screen resume and bulk review all use automatic admission.
        for(int i=0;i<100;i++)assertNull(mode.admit(false));
        assertTrue(mode.mayStart(mode.admit(true)));
    }
    @Test public void automaticModeAdmitsLiveRequests(){CoachMode mode=new CoachMode(true);var ticket=mode.admit(false);assertNotNull(ticket);assertTrue(ticket.automatic());assertTrue(mode.mayStart(ticket));}
    @Test public void switchingOffInvalidatesQueuedWorkEvenAfterSwitchingOnAgain(){CoachMode mode=new CoachMode(true);var old=mode.admit(false);mode.setAutomatic(false);assertFalse(mode.mayStart(old));mode.setAutomatic(true);assertFalse(mode.mayStart(old));assertTrue(mode.mayStart(mode.admit(false)));}
    @Test public void explicitSummaryAndDetailRemainAuthorizedAcrossToggleChanges(){CoachMode mode=new CoachMode(false);var manual=mode.admit(true);mode.setAutomatic(true);mode.setAutomatic(false);assertTrue(mode.mayStart(manual));assertFalse(manual.automatic());}
    @Test public void unchangedPreferenceDoesNotInvalidateQueuedRequests(){CoachMode mode=new CoachMode(true);var request=mode.admit(false);mode.setAutomatic(true);assertTrue(mode.mayStart(request));}
    @Test public void queuedAutomaticInferenceIsNeverInvokedAfterTurningOff()throws Exception {
        CoachMode mode=new CoachMode(true);CoachQueue queue=new CoachQueue();AtomicInteger calls=new AtomicInteger();CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(2);
        try{
            queue.submit(()->{started.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            assertTrue(started.await(3,TimeUnit.SECONDS));var automatic=mode.admit(false);var explicit=mode.admit(true);
            queue.submit(()->{if(mode.mayStart(automatic))calls.addAndGet(100);done.countDown();},false);
            queue.submit(()->{if(mode.mayStart(explicit))calls.incrementAndGet();done.countDown();},true);
            mode.setAutomatic(false);release.countDown();assertTrue(done.await(3,TimeUnit.SECONDS));assertEquals(1,calls.get());
        }finally{release.countDown();queue.shutdownNow();}
    }
}
