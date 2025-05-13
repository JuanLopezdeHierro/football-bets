package org.sofing.control;

import jakarta.jms.JMSException;
import org.sofing.model.Match;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller {
    private final FootballWebScrapingImpl footballWebScraping;
    private final EventProvider eventProvider;
    private final ScheduledExecutorService scheduler;

    public Controller() {
        this.footballWebScraping = new FootballWebScrapingImpl();
        this.eventProvider = new EventProvider();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        Runnable task = () -> {
            Match match = footballWebScraping.betfairScraping();
            try {
                eventProvider.matchInfoArray(footballWebScraping.matchDataToJson(match));
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Datos enviados al topic de ActiveMQ.");
        };

        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
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