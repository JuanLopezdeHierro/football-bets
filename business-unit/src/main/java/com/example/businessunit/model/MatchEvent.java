package com.example.businessunit.model;

import lombok.Getter;
import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
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

    public MatchEvent(ZonedDateTime timeStamp, String dateTimeString, double oddsDraw, String teamAway, double oddsAway,
                      String teamHome, String source, double oddsHome, MatchStatus initialStatus,
                      String homeTeamLogoUrl, String awayTeamLogoUrl) { // Constructor actualizado
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
        this.homeTeamLogoUrl = homeTeamLogoUrl; // Asignar
        this.awayTeamLogoUrl = awayTeamLogoUrl; // Asignar
    }

    // Setters (Lombok @Getter ya proporciona los getters)
    public void setMatchStatus(MatchStatus matchStatus) { this.matchStatus = matchStatus; }
    public void setOddsDraw(double oddsDraw) { this.oddsDraw = oddsDraw; }
    public void setOddsAway(double oddsAway) { this.oddsAway = oddsAway; }
    public void setOddsHome(double oddsHome) { this.oddsHome = oddsHome; }
    public void setLiveTimeDisplay(String liveTimeDisplay) { this.liveTimeDisplay = liveTimeDisplay; }
    public void setSource(String source) { this.source = source; }
    public void setHomeTeamLogoUrl(String homeTeamLogoUrl) { this.homeTeamLogoUrl = homeTeamLogoUrl; }
    public void setAwayTeamLogoUrl(String awayTeamLogoUrl) { this.awayTeamLogoUrl = awayTeamLogoUrl; }
}