package org.sofing.model;

public class Fixtures {
    private String referee;
    private String stadium;
    private String homeTeam;
    private String round;

    public Fixtures(String referee, String stadium, String homeTeam, String round) {
        this.referee = referee;
        this.stadium = stadium;
        this.homeTeam = homeTeam;
        this.round = round;
    }

    public String getReferee() {
        return referee;
    }

    public void setReferee(String referee) {
        this.referee = referee;
    }

    public String getStadium() {
        return stadium;
    }

    public void setStadium(String stadium) {
        this.stadium = stadium;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(String homeTeam) {
        this.homeTeam = homeTeam;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }
}
