package org.sofing.model;

public class Odd {
    private double oddsTeam1;
    private double oddsDraw;
    private double oddsTeam2;

    public Odd(double oddsTeam1, double oddsDraw, double oddsTeam2) {
        this.oddsTeam1 = oddsTeam1;
        this.oddsDraw = oddsDraw;
        this.oddsTeam2 = oddsTeam2;
    }

    public double getOddsTeam1() {
        return oddsTeam1;
    }

    public void setOddsTeam1(double oddsTeam1) {
        this.oddsTeam1 = oddsTeam1;
    }

    public double getOddsDraw() {
        return oddsDraw;
    }

    public void setOddsDraw(double oddsDraw) {
        this.oddsDraw = oddsDraw;
    }

    public double getOddsTeam2() {
        return oddsTeam2;
    }

    public void setOddsTeam2(double oddsTeam2) {
        this.oddsTeam2 = oddsTeam2;
    }
}
