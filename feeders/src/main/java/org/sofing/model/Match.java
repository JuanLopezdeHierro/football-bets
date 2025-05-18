package org.sofing.model;

import java.time.LocalDateTime;
import java.util.List;

public class Match {
    private List<String> teams;
    private List<String> dateTimes;
    private String league;
    private List<Double> odds;

    public Match(List<String> teams, List<String> dateTimes, List<Double> odds, String league) {
        this.teams = teams;
        this.dateTimes = dateTimes;
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

