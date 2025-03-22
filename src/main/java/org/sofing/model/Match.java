package org.sofing.model;

import java.time.LocalDateTime;
import java.util.List;

public class Match {
    private List<String> teams;
    private List<String> dateTimes;
    private String field;
    private String referee;
    private String league;
    private List<Double> odds;

    public Match(List<String> teams, List<String> dateTimes, String field, String referee, String league, List<Double> odds) {
        this.teams = teams;
        this.dateTimes = dateTimes;
        this.field = field;
        this.referee = referee;
        this.league = league;
        this.odds = odds;
    }

    public List<String> getTeams() {
        return teams;
    }

    public void setTeams(List<String> teams) {
        this.teams = teams;
    }

    public List<String> getDateTimes() {
        return dateTimes;
    }

    public void setDateTimes(List<String> dateTimes) {
        this.dateTimes = dateTimes;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getReferee() {
        return referee;
    }

    public void setReferee(String referee) {
        this.referee = referee;
    }

    public String getLeague() {
        return league;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public List<Double> getOdds() {
        return odds;
    }

    public void setOdds(List<Double> odds) {
        this.odds = odds;
    }
}

