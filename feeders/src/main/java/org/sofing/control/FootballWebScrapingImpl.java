package org.sofing.control;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.sofing.model.Match;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class FootballWebScrapingImpl implements FootballWebScraping {

    private static final String URL = "https://www.betfair.es/sport/football/la-liga-espa%C3%B1ola/117";


    @Override
    public Match betfairScraping() {
        List<String> teams = new ArrayList<>();
        List<String> dateTimes = new ArrayList<>();
        List<Double> odds = new ArrayList<>();

        try {
            Document connection = Jsoup.connect(URL).get();
            System.out.println("ConexiÃ³n establecida");

            extractTeamsAndDateTimes(connection, teams, dateTimes);
            extractOdds(connection, odds);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Match(teams, dateTimes,odds, "LaLiga");
    }

    @Override
    public JSONArray matchDataToJson(Match match) {
        JSONArray jsonArray = new JSONArray();
        List<String> teams = match.getTeams();
        List<Double> odds = match.getOdds();
        List<String> dateTimes = match.getDateTimes();

        for (int i = 0; i < teams.size(); i += 2) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("timeStamp", Instant.now().toString());
            jsonObject.put("source", "Betfair");
            jsonObject.put("teamHome", teams.get(i));
            jsonObject.put("teamAway", teams.get(i + 1));
            jsonObject.put("dateTime", dateTimes.get(i / 2));

            int oddsIndex = (i / 2) * 3;
            if (oddsIndex + 2 < odds.size()) {
                jsonObject.put("oddsHome", odds.get(oddsIndex));
                jsonObject.put("oddsDraw", odds.get(oddsIndex + 1));
                jsonObject.put("oddsAway", odds.get(oddsIndex + 2));
            }
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    private void extractTeamsAndDateTimes(Document connection, List<String> teams, List<String> dateTimes) {
        Elements eventRows = connection.select("div.event-information, .event-row");
        if (eventRows.isEmpty());

        for (Element row : eventRows) {
            Elements teamNameElements = row.select("span.team-name");
            Element inPlayElement = row.selectFirst("span.event-inplay-state.inplay, span.gs-o-Timer");
            Element dateTimeElement = row.selectFirst("span.date.ui-countdown, span.matches-time");

            if (teamNameElements.size() >= 2) {
                teams.add(getTeamName(teamNameElements.get(0)));
                teams.add(getTeamName(teamNameElements.get(1)));

                if (inPlayElement != null && !inPlayElement.text().trim().isEmpty()) {
                    dateTimes.add(getInPlayTime(inPlayElement));
                } else if (dateTimeElement != null && !dateTimeElement.text().trim().isEmpty()) {
                    dateTimes.add(getDateTime(dateTimeElement));
                } else {
                    dateTimes.add("N/A");
                }
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