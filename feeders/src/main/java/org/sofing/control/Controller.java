package org.sofing.control;

import jakarta.jms.JMSException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sofing.model.Match; // Asegúrate que esta es tu clase Match del feeder

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory


public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);
    private final FootballWebScrapingImpl footballWebScraping;
    private final EventProvider eventProvider;
    private final ScheduledExecutorService generalScheduler;
    private final ScheduledExecutorService liveMatchScheduler;
    private final Map<String, ScheduledFuture<?>> liveMatchTasks;

    // Directorio para el datamart de partidos en vivo
    private static final String LIVE_DATAMART_BASE_PATH = "datalake_feeder/eventstore/LiveMatchOdds_Datamart";

    public Controller() {
        this.footballWebScraping = new FootballWebScrapingImpl();
        this.eventProvider = new EventProvider(); // Envía a "Match_Topic"
        this.generalScheduler = Executors.newSingleThreadScheduledExecutor();
        this.liveMatchScheduler = Executors.newScheduledThreadPool(5); // Pool para hasta 5 partidos en vivo simultáneos
        this.liveMatchTasks = new ConcurrentHashMap<>();
    }

    public void start() {
        // Tarea para scraping general (cada 10 minutos)
        Runnable generalScrapingTask = () -> {
            logger.info("Iniciando scraping general de partidos futuros/actuales...");
            Match allMatchesData = footballWebScraping.betfairScraping();

            if (allMatchesData.getTeams().isEmpty()) {
                logger.warn("Scraping general no obtuvo datos de partidos.");
                return;
            }

            try {
                // Enviar todos los datos scrapeados al topic principal de ActiveMQ
                // Estos datos serán consumidos por EventReceiver para los archivos YYYYMMDD.events
                // y por el business-unit para la caché general.
                JSONArray generalJsonData = footballWebScraping.matchDataToJson(allMatchesData, false);
                if (!generalJsonData.isEmpty()) {
                    eventProvider.matchInfoArray(generalJsonData); // Asume que matchInfoArray toma un String
                    logger.info("Datos del scraping general ({}) enviados al topic principal de ActiveMQ.", generalJsonData.length());
                }


                // Identificar partidos en vivo y programar/actualizar scrapings específicos para ellos
                processAndScheduleLiveMatchScraping(allMatchesData);

            } catch (JMSException e) {
                logger.error("Error JMS durante el scraping general: {}", e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Error inesperado durante el scraping general: {}", e.getMessage(), e);
            }
        };
        // Ejecutar inmediatamente y luego cada 10 minutos
        generalScheduler.scheduleAtFixedRate(generalScrapingTask, 0, 10, TimeUnit.MINUTES);
        logger.info("Servicio de Feeder iniciado. Scraping general programado cada 10 minutos.");
    }

    private void processAndScheduleLiveMatchScraping(Match allMatchesData) {
        List<String> teams = allMatchesData.getTeams();
        List<String> dateTimes = allMatchesData.getDateTimes();
        List<String> activeLiveKeys = new ArrayList<>(); // Para rastrear qué partidos siguen en vivo

        for (int i = 0; i < teams.size(); i += 2) {
            String homeTeam = teams.get(i);
            String awayTeam = teams.get(i + 1);
            String dateTimeOrMinute = dateTimes.get(i / 2);

            String matchKey = homeTeam.replaceAll("\\s+", "") + "_vs_" + awayTeam.replaceAll("\\s+", "");

            // Usar el patrón para determinar si el formato de dateTimeString indica "en vivo"
            if (FootballWebScrapingImpl.LIVE_TIME_FORMAT_PATTERN.matcher(dateTimeOrMinute).matches()) {
                activeLiveKeys.add(matchKey); // Marcar este partido como actualmente en vivo

                // Si no hay una tarea activa para este partido en vivo, crearla
                if (!liveMatchTasks.containsKey(matchKey) || liveMatchTasks.get(matchKey).isDone()) {
                    logger.info("Detectado partido EN VIVO: {}. Iniciando scraping frecuente.", matchKey);
                    Runnable liveTask = createLiveMatchScrapingTask(homeTeam, awayTeam);
                    ScheduledFuture<?> taskFuture = liveMatchScheduler.scheduleAtFixedRate(liveTask, 0, 10, TimeUnit.SECONDS);
                    liveMatchTasks.put(matchKey, taskFuture);
                }
            }
        }

        // Cancelar tareas de partidos que ya no están en la lista de "en vivo" del scraping general
        List<String> tasksToCancel = new ArrayList<>();
        for (String existingTaskKey : liveMatchTasks.keySet()) {
            if (!activeLiveKeys.contains(existingTaskKey)) {
                tasksToCancel.add(existingTaskKey);
            }
        }
        for (String taskKeyToCancel : tasksToCancel) {
            logger.info("Partido {} ya no parece estar en vivo (no detectado en scraping general). Deteniendo scraping frecuente.", taskKeyToCancel);
            ScheduledFuture<?> task = liveMatchTasks.remove(taskKeyToCancel);
            if (task != null) {
                task.cancel(false);
            }
        }
    }

    private Runnable createLiveMatchScrapingTask(String homeTeam, String awayTeam) {
        String matchKey = homeTeam.replaceAll("\\s+", "") + "_vs_" + awayTeam.replaceAll("\\s+", "");
        return () -> {
            logger.debug("Ejecutando scraping EN VIVO para: {}", matchKey);
            Match liveMatchData = footballWebScraping.scrapeSpecificLiveMatch(homeTeam, awayTeam);

            if (liveMatchData != null && !liveMatchData.getTeams().isEmpty()) {
                // Asegurarse de que el partido sigue realmente en vivo según su dateTime
                String liveDateTime = liveMatchData.getDateTimes().get(0);
                if (FootballWebScrapingImpl.LIVE_TIME_FORMAT_PATTERN.matcher(liveDateTime).matches()) {
                    JSONArray liveJsonData = footballWebScraping.matchDataToJson(liveMatchData, true);

                    if (liveJsonData.length() > 0) {
                        saveLiveMatchDataToDatamart(homeTeam, awayTeam, liveJsonData);

                        try {
                            eventProvider.matchInfoArray(liveJsonData);
                            logger.debug("Datos EN VIVO para {} enviados a ActiveMQ.", matchKey);
                        } catch (JMSException e) {
                            logger.error("Error JMS enviando datos EN VIVO para {}: {}", matchKey, e.getMessage(), e);
                        }
                    }
                } else {
                    logger.info("Partido {} ya no reporta tiempo en formato EN VIVO ({}). Deteniendo scraping frecuente.", matchKey, liveDateTime);
                    ScheduledFuture<?> task = liveMatchTasks.remove(matchKey);
                    if (task != null) task.cancel(false);
                }
            } else {
                logger.warn("No se obtuvieron datos en scraping EN VIVO para {}. Podría haber terminado. Deteniendo scraping frecuente.", matchKey);
                ScheduledFuture<?> task = liveMatchTasks.remove(matchKey);
                if (task != null) task.cancel(false);
            }
        };
    }

    private void saveLiveMatchDataToDatamart(String homeTeam, String awayTeam, JSONArray jsonData) {
        try {
            String dateString = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            // Normalizar nombres de equipo para el nombre de archivo
            String safeHomeTeam = homeTeam.replaceAll("[^a-zA-Z0-9.-]", "_");
            String safeAwayTeam = awayTeam.replaceAll("[^a-zA-Z0-9.-]", "_");
            String fileName = dateString + "_" + safeHomeTeam + "_vs_" + safeAwayTeam + ".events";

            Path outputDir = Paths.get(LIVE_DATAMART_BASE_PATH, dateString); // Subcarpeta por día
            Files.createDirectories(outputDir);
            Path file = outputDir.resolve(fileName);

            // Cada scraping de 10 segundos añade una nueva línea (que es un array JSON con un solo partido)
            Files.writeString(file, jsonData.toString() + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            logger.debug("Datos en vivo para {} guardados en: {}", (safeHomeTeam + "_vs_" + safeAwayTeam), file);
        } catch (IOException e) {
            logger.error("Error guardando datos en vivo en archivo para {} vs {}: {}", homeTeam, awayTeam, e.getMessage(), e);
        }
    }

    public void stop() {
        logger.info("Deteniendo Feeder Controller...");
        if (generalScheduler != null) {
            generalScheduler.shutdown();
        }
        if (liveMatchScheduler != null) {
            liveMatchScheduler.shutdown();
        }
        try {
            if (generalScheduler != null && !generalScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                generalScheduler.shutdownNow();
            }
            if (liveMatchScheduler != null && !liveMatchScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                liveMatchScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupción esperando la finalización de los schedulers.");
            if (generalScheduler != null) generalScheduler.shutdownNow();
            if (liveMatchScheduler != null) liveMatchScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Feeder Controller detenido.");
    }
}
