package com.example.businessunit.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MatchEvent {
    private final String id;
    private final ZonedDateTime timeStamp;
    private final String dateTimeString;

    private double oddsDraw;
    private String teamAway;
    private double oddsAway;
    private String teamHome;
    private String source;
    private double oddsHome;
    private MatchStatus matchStatus;
    private String liveTimeDisplay;
    private String homeTeamLogoUrl;
    private String awayTeamLogoUrl;
    private String stadium;
    private String referee;
    private String oddsSource;

    public MatchEvent(ZonedDateTime timeStamp,
                      String dateTimeString,
                      double oddsDraw,
                      String teamAway,
                      double oddsAway,
                      String teamHome,
                      String source,
                      double oddsHome,
                      MatchStatus initialStatus,
                      String homeTeamLogoUrl,
                      String awayTeamLogoUrl) {
        this.id = UUID.randomUUID().toString();
        this.timeStamp = timeStamp;
        this.dateTimeString = dateTimeString;
        this.oddsDraw = oddsDraw;
        this.teamAway = teamAway;
        this.oddsAway = oddsAway;
        this.teamHome = teamHome;
        this.source = source;
        this.oddsHome = oddsHome;
        this.matchStatus = initialStatus;
        this.liveTimeDisplay = null;
        this.homeTeamLogoUrl = homeTeamLogoUrl;
        this.awayTeamLogoUrl = awayTeamLogoUrl;
        this.stadium = null;
        this.referee = null;
        this.oddsSource = "Betfair";
    }

    public String getId() { return id; }
    public ZonedDateTime getTimeStamp() { return timeStamp; }
    public String getDateTimeString() { return dateTimeString; }
    public double getOddsDraw() { return oddsDraw; }
    public String getTeamAway() { return teamAway; } // Getter para teamAway
    public double getOddsAway() { return oddsAway; }
    public String getTeamHome() { return teamHome; }
    public String getSource() { return source; }
    public double getOddsHome() { return oddsHome; }
    public MatchStatus getMatchStatus() { return matchStatus; }
    public String getLiveTimeDisplay() { return liveTimeDisplay; }
    public String getHomeTeamLogoUrl() { return homeTeamLogoUrl; }
    public String getAwayTeamLogoUrl() { return awayTeamLogoUrl; }
    public String getStadium() { return stadium; }
    public String getReferee() { return referee; }
    public String getOddsSource() { return oddsSource; }

    public void setMatchStatus(MatchStatus matchStatus) { this.matchStatus = matchStatus; }
    public void setOddsDraw(double oddsDraw) { this.oddsDraw = oddsDraw; }
    public void setOddsAway(double oddsAway) { this.oddsAway = oddsAway; }
    public void setOddsHome(double oddsHome) { this.oddsHome = oddsHome; }
    public void setLiveTimeDisplay(String liveTimeDisplay) { this.liveTimeDisplay = liveTimeDisplay; }
    public void setSource(String source) { this.source = source; }
    public void setHomeTeamLogoUrl(String homeTeamLogoUrl) { this.homeTeamLogoUrl = homeTeamLogoUrl; }
    public void setAwayTeamLogoUrl(String awayTeamLogoUrl) { this.awayTeamLogoUrl = awayTeamLogoUrl; }
    public void setStadium(String stadium) { this.stadium = stadium; }
    public void setReferee(String referee) { this.referee = referee; }
    public void setOddsSource(String oddsSource) { this.oddsSource = oddsSource; }
}