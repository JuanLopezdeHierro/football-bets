package org.sofing.control;

import org.sofing.model.Match;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class FootballApiClientImpl {
    String apiUrl = "https://v3.football.api-sports.io/teams?name=";

    public void apiSportsRequest() {
        String apiKey = "f9baa6b41aa2db169d13361b5e2a1c4e";

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("x-rapidapi-host", "v3.football.api-sports.io");
            connection.setRequestProperty("x-rapidapi-key", apiKey);
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void urlConstructor() {
        FootballWebScraping footballWebScraping = new FootballWebScraping();
        Match scrapingResult = footballWebScraping.betfairScraping();
        List<String> teams = scrapingResult.getTeams();

        for (int i = 1; i < teams.size(); i += 2) { // Equipos impares
            String teamName = teams.get(i);
            String url = apiUrl + teamName;
            System.out.println(url);
        }
    }
}