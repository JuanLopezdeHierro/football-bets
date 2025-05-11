package model;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class MatchEvent {
    private final String id;
    private final ZonedDateTime timeStamp;
    private final double oddsDraw;
    private final String teamAway;
    private final double oddsAway;
    private final String teamHome;
    private final String source;
    private final double oddsHome;

    public MatchEvent(ZonedDateTime timeStamp, double oddsDraw, String teamAway, double oddsAway,
                      String teamHome, String source, double oddsHome) {
        this.id = UUID.randomUUID().toString();
        this.timeStamp = timeStamp;
        this.oddsDraw = oddsDraw;
        this.teamAway = teamAway;
        this.oddsAway = oddsAway;
        this.teamHome = teamHome;
        this.source = source;
        this.oddsHome = oddsHome;
    }

    public String getId() { return id; }
    public ZonedDateTime getTimeStamp() { return timeStamp; }
    public double getOddsDraw() { return oddsDraw; }
    public String getTeamAway() { return teamAway; }
    public double getOddsAway() { return oddsAway; }
    public String getTeamHome() { return teamHome; }
    public String getSource() { return source; }
    public double getOddsHome() { return oddsHome; }
}
