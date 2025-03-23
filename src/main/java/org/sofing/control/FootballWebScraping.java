package org.sofing.control;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.sofing.model.Match;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FootballWebScraping {

    private static final String URL = "https://www.betfair.es/sport/football/la-liga-espa%C3%B1ola/117";

    public Match betfairScraping() {
        List<String> teams = new ArrayList<>();
        List<String> dateTimes = new ArrayList<>();
        List<Double> odds = new ArrayList<>();

        try {
            Document connection = Jsoup.connect(URL).get();
            System.out.println("Conexión establecida");

            extractTeamsAndDateTimes(connection, teams, dateTimes);
            extractOdds(connection, odds);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Match(teams, dateTimes, null, null, null, odds);
    }

    private void extractTeamsAndDateTimes(Document connection, List<String> teams, List<String> dateTimes) {
        List<Element> teamElements = connection.select("span.team-name");
        List<Element> dateTimeElements = connection.select("span.date.ui-countdown");

        for (int i = 0; i < teamElements.size(); i += 2) {
            teams.add(getTeamName(teamElements.get(i)));
            teams.add(getTeamName(teamElements.get(i + 1)));
            dateTimes.add(getDateTime(dateTimeElements.get(i / 2)));
        }
    }

    private void extractOdds(Document connection, List<Double> odds) {
        List<Element> oddsElements = connection.select("span.ui-runner-price.ui-display-decimal-price");

        for (int i = 0; i < oddsElements.size(); i += 5) {
            odds.add(getOdds(oddsElements.get(i + 2)));
            odds.add(getOdds(oddsElements.get(i + 3)));
            odds.add(getOdds(oddsElements.get(i + 4)));
        }
    }

    private String getTeamName(Element team) {
        return team.text();
    }

    private double getOdds(Element odd) {
        return Double.parseDouble(odd.text());
    }

    private String getDateTime(Element dateTime) {
        return dateTime.text();
    }
}