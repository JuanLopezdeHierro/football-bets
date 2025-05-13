package com.example.businessunit.model;

import lombok.Getter; // Opcional con Lombok
import java.time.ZonedDateTime;
import java.util.UUID;

// @Getter
public class MatchEvent {
    private final String id;
    private final ZonedDateTime timeStamp; // Timestamp del registro del evento
    private final String dateTimeString;   // El string original de fecha/hora/minuto del partido
    private double oddsDraw;
    private String teamAway;
    private double oddsAway;
    private String teamHome;
    private String source;
    private double oddsHome;
    // @Setter
    private MatchStatus matchStatus;
    // @Setter
    private String liveTimeDisplay; // Para "34'", "HT", etc.


    public MatchEvent(ZonedDateTime timeStamp, String dateTimeString, double oddsDraw, String teamAway, double oddsAway,
                      String teamHome, String source, double oddsHome, MatchStatus initialStatus) {
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
        this.liveTimeDisplay = null; // Inicialmente nulo, se establece si está en vivo
    }

    // Getters
    public String getId() { return id; }
    public ZonedDateTime getTimeStamp() { return timeStamp; }
    public String getDateTimeString() { return dateTimeString; }
    public double getOddsDraw() { return oddsDraw; }
    public String getTeamAway() { return teamAway; }
    public double getOddsAway() { return oddsAway; }
    public String getTeamHome() { return teamHome; }
    public String getSource() { return source; }
    public double getOddsHome() { return oddsHome; }
    public MatchStatus getMatchStatus() { return matchStatus; }
    public String getLiveTimeDisplay() { return liveTimeDisplay; }

    // Setters
    public void setMatchStatus(MatchStatus matchStatus) { this.matchStatus = matchStatus; }
    public void setOddsDraw(double oddsDraw) { this.oddsDraw = oddsDraw; }
    public void setOddsAway(double oddsAway) { this.oddsAway = oddsAway; }
    public void setOddsHome(double oddsHome) { this.oddsHome = oddsHome; }
    public void setLiveTimeDisplay(String liveTimeDisplay) { this.liveTimeDisplay = liveTimeDisplay; }
    public void setSource(String source) { this.source = source; }
    // Podrías añadir un setter para dateTimeString si se espera que cambie
    // public void setDateTimeString(String dateTimeString) { this.dateTimeString = dateTimeString; }
}