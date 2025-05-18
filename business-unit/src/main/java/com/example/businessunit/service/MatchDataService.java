package com.example.businessunit.service;

import com.example.businessunit.model.MatchEvent;
import com.example.businessunit.model.MatchEventDTO;
import com.example.businessunit.model.MatchStatus;
import com.example.businessunit.util.DateTimeUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
// import java.util.Collections; // No se usa directamente, se puede quitar si no es necesario en otro lado
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // Importar para Objects.equals
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MatchDataService {

    private static final Logger logger = LoggerFactory.getLogger(MatchDataService.class);
    private final ObjectMapper objectMapper;
    private final List<MatchEvent> matchEventsCache = new ArrayList<>();
    private final Path EVENTS_DIRECTORY_PATH = Paths.get("datalake/eventstore/Match_Topic/default");
    private static final Pattern LIVE_TIME_PATTERN = Pattern.compile("^(\\d{1,3}['+]?\\d*|HT|FT|Descanso)$", Pattern.CASE_INSENSITIVE);
    private final DateTimeFormatter DAILY_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private FileTime lastDatalakeFileReadTimestamp = null;
    private Path currentDailyEventFilePath = null;

    private final MatchSseService sseService;

    @Autowired
    public MatchDataService(ObjectMapper objectMapper, MatchSseService sseService) {
        this.objectMapper = objectMapper;
        this.sseService = sseService;
    }

    @PostConstruct
    public void initializeMatchEvents() {
        logger.info("Initializing historic match event load from datalake...");
        loadAllHistoricalEventsFromDatalake();
        updateCurrentDailyEventFilePath();
    }

    private void loadAllHistoricalEventsFromDatalake() {
        Map<String, MatchEvent> uniqueMatchesMap = new HashMap<>();
        List<Path> eventFiles = List.of();

        if (!Files.isDirectory(EVENTS_DIRECTORY_PATH)) {
            logger.warn("Datalake event directory does not exist: {}", EVENTS_DIRECTORY_PATH.toAbsolutePath());
            return;
        }
        try (Stream<Path> paths = Files.walk(EVENTS_DIRECTORY_PATH)) {
            eventFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".events"))
                    .collect(Collectors.toList());

            for (Path filePath : eventFiles) {
                processDatalakeFile(filePath, uniqueMatchesMap);
            }
        } catch (IOException e) {
            logger.error("Error accessing event directory: " + EVENTS_DIRECTORY_PATH, e);
        }

        synchronized (this.matchEventsCache) {
            this.matchEventsCache.clear();
            this.matchEventsCache.addAll(uniqueMatchesMap.values());
            logger.info("✅ {} unique events loaded initially from {} datalake files. Total cache size: {}", uniqueMatchesMap.size(), eventFiles.size(), this.matchEventsCache.size());
        }
    }

    private void updateCurrentDailyEventFilePath() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String dailyFileName = today.format(DAILY_FILE_FORMATTER) + ".events";
        this.currentDailyEventFilePath = EVENTS_DIRECTORY_PATH.resolve(dailyFileName);
        logger.info("Monitoring daily event file: {}", this.currentDailyEventFilePath);
    }

    @Scheduled(fixedDelayString = "PT10S")
    public void pollDailyDatalakeFile() {
        if (this.currentDailyEventFilePath == null || !Files.exists(this.currentDailyEventFilePath)) {
            updateCurrentDailyEventFilePath();
            if (this.currentDailyEventFilePath == null || !Files.exists(this.currentDailyEventFilePath)) {
                logger.trace("Daily event file not found or not yet created: {}", this.currentDailyEventFilePath);
                return;
            }
        }

        try {
            FileTime currentFileTimestamp = Files.getLastModifiedTime(this.currentDailyEventFilePath);
            // CORRECCIÓN 1: Comparación de FileTime
            if (lastDatalakeFileReadTimestamp != null && currentFileTimestamp.compareTo(lastDatalakeFileReadTimestamp) <= 0) {
                logger.trace("No changes in daily event file {} since last read.", this.currentDailyEventFilePath.getFileName());
                return;
            }

            logger.info("Detected change in daily event file: {}. Processing...", this.currentDailyEventFilePath.getFileName());

            Map<String, MatchEvent> dailyFileMatchesMap = new HashMap<>();
            processDatalakeFile(this.currentDailyEventFilePath, dailyFileMatchesMap);

            if (!dailyFileMatchesMap.isEmpty()) {
                boolean cacheUpdated = false;
                synchronized (this.matchEventsCache) {
                    for (MatchEvent eventFromFile : dailyFileMatchesMap.values()) {
                        cacheUpdated |= updateOrAddEventInCache(eventFromFile);
                    }
                }
                if (cacheUpdated) {
                    logger.info("Cache updated from daily datalake file. Sending SSE update.");
                    sseService.sendUpdate(getAllMatchEvents());
                }
            }
            lastDatalakeFileReadTimestamp = currentFileTimestamp;

        } catch (IOException e) {
            logger.error("Error polling daily datalake file: {}", this.currentDailyEventFilePath, e);
        }
    }

    private void processDatalakeFile(Path filePath, Map<String, MatchEvent> matchesMapToUpdate) {
        logger.debug("Processing datalake file: {}", filePath);
        try {
            List<String> lines = Files.readAllLines(filePath);
            if (!lines.isEmpty()) {
                String lastLineWithData = null;
                for (int i = lines.size() - 1; i >= 0; i--) {
                    if (!lines.get(i).trim().isEmpty()) {
                        lastLineWithData = lines.get(i);
                        break;
                    }
                }
                if (lastLineWithData != null) {
                    logger.debug("Processing last JSON array from file {}: '{}'", filePath, lastLineWithData);
                    processJsonLine(lastLineWithData, filePath, matchesMapToUpdate);
                } else {
                    logger.debug("File {} is empty or contains only empty lines.", filePath);
                }
            }
        } catch (IOException e) {
            logger.error("Error reading event file: " + filePath, e);
        }
    }

    private void processJsonLine(String jsonLine, Path sourceFilePath, Map<String, MatchEvent> matchesMapToUpdate) {
        try {
            List<MatchEventDTO> dtos = objectMapper.readValue(jsonLine, new TypeReference<List<MatchEventDTO>>() {});
            for (MatchEventDTO dto : dtos) {
                ZonedDateTime parsedEventTimestamp = dto.getTimeStamp();
                String matchKey = generateMatchKey(dto.getTeamHome(), dto.getTeamAway(), dto.getDateTime(), parsedEventTimestamp);

                MatchStatus status = determineStatusFromDateTimeString(dto.getDateTime(), parsedEventTimestamp);
                String homeLogoUrl = generateLogoUrlFromName(dto.getTeamHome()); // Asegúrate que este método exista en la clase
                String awayLogoUrl = generateLogoUrlFromName(dto.getTeamAway()); // Asegúrate que este método exista en la clase

                MatchEvent event = new MatchEvent(
                        parsedEventTimestamp, dto.getDateTime(),
                        dto.getOddsDraw() != null ? dto.getOddsDraw() : 0.0,
                        dto.getTeamAway(),
                        dto.getOddsAway() != null ? dto.getOddsAway() : 0.0,
                        dto.getTeamHome(),
                        "datalake_file:" + sourceFilePath.getFileName().toString(),
                        dto.getOddsHome() != null ? dto.getOddsHome() : 0.0,
                        status,
                        homeLogoUrl, awayLogoUrl
                );
                if (status == MatchStatus.LIVE) {
                    event.setLiveTimeDisplay(dto.getDateTime());
                }
                matchesMapToUpdate.put(matchKey, event);
            }
        } catch (Exception e) {
            logger.error("Error parsing JSON line from file {}: '{}'. Error: {}", sourceFilePath, jsonLine, e.getMessage(), e);
        }
    }

    // Asegúrate de que estos métodos privados estén definidos DENTRO de la clase MatchDataService
    private String generateLogoUrlFromName(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) {
            return "/images/logos/default_logo.png"; // O null, o un logo por defecto real
        }
        return "/images/logos/" + teamName.replace(" ", "_") + ".png";
    }

    private String generateMatchKey(String teamHome, String teamAway, String dateTimeString, ZonedDateTime eventTimestamp) {
        String dateComponent;
        if (teamHome == null || teamAway == null) return "invalid_key_teams_null";

        if (dateTimeString != null && LIVE_TIME_PATTERN.matcher(dateTimeString).matches()) {
            dateComponent = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else {
            ZonedDateTime matchTime = DateTimeUtil.parseCustomDateTime(dateTimeString);
            if (matchTime != null) {
                dateComponent = matchTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            } else if (eventTimestamp != null) {
                dateComponent = eventTimestamp.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                dateComponent = "unknown_date";
            }
        }
        return teamHome.toLowerCase() + "|" + teamAway.toLowerCase() + "|" + dateComponent;
    }

    private MatchStatus determineStatusFromDateTimeString(String dateTimeString, ZonedDateTime eventTimestampForFallback) {
        if (dateTimeString != null && LIVE_TIME_PATTERN.matcher(dateTimeString).matches()) {
            return MatchStatus.LIVE;
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime matchStartTime = DateTimeUtil.parseCustomDateTime(dateTimeString);

        if (matchStartTime == null) {
            if (eventTimestampForFallback != null) {
                return eventTimestampForFallback.isBefore(now) ? MatchStatus.HISTORICAL : MatchStatus.UPCOMING;
            }
            return MatchStatus.SCHEDULED;
        }

        if (now.isBefore(matchStartTime)) {
            return MatchStatus.UPCOMING;
        } else {
            return MatchStatus.HISTORICAL;
        }
    }

    private synchronized boolean updateOrAddEventInCache(MatchEvent eventData) {
        String matchKey = generateMatchKey(eventData.getTeamHome(), eventData.getTeamAway(), eventData.getDateTimeString(), eventData.getTimeStamp());

        Optional<MatchEvent> existingEventOpt = matchEventsCache.stream()
                .filter(e -> matchKey.equals(generateMatchKey(e.getTeamHome(), e.getTeamAway(), e.getDateTimeString(), e.getTimeStamp())))
                .findFirst();

        if (existingEventOpt.isPresent()) {
            MatchEvent existingEvent = existingEventOpt.get();
            boolean changed = false;
            if (existingEvent.getOddsDraw() != eventData.getOddsDraw()) { existingEvent.setOddsDraw(eventData.getOddsDraw()); changed = true; }
            if (existingEvent.getOddsHome() != eventData.getOddsHome()) { existingEvent.setOddsHome(eventData.getOddsHome()); changed = true; }
            if (existingEvent.getOddsAway() != eventData.getOddsAway()) { existingEvent.setOddsAway(eventData.getOddsAway()); changed = true; }
            if (existingEvent.getMatchStatus() != eventData.getMatchStatus()) { existingEvent.setMatchStatus(eventData.getMatchStatus()); changed = true; }
            if (!Objects.equals(existingEvent.getLiveTimeDisplay(), eventData.getLiveTimeDisplay())) { existingEvent.setLiveTimeDisplay(eventData.getLiveTimeDisplay()); changed = true; }

            // Solo actualiza dateTimeString si el estado no es LIVE, para evitar sobreescribir el tiempo de juego con una fecha.
            if (eventData.getMatchStatus() != MatchStatus.LIVE && !Objects.equals(existingEvent.getDateTimeString(), eventData.getDateTimeString())) {
                // existingEvent.setDateTimeString(eventData.getDateTimeString()); // Comentado por riesgo de cambiar la clave.
                changed = true; // Pero indica que hubo una diferencia.
            }

            existingEvent.setSource(eventData.getSource());
            if (!Objects.equals(existingEvent.getHomeTeamLogoUrl(), eventData.getHomeTeamLogoUrl())) { existingEvent.setHomeTeamLogoUrl(eventData.getHomeTeamLogoUrl()); changed = true; }
            if (!Objects.equals(existingEvent.getAwayTeamLogoUrl(), eventData.getAwayTeamLogoUrl())) { existingEvent.setAwayTeamLogoUrl(eventData.getAwayTeamLogoUrl()); changed = true; }

            if (changed) {
                logger.info("Match updated in cache (Source: {}): {} vs {} - Status: {}, OddsH: {}",
                        eventData.getSource(), existingEvent.getTeamHome(), existingEvent.getTeamAway(), existingEvent.getMatchStatus(), existingEvent.getOddsHome());
            }
            return changed;
        } else {
            matchEventsCache.add(eventData);
            logger.info("New match added to cache (Source: {}): {} vs {} - Status: {}, OddsH: {}",
                    eventData.getSource(), eventData.getTeamHome(), eventData.getTeamAway(), eventData.getMatchStatus(), eventData.getOddsHome());
            return true;
        }
    }

    public synchronized void processRealtimeEvent(MatchEvent eventDataFromMQ) {
        MatchStatus determinedStatus = determineStatusFromDateTimeString(eventDataFromMQ.getDateTimeString(), eventDataFromMQ.getTimeStamp());
        eventDataFromMQ.setMatchStatus(determinedStatus);
        if (determinedStatus == MatchStatus.LIVE) {
            eventDataFromMQ.setLiveTimeDisplay(eventDataFromMQ.getDateTimeString());
        } else {
            eventDataFromMQ.setLiveTimeDisplay(null);
        }

        if (eventDataFromMQ.getHomeTeamLogoUrl() == null && eventDataFromMQ.getTeamHome() != null) {
            eventDataFromMQ.setHomeTeamLogoUrl(generateLogoUrlFromName(eventDataFromMQ.getTeamHome()));
        }
        if (eventDataFromMQ.getAwayTeamLogoUrl() == null && eventDataFromMQ.getTeamAway() != null) {
            eventDataFromMQ.setAwayTeamLogoUrl(generateLogoUrlFromName(eventDataFromMQ.getTeamAway()));
        }

        updateOrAddEventInCache(eventDataFromMQ);
        // El SSE update se maneja centralizadamente en el MatchTopicListener después de procesar el lote,
        // o en pollDailyDatalakeFile después de procesar el archivo.
    }

    private void refreshMatchStatuses() {
        boolean changedOverall = false;
        synchronized (this.matchEventsCache) {
            for (MatchEvent event : this.matchEventsCache) {
                MatchStatus oldStatus = event.getMatchStatus();
                String oldLiveDisplay = event.getLiveTimeDisplay();

                MatchStatus newStatus = determineStatusFromDateTimeString(event.getDateTimeString(), event.getTimeStamp());
                if (oldStatus != newStatus) {
                    event.setMatchStatus(newStatus);
                    changedOverall = true;
                }

                if (newStatus == MatchStatus.LIVE) {
                    if (event.getDateTimeString() != null && LIVE_TIME_PATTERN.matcher(event.getDateTimeString()).matches()) {
                        if (!event.getDateTimeString().equals(oldLiveDisplay)) {
                            event.setLiveTimeDisplay(event.getDateTimeString());
                            changedOverall = true;
                        }
                    } else if (oldLiveDisplay == null) {
                        event.setLiveTimeDisplay("En Vivo"); // Placeholder si no hay tiempo específico
                        changedOverall = true;
                    }
                } else {
                    if (oldLiveDisplay != null) {
                        event.setLiveTimeDisplay(null);
                        changedOverall = true;
                    }
                }
            }
        }
        if (changedOverall) {
            logger.debug("Match statuses refreshed, potential changes detected.");
        }
    }

    public List<MatchEvent> getLiveMatches() {
        refreshMatchStatuses();
        synchronized (this.matchEventsCache) {
            return matchEventsCache.stream()
                    .filter(event -> event.getMatchStatus() == MatchStatus.LIVE)
                    .collect(Collectors.toList());
        }
    }

    public List<MatchEvent> getUpcomingMatches() {
        refreshMatchStatuses();
        synchronized (this.matchEventsCache) {
            return matchEventsCache.stream()
                    .filter(event -> event.getMatchStatus() == MatchStatus.UPCOMING || event.getMatchStatus() == MatchStatus.SCHEDULED)
                    .sorted((e1, e2) -> {
                        ZonedDateTime dt1 = DateTimeUtil.parseCustomDateTime(e1.getDateTimeString());
                        ZonedDateTime dt2 = DateTimeUtil.parseCustomDateTime(e2.getDateTimeString());
                        if (dt1 == null && dt2 == null) return 0;
                        if (dt1 == null) return 1;
                        if (dt2 == null) return -1;
                        return dt1.compareTo(dt2);
                    })
                    .collect(Collectors.toList());
        }
    }

    public List<MatchEvent> getHistoricalMatches() {
        refreshMatchStatuses();
        synchronized (this.matchEventsCache) {
            return matchEventsCache.stream()
                    .filter(event -> event.getMatchStatus() == MatchStatus.HISTORICAL)
                    .sorted((e1, e2) -> {
                        ZonedDateTime dt1 = DateTimeUtil.parseCustomDateTime(e1.getDateTimeString());
                        ZonedDateTime dt2 = DateTimeUtil.parseCustomDateTime(e2.getDateTimeString());
                        if (dt1 == null && dt2 == null) return 0;
                        if (dt1 == null) return 1;
                        if (dt2 == null) return -1;
                        return dt2.compareTo(dt1);
                    })
                    .collect(Collectors.toList());
        }
    }
    public List<MatchEvent> getAllMatchEvents() {
        refreshMatchStatuses();
        synchronized (this.matchEventsCache) {
            return new ArrayList<>(matchEventsCache);
        }
    }

    public Optional<MatchEvent> getMatchEventById(String id) {
        if (id == null || id.trim().isEmpty()) return Optional.empty();
        refreshMatchStatuses();
        synchronized (this.matchEventsCache) {
            return matchEventsCache.stream()
                    .filter(event -> id.equals(event.getId()))
                    .findFirst();
        }
    }
}