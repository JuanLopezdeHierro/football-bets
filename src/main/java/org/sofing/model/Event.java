package org.sofing.model;

import java.sql.Timestamp;
import java.time.Instant;

public class Event {
    private Instant ts;
    private String ss;

    public Event(Instant ts, String ss) {
        this.ts = ts;
        this.ss = ss;
    }
    public Instant getTs() {
        return ts;
    }
    public void setTs(Instant ts) {
        this.ts = ts;
    }
    public String getSs() {
        return ss;
    }
    public void setSs(String ss) {
        this.ss = ss;
    }
}
