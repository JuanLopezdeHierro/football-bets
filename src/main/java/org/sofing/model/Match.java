package org.sofing.model;

import java.time.LocalDateTime;

public class Match {
    private String team1;
    private String team2;
    private String field;
    private LocalDateTime dateTime;
    private String league;

    public Match(String team1, String team2, String field, LocalDateTime dateTime, String league) {
        this.team1 = team1;
        this.team2 = team2;
        this.field = field;
        this.dateTime = dateTime;
        this.league = league;
    }

    public String getTeam1() {
        return team1;
    }

    public void setTeam1(String team1) {
        this.team1 = team1;
    }

    public String getTeam2() {
        return team2;
    }

    public void setTeam2(String team2) {
        this.team2 = team2;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getLeague() {
        return league;
    }

    public void setLeague(String league) {
        this.league = league;
    }
}
