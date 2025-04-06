package org.sofing.control;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.sofing.model.Event;
import org.sofing.model.Match;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FootballWebScrapingImpl implements FootballWebScraping {

    private static final String URL = "https://www.betfair.es/sport/football/la-liga-espa%C3%B1ola/117";
    private static final String DATA_LAKE_PATH = "event-store-builder/eventstore/datalake/";

    @Override
    public void dataToJson(Match match) {
        String source = "Betfair";
        Event event = new Event(Instant.now(), source);
        List<String> teams = match.getTeams();
        List<String> dateTimes = match.getDateTimes();
        List<Double> odds = match.getOdds();

        for (int i = 0; i < teams.size(); i += 2) {
            JSONObject matchJson = new JSONObject();
            matchJson.put("homeTeam", teams.get(i));
            matchJson.put("awayTeam", teams.get(i + 1));
            matchJson.put("dateTime", dateTimes.get(i / 2));
            matchJson.put("homeOdd", odds.get(i / 2 * 3));
            matchJson.put("drawOdd", odds.get(i / 2 * 3 + 1));
            matchJson.put("awayOdd", odds.get(i / 2 * 3 + 2));
            matchJson.put("ts", event.getTs());
            matchJson.put("ss", event.getSs());

            saveJsonToFile(matchJson, "match_" + (i / 2) + ".json");
        }
    }

    private void saveJsonToFile(JSONObject jsonObject, String fileName) {
        try {
            Files.createDirectories(Paths.get(DATA_LAKE_PATH));
            try (FileWriter file = new FileWriter(DATA_LAKE_PATH + fileName)) {
                file.write(jsonObject.toString(4)); // Pretty print with an indent of 4 spaces
            }
        } catch (IOException e) {
            throw new RuntimeException("Error saving JSON to file", e);
        }
    }

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
        return new Match(teams, dateTimes, new ArrayList<>(), "", "", odds);
    }

    private void extractTeamsAndDateTimes(Document connection, List<String> teams, List<String> dateTimes) {
        List<Element> teamElements = connection.select("span.team-name");
        List<Element> dateTimeElements = connection.select("span.date.ui-countdown");
        List<Element> inPlayElements = connection.select("span.event-inplay-state.inplay.ui-time-stop-format");

        for (int i = 0; i < teamElements.size(); i += 2) {
            teams.add(getTeamName(teamElements.get(i)));
            teams.add(getTeamName(teamElements.get(i + 1)));

            int matchIndex = i / 2;

            if (matchIndex < inPlayElements.size()) {
                dateTimes.add(getInPlayTime(inPlayElements.get(matchIndex)));
            } else if (matchIndex < dateTimeElements.size()) {
                dateTimes.add(getDateTime(dateTimeElements.get(matchIndex)));
            } else {
                dateTimes.add("N/A");
            }
        }
    }

    private String getInPlayTime(Element inPlayElement) {
        return inPlayElement.text();
    }

    private void extractOdds(Document connection, List<Double> odds) {
        List<Element> oddsElements = connection.select("span.ui-runner-price.ui-display-decimal-price");

        for (int i = 0; i + 4 < oddsElements.size(); i += 5) {
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