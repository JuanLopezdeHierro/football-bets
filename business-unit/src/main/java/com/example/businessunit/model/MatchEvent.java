package com.example.businessunit.model;

import lombok.Getter; // Opcional con Lombok
import java.time.ZonedDateTime;
import java.util.UUID;

// @Getter // Descomenta si usas Lombok
public class MatchEvent {
    private final String id;
    private final String dateTime;
    private final double oddsDraw;
    private final String teamAway;
    private final double oddsAway;
    private final String teamHome;
    private final String source;
    private final double oddsHome;

    public MatchEvent(String dateTime, double oddsDraw, String teamAway, double oddsAway,
                      String teamHome, String source, double oddsHome) {
        this.id = UUID.randomUUID().toString();
        this.dateTime = dateTime;
        this.oddsDraw = oddsDraw;
        this.teamAway = teamAway;
        this.oddsAway = oddsAway;
        this.teamHome = teamHome;
        this.source = source;
        this.oddsHome = oddsHome;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getDateTime() { return dateTime; }       // NUEVO GETTER
    public double getOddsDraw() { return oddsDraw; }
    public String getTeamAway() { return teamAway; }
    public double getOddsAway() { return oddsAway; }
    public String getTeamHome() { return teamHome; }
    public String getSource() { return source; }
    public double getOddsHome() { return oddsHome; }
}