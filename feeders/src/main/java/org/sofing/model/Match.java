package org.sofing.model;

import java.time.LocalDateTime;
import java.util.List;

public class Match {
    private List<String> teams;
    private List<String> dateTimes;
    private List<String> fields;
    private String referee;
    private String league;
    private List<Double> odds;

    public Match(List<String> teams, List<String> dateTimes, List<Double> odds, List<String> fields, String referee, String league) {
        this.teams = teams;
        this.dateTimes = dateTimes;
        this.fields = fields;
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

    public List<String> getField() {
        return fields;
    }

    public void setFields(List<String> fields) {this.fields = fields;}

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

