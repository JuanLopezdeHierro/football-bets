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

public class FootballApiClientImpl {
    private final String apiUrl = "https://v3.football.api-sports.io/teams?country=Spain";
    private final String apiKey;
    private final FootballWebScraping footballWebScraping = new FootballWebScraping();

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

    public FootballApiClientImpl(String apiKey) {
        this.apiKey = apiKey;
    }

    public void updateMatchFields(Match match) {
        List<String> fields = new ArrayList<>();

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("x-rapidapi-host", "v3.football.api-sports.io");
            connection.setRequestProperty("x-rapidapi-key", apiKey);
            connection.setRequestMethod("GET");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                JSONArray responseArray = getObjects(connection);
                if (!responseArray.isEmpty()) {
                    List<String> teams = match.getTeams();
                    fields.addAll(fileSearcher(teams, responseArray));
                    match.setFields(fields);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> fileSearcher(List<String> teams, JSONArray responseArray) {
        List<String> matchFields = new ArrayList<>();
        for (int i = 0; i < teams.size(); i += 2) {
            String teamName = teams.get(i);
            String teamNameNormalized = TEAM_NAME_MAPPING.getOrDefault(teamName, teamName);

            for (int j = 0; j < responseArray.length(); j++) {
                JSONObject teamObj = responseArray.getJSONObject(j).getJSONObject("team");
                String apiTeamName = teamObj.getString("name");

                if (apiTeamName.equalsIgnoreCase(teamNameNormalized)) {
                    JSONObject venue = responseArray.getJSONObject(j).getJSONObject("venue");
                    matchFields.add(venue.getString("name"));
                    break;
                }
            }
        }
        return matchFields;
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
}