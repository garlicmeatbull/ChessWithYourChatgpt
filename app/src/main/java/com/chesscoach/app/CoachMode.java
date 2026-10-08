package com.chesscoach.app;

/** Admission and dispatch guard: turning automation off invalidates queued automatic work. */
final class CoachMode {
    record Ticket(boolean automatic,long version) {}
    private boolean automatic;
    private long version;
    CoachMode(boolean initial){automatic=initial;}
    synchronized boolean automatic(){return automatic;}
    synchronized void setAutomatic(boolean enabled){if(automatic!=enabled){automatic=enabled;version++;}}
    synchronized Ticket admit(boolean explicit){return explicit||automatic?new Ticket(!explicit,version):null;}
    synchronized boolean mayStart(Ticket ticket){return ticket!=null&&(!ticket.automatic()||automatic&&ticket.version()==version);}
}
