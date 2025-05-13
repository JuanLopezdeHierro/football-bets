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
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    // Patrón para identificar formatos como "34'", "HT", "90+2'", "Descanso"
    private static final Pattern LIVE_TIME_PATTERN = Pattern.compile("^(\\d{1,3}['+]?\\d*|HT|FT|Descanso)$", Pattern.CASE_INSENSITIVE);

    @Autowired
    public MatchDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeMatchEvents() {
        logger.info("Inicializando carga de eventos de partidos desde datalake...");
        Map<String, MatchEvent> uniqueMatchesMap = new HashMap<>();
        List<Path> eventFiles = new ArrayList<>();

        if (!Files.isDirectory(EVENTS_DIRECTORY_PATH)) {
            logger.warn("El directorio de eventos del datalake no existe: {}", EVENTS_DIRECTORY_PATH.toAbsolutePath());
        } else {
            try (Stream<Path> paths = Files.walk(EVENTS_DIRECTORY_PATH)) {
                eventFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".events"))
                        .collect(Collectors.toList());

                for (Path filePath : eventFiles) {
                    logger.debug("Procesando archivo histórico/datalake: {}", filePath);
                    try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().isEmpty()) continue;
                            try {
                                List<MatchEventDTO> dtos = objectMapper.readValue(line, new TypeReference<List<MatchEventDTO>>() {});
                                for (MatchEventDTO dto : dtos) {
                                    ZonedDateTime parsedEventTimestamp = dto.timeStamp;
                                    String matchKey = generateMatchKey(dto.teamHome, dto.teamAway, dto.dateTime, parsedEventTimestamp);

                                    MatchEvent currentEventInMap = uniqueMatchesMap.get(matchKey);
                                    if (currentEventInMap == null || (parsedEventTimestamp != null && parsedEventTimestamp.isAfter(currentEventInMap.getTimeStamp()))) {
                                        MatchStatus status = determineStatusFromDateTimeString(dto.dateTime, parsedEventTimestamp);
                                        MatchEvent event = new MatchEvent(
                                                parsedEventTimestamp, dto.dateTime, dto.oddsDraw, dto.teamAway,
                                                dto.oddsAway, dto.teamHome, dto.source, dto.oddsHome, status
                                        );
                                        if (status == MatchStatus.LIVE) {
                                            event.setLiveTimeDisplay(dto.dateTime);
                                        }
                                        uniqueMatchesMap.put(matchKey, event);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Error parseando línea JSON del archivo {}: '{}'. Error: {}", filePath, line, e.getMessage());
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Error leyendo el archivo de eventos: " + filePath, e);
                    }
                }
            } catch (IOException e) {
                logger.error("Error al acceder al directorio de eventos: " + EVENTS_DIRECTORY_PATH, e);
            }
        }
        synchronized (this.matchEventsCache) {
            this.matchEventsCache.clear();
            this.matchEventsCache.addAll(uniqueMatchesMap.values());
            logger.info("✅ {} eventos únicos cargados inicialmente desde {} archivos del datalake. Tamaño total de caché: {}", uniqueMatchesMap.size(), eventFiles.size(), this.matchEventsCache.size());
        }
    }

    private String generateMatchKey(String teamHome, String teamAway, String dateTimeString, ZonedDateTime eventTimestamp) {
        String dateComponent;
        if (dateTimeString != null && LIVE_TIME_PATTERN.matcher(dateTimeString).matches()) {
            // Para partidos en vivo, la clave de fecha es el día actual para agrupar los que ocurren hoy
            dateComponent = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else {
            ZonedDateTime matchTime = DateTimeUtil.parseCustomDateTime(dateTimeString);
            if (matchTime != null) {
                dateComponent = matchTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            } else if (eventTimestamp != null) {
                dateComponent = eventTimestamp.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                dateComponent = "unknown_date"; // Fallback
            }
        }
        return teamHome.toLowerCase() + "|" + teamAway.toLowerCase() + "|" + dateComponent;
    }

    // Determina el estado basado principalmente en el formato de dateTimeString
    private MatchStatus determineStatusFromDateTimeString(String dateTimeString, ZonedDateTime eventTimestampForFallback) {
        if (dateTimeString != null && LIVE_TIME_PATTERN.matcher(dateTimeString).matches()) {
            return MatchStatus.LIVE;
        }

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime matchStartTime = DateTimeUtil.parseCustomDateTime(dateTimeString);

        if (matchStartTime == null && eventTimestampForFallback != null) {
            logger.debug("Usando eventTimestamp ({}) como fallback para determinar estado (dateTimeString: '{}' no es formato LIVE ni fecha parseable)", eventTimestampForFallback, dateTimeString);
            // Si usamos eventTimestamp como fallback, es probable que sea histórico si ya pasó
            return eventTimestampForFallback.isBefore(now) ? MatchStatus.HISTORICAL : MatchStatus.UPCOMING;
        } else if (matchStartTime == null) {
            logger.warn("No se pudo determinar la hora del partido (dateTimeString: '{}', eventTimestamp: {}), clasificando como HISTORICAL.", dateTimeString, eventTimestampForFallback);
            return MatchStatus.HISTORICAL;
        }

        // Si se pudo parsear dateTimeString a una fecha/hora concreta
        if (now.isBefore(matchStartTime)) {
            return MatchStatus.UPCOMING;
        } else {
            // Si la hora actual es después de la hora de inicio parseada, y no es formato LIVE, es HISTORICAL
            return MatchStatus.HISTORICAL;
        }
    }

    public synchronized void processRealtimeEvent(MatchEvent eventDataFromMQ) {
        MatchStatus determinedStatus = determineStatusFromDateTimeString(eventDataFromMQ.getDateTimeString(), eventDataFromMQ.getTimeStamp());
        eventDataFromMQ.setMatchStatus(determinedStatus);

        if (determinedStatus == MatchStatus.LIVE) {
            eventDataFromMQ.setLiveTimeDisplay(eventDataFromMQ.getDateTimeString()); // El string ya es "XX'"
        } else {
            eventDataFromMQ.setLiveTimeDisplay(null);
        }

        String matchKey = generateMatchKey(eventDataFromMQ.getTeamHome(), eventDataFromMQ.getTeamAway(), eventDataFromMQ.getDateTimeString(), eventDataFromMQ.getTimeStamp());

        Optional<MatchEvent> existingEventOpt = matchEventsCache.stream()
                .filter(e -> generateMatchKey(e.getTeamHome(), e.getTeamAway(), e.getDateTimeString(), e.getTimeStamp()).equals(matchKey))
                .findFirst();

        if (existingEventOpt.isPresent()) {
            MatchEvent existingEvent = existingEventOpt.get();
            existingEvent.setOddsDraw(eventDataFromMQ.getOddsDraw());
            existingEvent.setOddsHome(eventDataFromMQ.getOddsHome());
            existingEvent.setOddsAway(eventDataFromMQ.getOddsAway());
            existingEvent.setMatchStatus(determinedStatus);
            existingEvent.setLiveTimeDisplay(eventDataFromMQ.getLiveTimeDisplay());
            existingEvent.setSource(eventDataFromMQ.getSource()); // Actualizar fuente si viene de MQ_LIVE
            logger.info("Partido actualizado en caché (MQ): {} vs {} - Estado: {}, Display: {}",
                    existingEvent.getTeamHome(), existingEvent.getTeamAway(), existingEvent.getMatchStatus(),
                    existingEvent.getMatchStatus() == MatchStatus.LIVE ? existingEvent.getLiveTimeDisplay() : existingEvent.getDateTimeString());
        } else {
            matchEventsCache.add(eventDataFromMQ);
            logger.info("Nuevo partido añadido a caché (MQ): {} vs {} - Estado: {}, Display: {}",
                    eventDataFromMQ.getTeamHome(), eventDataFromMQ.getTeamAway(), eventDataFromMQ.getMatchStatus(),
                    eventDataFromMQ.getMatchStatus() == MatchStatus.LIVE ? eventDataFromMQ.getLiveTimeDisplay() : eventDataFromMQ.getDateTimeString());
        }
    }

    private void refreshMatchStatuses() {
        synchronized (this.matchEventsCache) {
            this.matchEventsCache.forEach(event -> {
                MatchStatus newStatus = determineStatusFromDateTimeString(event.getDateTimeString(), event.getTimeStamp());
                if (event.getMatchStatus() != newStatus) {
                    logger.debug("Actualizando estado de {} vs {} de {} a {}", event.getTeamHome(), event.getTeamAway(), event.getMatchStatus(), newStatus);
                    event.setMatchStatus(newStatus);
                }
                if (newStatus == MatchStatus.LIVE) {
                    event.setLiveTimeDisplay(event.getDateTimeString()); // Asegurar que el display es el correcto
                } else {
                    event.setLiveTimeDisplay(null); // Limpiar si ya no está en vivo
                }
            });
        }
    }

    public List<MatchEvent> getLiveMatches() {
        refreshMatchStatuses(); // Actualizar estados antes de filtrar
        synchronized (this.matchEventsCache) {
            List<MatchEvent> liveMatches = matchEventsCache.stream()
                    .filter(event -> event.getMatchStatus() == MatchStatus.LIVE)
                    .collect(Collectors.toList());
            logger.info("getLiveMatches() - Encontrados: {} partidos EN VIVO de {} en caché", liveMatches.size(), matchEventsCache.size());
            return liveMatches;
        }
    }

    public List<MatchEvent> getUpcomingMatches() {
        refreshMatchStatuses();
        synchronized (this.matchEventsCache) {
            List<MatchEvent> upcomingMatches = matchEventsCache.stream()
                    .filter(event -> event.getMatchStatus() == MatchStatus.UPCOMING)
                    .sorted((e1, e2) -> {
                        ZonedDateTime dt1 = DateTimeUtil.parseCustomDateTime(e1.getDateTimeString());
                        ZonedDateTime dt2 = DateTimeUtil.parseCustomDateTime(e2.getDateTimeString());
                        if (dt1 == null && dt2 == null) return 0;
                        if (dt1 == null) return 1;
                        if (dt2 == null) return -1;
                        return dt1.compareTo(dt2);
                    })
                    .collect(Collectors.toList());
            logger.info("getUpcomingMatches() - Encontrados: {} partidos PRÓXIMOS de {} en caché", upcomingMatches.size(), matchEventsCache.size());
            return upcomingMatches;
        }
    }

    public List<MatchEvent> getHistoricalMatches() {
        refreshMatchStatuses();
        synchronized (this.matchEventsCache) {
            List<MatchEvent> historicalMatches = matchEventsCache.stream()
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
            logger.info("getHistoricalMatches() - Encontrados: {} partidos HISTÓRICOS de {} en caché", historicalMatches.size(), matchEventsCache.size());
            return historicalMatches;
        }
    }
    public List<MatchEvent> getAllMatchEvents() {
        refreshMatchStatuses();
        synchronized (this.matchEventsCache) {
            return new ArrayList<>(matchEventsCache);
        }
    }

    public Optional<MatchEvent> getMatchEventById(String id) {
        if (id == null) return Optional.empty();
        refreshMatchStatuses(); // Asegurar que el estado está actualizado
        synchronized (this.matchEventsCache) {
            return matchEventsCache.stream()
                    .filter(event -> id.equals(event.getId()))
                    .findFirst();
        }
    }
}
