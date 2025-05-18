package org.sofing.control;

import jakarta.jms.JMSException;
import org.json.JSONArray;
import org.sofing.model.Fixtures;
import org.sofing.model.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    private final FootballWebScrapingImpl footballWebScraping;
    private final FootballApiClientImpl    footballApiClient;
    private final EventProvider            scrapingProvider;
    private final EventProvider            apiProvider;

    private final ScheduledExecutorService scheduler;

    /** Pide la API key, inicializa clientes y providers. */
    public Controller() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese su API Key de API-SPORTS.IO: ");
        String apiKey = scanner.nextLine().trim();

        this.footballWebScraping = new FootballWebScrapingImpl();
        this.footballApiClient   = new FootballApiClientImpl(apiKey);

        String brokerUrl = "tcp://localhost:61616";
        this.scrapingProvider = new EventProvider(brokerUrl, "Match_Topic");
        this.apiProvider      = new EventProvider(brokerUrl, "MatchApi_Topic");

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /** Arranca ambos feeders cada 10 minutos. */
    public void start() {
        // Scraping Betfair → Match_Topic
        scheduler.scheduleAtFixedRate(
                this::runWebScraping,
                0, 1, TimeUnit.MINUTES
        );
        logger.info("Scraping Betfair programado cada 10 minutos en Match_Topic");

        // Feeder API → MatchApi_Topic
        scheduler.scheduleAtFixedRate(
                this::runApiFeeder,
                0, 24, TimeUnit.HOURS
        );
        logger.info("Feeder API programado cada 10 minutos en MatchApi_Topic");
    }

    private void runWebScraping() {
        try {
            logger.info("Ejecutando scraping general de Betfair…");
            Match match = footballWebScraping.betfairScraping();
            JSONArray json = footballWebScraping.matchDataToJson(match);
            if (!json.isEmpty()) {
                scrapingProvider.matchInfoArray(json);
                logger.info("Enviados {} registros a Match_Topic", json.length());
            }
        } catch (JMSException e) {
            logger.error("Error JMS enviando Match_Topic:", e);
        } catch (Exception e) {
            logger.error("Error en scraping Betfair:", e);
        }
    }

    private void runApiFeeder() {
        try {
            logger.info("Ejecutando feeder LaLiga vía API…");
            List<Fixtures> fixtures = footballApiClient.fetchLaLigaFixtures();
            JSONArray json = footballApiClient.fixturesToJson(fixtures, false);
            if (!json.isEmpty()) {
                apiProvider.matchInfoArray(json);
                logger.info("Enviados {} partidos a MatchApi_Topic", json.length());
            }
        } catch (JMSException e) {
            logger.error("Error JMS enviando MatchApi_Topic:", e);
        } catch (Exception e) {
            logger.error("Error en feeder API:", e);
        }
    }

    /** Detiene el scheduler. */
    public void stop() {
        logger.info("Deteniendo Feeder Controller...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrumpido esperando scheduler.", e);
            Thread.currentThread().interrupt();
        }
        logger.info("Feeder Controller detenido.");
    }
}
