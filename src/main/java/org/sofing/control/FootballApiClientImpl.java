package org.sofing.control;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class FootballApiClientImpl {
    String apiKey = "f9baa6b41aa2db169d13361b5e2a1c4e";
    String apiUrl = "https://v3.football.api-sports.io/fixtures?date=2025-03-22";

    public void footballApiRequest() {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection;
            connection = (HttpURLConnection) url.openConnection();
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
}
