package org.sofing.control;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sofing.model.Match;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.swing.UIManager.put;

public class FootballApiClientImpl {
    String apiUrl = "https://v3.football.api-sports.io/teams?name=";

    private static final Map<String, String> TEAM_NAME_MAPPING = new HashMap<>() {{
        put("FC Barcelona", "Barcelona");
        put("Real Madrid", "Real Madrid");
        put("Atlético de Madrid", "Atletico Madrid");
        put("Athletic de Bilbao", "Athletic Club");
        put("Real Sociedad", "Real Sociedad");
        put("Betis", "Real Betis");
        put("Villarreal", "Villarreal");
        put("Las Palmas", "Las Palmas");
        put("Valencia", "Valencia");
        put("Rayo Vallecano", "Rayo Vallecano");
        put("Osasuna", "Osasuna");
        put("Getafe", "Getafe");
        put("Alavés", "Alaves");
        put("Sevilla", "Sevilla");
        put("Celta de Vigo", "Celta Vigo");
        put("Mallorca", "Mallorca");
        put("Girona", "Girona");
        put("Espanyol", "Espanyol");
        put("Valladolid", "Valladolid");
        put("Leganés", "Leganes");
    }};

    public List<String> getMatchFields() {
        String apiKey = "f9baa6b41aa2db169d13361b5e2a1c4e";
        List<String> fields = new ArrayList<>();
        List<String> urls = urlsConstructor();

        for (String urlStr : urls) {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("x-rapidapi-host", "v3.football.api-sports.io");
                connection.setRequestProperty("x-rapidapi-key", apiKey);
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    JSONArray responseArray = getObjects(connection);
                    if (!responseArray.isEmpty()) {
                        JSONObject venue = responseArray.getJSONObject(0).getJSONObject("venue");
                        fields.add(venue.getString("name"));
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return fields;
    }

    private static JSONArray getObjects(HttpURLConnection connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(content.toString());
        return jsonResponse.getJSONArray("response");
    }
    public List<String> urlsConstructor() {
        FootballWebScraping footballWebScraping = new FootballWebScraping();
        Match scrapingResult = footballWebScraping.betfairScraping();
        List<String> teams = scrapingResult.getTeams();
        List<String> urls = new ArrayList<>();

        for (int i = 0; i < teams.size(); i += 2) {
            String teamName = teams.get(i);
            String normalizedName = TEAM_NAME_MAPPING.getOrDefault(teamName, teamName);
            normalizedName = normalizedName.replace(" ", "%20");
            urls.add(apiUrl + normalizedName);
        }
        return urls;
    }
}