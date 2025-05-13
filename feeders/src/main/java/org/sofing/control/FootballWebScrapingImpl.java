package org.sofing.control;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofing.model.Match; // Asegúrate que la ruta a tu modelo Match es correcta

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FootballWebScrapingImpl{

    private static final Logger logger = LoggerFactory.getLogger(FootballWebScrapingImpl.class);
    private static final String URL = "https://www.betfair.es/sport/football/la-liga-espa%C3%B1ola/117";
    // Patrón para identificar tiempos en vivo como "34'", "HT", "Descanso", "90+2'"
    public static final Pattern LIVE_TIME_FORMAT_PATTERN = Pattern.compile("^(\\d{1,3}['+]?\\d*|HT|FT|Descanso)$", Pattern.CASE_INSENSITIVE);

    public Match betfairScraping() {
        List<String> teams = new ArrayList<>();
        List<String> dateTimes = new ArrayList<>();
        // No necesitamos isInPlayFlags si lo determinamos por el formato de dateTime
        List<Double> odds = new ArrayList<>();

        try {
            Document connection = Jsoup.connect(URL).get();
            logger.debug("Conexión establecida para scraping general en Betfair.");

            extractTeamsAndDateTimes(connection, teams, dateTimes);
            extractOdds(connection, odds, teams.size() / 2); // Pasamos el número de partidos para alinear cuotas

        } catch (IOException e) {
            logger.error("Error durante el scraping general en Betfair: {}", e.getMessage(), e);
            return new Match(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "", "LaLiga");
        }
        return new Match(teams, dateTimes, odds, new ArrayList<>(), "", "LaLiga");
    }


    public Match scrapeSpecificLiveMatch(String homeTeamTarget, String awayTeamTarget) {
        logger.debug("Iniciando scraping específico para partido EN VIVO: {} vs {}", homeTeamTarget, awayTeamTarget);
        List<String> teams = new ArrayList<>();
        List<String> dateTimes = new ArrayList<>();
        List<Double> odds = new ArrayList<>();

        try {
            Document connection = Jsoup.connect(URL).get();
            Elements eventRows = connection.select("div.event-information, .event-row"); // Ajusta este selector según la estructura de Betfair

            for (Element row : eventRows) {
                Elements teamNameElements = row.select("span.team-name");
                if (teamNameElements.size() >= 2) {
                    String homeTeam = getTeamName(teamNameElements.get(0));
                    String awayTeam = getTeamName(teamNameElements.get(1));

                    if (homeTeam.equalsIgnoreCase(homeTeamTarget) && awayTeam.equalsIgnoreCase(awayTeamTarget)) {
                        teams.add(homeTeam);
                        teams.add(awayTeam);

                        Element inPlayElement = row.selectFirst("span.event-inplay-state.inplay, span.gs-o-Timer"); // Para tiempo en juego
                        if (inPlayElement != null && !inPlayElement.text().trim().isEmpty()) {
                            dateTimes.add(getInPlayTime(inPlayElement));
                        } else {
                            // Si no se encuentra tiempo en vivo para un partido que se esperaba en vivo, marcar como N/A o error
                            logger.warn("No se encontró tiempo en vivo para el partido específico {} vs {}", homeTeam, awayTeam);
                            dateTimes.add("LIVE_ERR"); // O alguna indicación de que se esperaba en vivo pero no se encontró tiempo
                        }

                        // Extraer cuotas SOLO para este partido
                        Elements oddsElementsInRow = row.select("span.ui-runner-price"); // Ajusta este selector
                        if (oddsElementsInRow.size() >= 3) {
                            try {
                                odds.add(getOdds(oddsElementsInRow.get(0)));
                                odds.add(getOdds(oddsElementsInRow.get(1)));
                                odds.add(getOdds(oddsElementsInRow.get(2)));
                            } catch (NumberFormatException e) {
                                logger.warn("Error al parsear cuotas para partido en vivo {} vs {}: {}", homeTeam, awayTeam, oddsElementsInRow.text());
                                addFallbackOdds(odds);
                            }
                        } else {
                            logger.warn("No se encontraron 3 cuotas para partido en vivo {} vs {}", homeTeam, awayTeam);
                            addFallbackOdds(odds);
                        }
                        break; // Partido encontrado y procesado
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error durante scraping específico para {} vs {}: {}", homeTeamTarget, awayTeamTarget, e.getMessage(), e);
        }
        if (teams.isEmpty()) {
            logger.warn("No se encontró el partido específico EN VIVO: {} vs {}", homeTeamTarget, awayTeamTarget);
        }
        return new Match(teams, dateTimes, odds, new ArrayList<>(), "", "LaLiga");
    }


    // El parámetro isLiveSpecific ahora se usa para determinar qué campos incluir
    public JSONArray matchDataToJson(Match match, boolean isLiveSpecificDatamart) {
        JSONArray jsonArray = new JSONArray();
        List<String> teams = match.getTeams();
        List<Double> odds = match.getOdds();
        List<String> dateTimes = match.getDateTimes();

        for (int i = 0; i < teams.size(); i += 2) {
            JSONObject jsonObject = new JSONObject();
            String teamHome = teams.get(i);
            String teamAway = teams.get(i + 1);
            String currentDateTime = dateTimes.get(i / 2);

            jsonObject.put("teamHome", teamHome);
            jsonObject.put("teamAway", teamAway);
            jsonObject.put("dateTime", currentDateTime); // "34'", "HT", "15 May 19:00", etc.

            int oddsIndex = (i / 2) * 3;
            if (oddsIndex + 2 < odds.size()) {
                jsonObject.put("oddsHome", odds.get(oddsIndex));
                jsonObject.put("oddsDraw", odds.get(oddsIndex + 1));
                jsonObject.put("oddsAway", odds.get(oddsIndex + 2));
            } else {
                jsonObject.put("oddsHome", JSONObject.NULL);
                jsonObject.put("oddsDraw", JSONObject.NULL);
                jsonObject.put("oddsAway", JSONObject.NULL);
            }

            if (!isLiveSpecificDatamart) { // Para el topic general, incluir todos los campos
                jsonObject.put("timeStamp", Instant.now().toString());
                jsonObject.put("source", "Betfair");
            }
            // Para el datamart de partidos en vivo, solo dateTime, equipos y cuotas son estrictamente necesarios
            // pero el DTO en business-unit espera timeStamp y source, así que los incluimos por ahora
            // o ajustamos el DTO/lógica en business-unit para el datamart.
            // Por simplicidad actual, los incluimos también para los mensajes de datamart.
            else {
                jsonObject.put("timeStamp", Instant.now().toString()); // Timestamp del evento de scraping específico
                jsonObject.put("source", "Betfair_LIVE"); // Fuente diferente para identificar
            }


            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    private void extractTeamsAndDateTimes(Document connection, List<String> teams, List<String> dateTimes) {
        Elements eventRows = connection.select("div.event-information, .event-row");
        if (eventRows.isEmpty()) logger.warn("Scraping: No se encontraron filas de eventos.");

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
        return inPlayElement.text().trim();
    }

    private void extractOdds(Document connection, List<Double> odds, int expectedMatches) {
        Elements eventRows = connection.select("div.event-information, .event-row");
        int oddsFoundCount = 0;
        for (Element row : eventRows) {
            Elements oddsElementsInRow = row.select("span.ui-runner-price");
            if (oddsElementsInRow.size() >= 3) {
                try {
                    odds.add(getOdds(oddsElementsInRow.get(0)));
                    odds.add(getOdds(oddsElementsInRow.get(1)));
                    odds.add(getOdds(oddsElementsInRow.get(2)));
                    oddsFoundCount++;
                } catch (NumberFormatException e) {
                    logger.warn("Error al parsear cuotas. Texto: {}", oddsElementsInRow.text(), e);
                    addFallbackOdds(odds);
                }
            } else {
                addFallbackOdds(odds);
            }
        }
        // Rellenar con ceros si no se encontraron suficientes grupos de cuotas
        while (odds.size() < expectedMatches * 3) {
            addFallbackOdds(odds);
        }
        if (oddsFoundCount < expectedMatches && expectedMatches > 0) {
            logger.warn("Se esperaban cuotas para {} partidos, pero solo se encontraron para {}.", expectedMatches, oddsFoundCount);
        }
    }
    private void addFallbackOdds(List<Double> odds) {
        odds.add(0.0); odds.add(0.0); odds.add(0.0);
    }
    private String getTeamName(Element team) { return team.text().trim(); }
    private double getOdds(Element oddElement) {
        String oddText = oddElement.text().trim();
        if (oddText.isEmpty() || oddText.equals("-")) return 0.0;
        try { return Double.parseDouble(oddText); }
        catch (NumberFormatException e) { logger.error("No se pudo parsear la cuota: '{}'", oddText, e); return 0.0; }
    }
    private String getDateTime(Element dateTime) { return dateTime.text().trim(); }
}
