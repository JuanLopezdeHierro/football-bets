package org.sofing.control;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sofing.model.Fixtures;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FootballApiClientImpl {
    private static final String API_URL = "https://v3.football.api-sports.io/fixtures?date=";
    private final String apiKey;

    public FootballApiClientImpl(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Recupera los partidos de La Liga (id=140) de mañana y devuelve
     * una lista de Fixtures con referee, stadium, homeTeam y round.
     */
    public List<Fixtures> fetchLaLigaFixtures() {
        List<Fixtures> fixturesList = new ArrayList<>();

        try {
            // Fecha de mañana
            LocalDate tomorrow = LocalDate.now().plusDays(0);
            String dateParam = tomorrow.format(DateTimeFormatter.ISO_LOCAL_DATE);

            // Conexión
            URL url = new URL(API_URL + dateParam);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("x-rapidapi-host", "v3.football.api-sports.io");
            conn.setRequestProperty("x-rapidapi-key", apiKey);
            conn.setRequestMethod("GET");
            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("API responded with code " + conn.getResponseCode());
            }

            // Parsear array "response"
            JSONArray response = getResponseArray(conn);

            // Filtrar y mapear a Fixtures
            for (int i = 0; i < response.length(); i++) {
                JSONObject item = response.getJSONObject(i);
                JSONObject league = item.getJSONObject("league");
                if (league.getInt("id") != 140) continue;

                JSONObject fixture = item.getJSONObject("fixture");
                String referee = fixture.isNull("referee")
                        ? "N/A"
                        : fixture.getString("referee");

                JSONObject venue = fixture.getJSONObject("venue");
                String stadium = venue.isNull("name")
                        ? "N/A"
                        : venue.getString("name");

                JSONObject teams = item.getJSONObject("teams");
                String homeTeam = teams.getJSONObject("home").getString("name");

                String round = league.optString("round", "N/A");

                fixturesList.add(new Fixtures(referee, stadium, homeTeam, round));
            }

        } catch (IOException e) {
            throw new RuntimeException("Error fetching La Liga fixtures", e);
        }

        return fixturesList;
    }

    /**
     * Convierte una lista de Fixtures a JSONArray, igual que matchDataToJson().
     */
    public JSONArray fixturesToJson(List<Fixtures> fixturesList, boolean includeMeta) {
        JSONArray array = new JSONArray();
        for (Fixtures f : fixturesList) {
            JSONObject obj = new JSONObject();
            obj.put("referee",  f.getReferee());
            obj.put("stadium",  f.getStadium());
            obj.put("homeTeam", f.getHomeTeam());
            obj.put("round",    f.getRound());
            if (includeMeta) {
                obj.put("timeStamp", System.currentTimeMillis());
                obj.put("source",    "APiSports");
            }
            array.put(obj);
        }
        return array;
    }

    /** Lee todo el InputStream y devuelve el JSONArray "response". */
    private static JSONArray getResponseArray(HttpURLConnection conn) throws IOException {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            JSONObject root = new JSONObject(sb.toString());
            return root.getJSONArray("response");
        }
    }
}
