package org.sofing.control;

import org.sofing.model.Match;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller {
    private final FootballWebScraping footballWebScraping;
    private final FootballApiClientImpl footballApiClient;
    private final DataStorage dataStorage;
    private final ScheduledExecutorService scheduler;

    public Controller(String apiKey) {
        this.footballWebScraping = new FootballWebScraping();
        this.footballApiClient = new FootballApiClientImpl(apiKey);
        this.dataStorage = new DataStorage();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        Runnable task = () -> {
            Match match = footballWebScraping.betfairScraping();
            footballApiClient.updateMatchFields(match);
            dataStorage.insertMatch(match);
            System.out.println("Datos guardados en la base de datos.");
        };

        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}