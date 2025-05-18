package com.example.businessunit.service;

import com.example.businessunit.model.MatchApiDataDTO;
import com.example.businessunit.model.MatchEvent;
import com.example.businessunit.model.MatchEventDTO;
import com.example.businessunit.model.MatchStatus;
import com.example.businessunit.util.DateTimeUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import lombok.Getter; // Para OddsHistoryPoint
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator; // Para ordenar
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MatchDataService {

    private static final Logger logger = LoggerFactory.getLogger(MatchDataService.class);
    private final ObjectMapper objectMapper;
    private final List<MatchEvent> matchEventsCache = new ArrayList<>();
    private final Path ODDS_STATUS_EVENTS_DIRECTORY_PATH = Paths.get("datalake/eventstore/Match_Topic/default");
    private final Path MATCH_API_EVENTS_DIRECTORY_PATH = Paths.get("datalake/eventstore/MatchApi_Topic/default");
    private final Path DATAMART_DIRECTORY_PATH = Paths.get("output_datamart/default");

    private static final Pattern LIVE_TIME_PATTERN = Pattern.compile("^(\\d{1,3}['\u2032+]?\\d*|HT|FT|Descanso)$", Pattern.CASE_INSENSITIVE);
    private final DateTimeFormatter DAILY_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");


    private FileTime lastOddsStatusFileReadTimestamp = null;
    private Path currentDailyOddsStatusFilePath = null;
    private FileTime lastMatchApiFileReadTimestamp = null;
    private Path currentDailyMatchApiFilePath = null;
    private Path currentDailyDataMartFilePath = null;

    private final MatchSseService sseService;

    @Autowired
    public MatchDataService(ObjectMapper objectMapper, MatchSseService sseService) {
        this.objectMapper = objectMapper;
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.sseService = sseService;
    }

    // Clase interna para el historial de cuotas
    @Getter
    public static class OddsHistoryPoint {
        private final String timeLabel; // "19:00", "23′", "HT" - para el eje X del gráfico
        private final ZonedDateTime timestampRecord; // El timeStamp del DTO, para ordenación precisa
        private final double oddsHome;
        private final double oddsAway;
        private final double oddsDraw;

        public OddsHistoryPoint(String timeLabel, ZonedDateTime timestampRecord, double oddsHome, double oddsAway, double oddsDraw) {
            this.timeLabel = timeLabel;
            this.timestampRecord = timestampRecord;
            this.oddsHome = oddsHome;
            this.oddsAway = oddsAway;
            this.oddsDraw = oddsDraw;
        }
    }

    @PostConstruct
    public void initialLoadAndSetup() {
        try {
            Files.createDirectories(DATAMART_DIRECTORY_PATH);
        } catch (IOException e) {
            logger.error("Failed to create datamart directory: {}", DATAMART_DIRECTORY_PATH, e);
        }
        updateCurrentDailyEventFilePaths();
        logger.info("Initial data load, merge, and datamart generation...");
        buildAndBroadcastAndStoreDataMart();
    }

    private void updateCurrentDailyEventFilePaths() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String dailyFileNameBase = today.format(DAILY_FILE_FORMATTER);

        Path oldOddsPath = this.currentDailyOddsStatusFilePath;
        Path oldApiPath = this.currentDailyMatchApiFilePath;
        Path oldDataMartPath = this.currentDailyDataMartFilePath;

        this.currentDailyOddsStatusFilePath = ODDS_STATUS_EVENTS_DIRECTORY_PATH.resolve(dailyFileNameBase + ".events");
        this.currentDailyMatchApiFilePath = MATCH_API_EVENTS_DIRECTORY_PATH.resolve(dailyFileNameBase + ".events");
        this.currentDailyDataMartFilePath = DATAMART_DIRECTORY_PATH.resolve(dailyFileNameBase + ".datamart.json");

        if (!Objects.equals(oldOddsPath, this.currentDailyOddsStatusFilePath)) {
            this.lastOddsStatusFileReadTimestamp = null;
        }
        if (!Objects.equals(oldApiPath, this.currentDailyMatchApiFilePath)) {
            this.lastMatchApiFileReadTimestamp = null;
        }
    }

    private String normalizeTeamNameForKey(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) {
            return "unknownteam" + UUID.randomUUID().toString().substring(0, 4);
        }
        String normalized = teamName.toLowerCase();
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");

        Map<String, String> specificReplacements = new HashMap<>();
        specificReplacements.put("fc barcelona", "barcelona");
        specificReplacements.put("atletico de madrid", "atleticomadrid");
        specificReplacements.put("atletico madrid", "atleticomadrid");
        specificReplacements.put("celta de vigo", "celtavigo");
        specificReplacements.put("rc celta de vigo", "celtavigo");
        specificReplacements.put("celta vigo", "celtavigo");
        specificReplacements.put("real sociedad de futbol", "realsociedad");
        specificReplacements.put("r. sociedad", "realsociedad");
        specificReplacements.put("union deportiva las palmas", "laspalmas");
        specificReplacements.put("ud las palmas", "laspalmas");
        specificReplacements.put("club atletico osasuna", "osasuna");
        specificReplacements.put("c.a. osasuna", "osasuna");
        specificReplacements.put("athletic club de bilbao", "athleticbilbao");
        specificReplacements.put("athletic de bilbao", "athleticbilbao");
        specificReplacements.put("athletic club", "athleticbilbao");
        specificReplacements.put("real valladolid cf", "valladolid");
        specificReplacements.put("real valladolid", "valladolid");
        specificReplacements.put("club deportivo alaves", "alaves");
        specificReplacements.put("deportivo alaves", "alaves");
        specificReplacements.put("rcd espanyol de barcelona", "espanyol");
        specificReplacements.put("rcd espanyol", "espanyol");
        specificReplacements.put("valencia cf", "valencia");
        specificReplacements.put("sevilla fc", "sevilla");
        specificReplacements.put("getafe cf", "getafe");
        specificReplacements.put("rcd mallorca", "mallorca");
        specificReplacements.put("girona fc", "girona");
        specificReplacements.put("rayo vallecano de madrid", "rayovallecano");
        specificReplacements.put("rayo vallecano", "rayovallecano");
        specificReplacements.put("villarreal cf", "villarreal");
        specificReplacements.put("real betis balompie", "realbetis");
        specificReplacements.put("real betis", "realbetis");
        specificReplacements.put("betis", "realbetis");
        specificReplacements.put("club deportivo leganes", "leganes");
        specificReplacements.put("cd leganes", "leganes");

        String bestMatch = normalized;
        int longestMatchLength = 0;
        for (Map.Entry<String, String> entry : specificReplacements.entrySet()) {
            if (normalized.contains(entry.getKey()) && entry.getKey().length() > longestMatchLength) {
                bestMatch = entry.getValue();
                longestMatchLength = entry.getKey().length();
            }
        }
        if (longestMatchLength > 0) normalized = bestMatch;

        normalized = normalized.replaceAll("\\s*fc\\s*|\\s*cf\\s*|\\s*ud\\s*|\\s*rcd\\s*|\\s*cd\\s*|\\s*sad\\s*|\\s*de\\s*|\\s*club\\s*|\\s*futbol\\s*", "");
        normalized = normalized.replaceAll("[^a-z0-9]", "");

        if (normalized.trim().isEmpty()) return "unknownteamnorm" + UUID.randomUUID().toString().substring(0, 4);
        return normalized;
    }

    @Scheduled(fixedDelayString = "PT5S")
    public void scheduledDataMartUpdate() {
        updateCurrentDailyEventFilePaths();
        boolean oddsFileChanged = hasFileChanged(this.currentDailyOddsStatusFilePath, this.lastOddsStatusFileReadTimestamp);
        boolean apiFileChanged = hasFileChanged(this.currentDailyMatchApiFilePath, this.lastMatchApiFileReadTimestamp);
        boolean statusesRefreshedAndChanged = refreshMatchStatusesGlobally();
        if (oddsFileChanged || apiFileChanged || statusesRefreshedAndChanged) {
            if (oddsFileChanged && this.currentDailyOddsStatusFilePath != null && Files.exists(this.currentDailyOddsStatusFilePath)) {
                try { this.lastOddsStatusFileReadTimestamp = Files.getLastModifiedTime(this.currentDailyOddsStatusFilePath); } catch (IOException e) {logger.error("Error updating odds file timestamp for {}: {}",this.currentDailyOddsStatusFilePath, e.getMessage());}
            }
            if (apiFileChanged && this.currentDailyMatchApiFilePath != null && Files.exists(this.currentDailyMatchApiFilePath)) {
                try { this.lastMatchApiFileReadTimestamp = Files.getLastModifiedTime(this.currentDailyMatchApiFilePath); } catch (IOException e) {logger.error("Error updating api file timestamp for {}: {}", this.currentDailyMatchApiFilePath, e.getMessage());}
            }
            buildAndBroadcastAndStoreDataMart();
        }
    }

    private boolean hasFileChanged(Path filePath, FileTime lastReadTime) {
        if (filePath == null || !Files.exists(filePath)) return false;
        try {
            FileTime currentModTime = Files.getLastModifiedTime(filePath);
            return lastReadTime == null || currentModTime.compareTo(lastReadTime) > 0;
        } catch (IOException e) { return false; }
    }

    private void buildAndBroadcastAndStoreDataMart() {
        List<MatchEventDTO> oddsStatusDTOs = readLastJsonArrayFromFile(this.currentDailyOddsStatusFilePath, new TypeReference<List<MatchEventDTO>>() {});
        List<MatchApiDataDTO> matchApiDTOs = readLastJsonArrayFromFile(this.currentDailyMatchApiFilePath, new TypeReference<List<MatchApiDataDTO>>() {});

        Map<String, MatchApiDataDTO> apiDataMapByNormalizedHomeTeam = matchApiDTOs.stream()
                .map(dto -> {
                    String normalizedKey = normalizeTeamNameForKey(dto.getHomeTeam());
                    if (normalizedKey != null && !normalizedKey.startsWith("unknown")) return new NormalizedApiDTO(normalizedKey, dto);
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap( NormalizedApiDTO::getNormalizedKey, NormalizedApiDTO::getDto, (apiDtoValue1, apiDtoValue2) -> apiDtoValue2 ));

        List<MatchEvent> newCache = new ArrayList<>();
        if (oddsStatusDTOs.isEmpty() && !matchApiDTOs.isEmpty()) {
            for (MatchApiDataDTO apiDto : matchApiDTOs) {
                String normalizedHomeKey = normalizeTeamNameForKey(apiDto.getHomeTeam());
                if (normalizedHomeKey == null || normalizedHomeKey.startsWith("unknown")) continue;
                String homeLogo = generateLogoUrlFromName(apiDto.getHomeTeam());
                String awayLogo = generateLogoUrlFromName(apiDto.getAwayTeam());
                ZonedDateTime ts = DateTimeUtil.parseCustomDateTime(apiDto.getDateTime());
                if (ts == null) ts = ZonedDateTime.now(ZoneId.systemDefault());
                MatchEvent event = new MatchEvent(ts, apiDto.getDateTime(),0, apiDto.getAwayTeam(),0, apiDto.getHomeTeam(), "Datalake_API_Only", 0, MatchStatus.SCHEDULED, homeLogo, awayLogo);
                event.setStadium(apiDto.getStadium());
                event.setReferee(apiDto.getReferee());
                event.setOddsSource("Betfair");
                newCache.add(event);
            }
        } else {
            for (MatchEventDTO oddsDto : oddsStatusDTOs) {
                String normalizedHomeKey = normalizeTeamNameForKey(oddsDto.getTeamHome());
                if (normalizedHomeKey == null || normalizedHomeKey.startsWith("unknown")) continue;
                MatchApiDataDTO apiData = apiDataMapByNormalizedHomeTeam.get(normalizedHomeKey);
                ZonedDateTime ts = oddsDto.getTimeStamp() != null ? oddsDto.getTimeStamp() : ZonedDateTime.now(ZoneId.systemDefault());
                MatchStatus status = determineStatusFromDateTimeString(oddsDto.getDateTime(), ts);
                String homeLogo = generateLogoUrlFromName(oddsDto.getTeamHome());
                String awayLogo = generateLogoUrlFromName(oddsDto.getTeamAway());
                MatchEvent event = new MatchEvent(ts, oddsDto.getDateTime(),
                        oddsDto.getOddsDraw() != null ? oddsDto.getOddsDraw() : 0.0, oddsDto.getTeamAway(),
                        oddsDto.getOddsAway() != null ? oddsDto.getOddsAway() : 0.0, oddsDto.getTeamHome(),
                        "Datalake_Consolidated", oddsDto.getOddsHome() != null ? oddsDto.getOddsHome() : 0.0,
                        status, homeLogo, awayLogo);
                if (status == MatchStatus.LIVE) event.setLiveTimeDisplay(oddsDto.getDateTime());
                else event.setLiveTimeDisplay(null);
                if (apiData != null) {
                    event.setStadium(apiData.getStadium());
                    event.setReferee(apiData.getReferee());
                }
                event.setOddsSource("Betfair");
                newCache.add(event);
            }
        }

        boolean cacheEffectivelyChanged;
        synchronized (this.matchEventsCache) {
            if (this.matchEventsCache.size() != newCache.size() || !areMatchEventListsEffectivelyEqual(this.matchEventsCache, newCache)) {
                this.matchEventsCache.clear();
                this.matchEventsCache.addAll(newCache);
                cacheEffectivelyChanged = true;
            } else {
                cacheEffectivelyChanged = false;
            }
        }

        if (cacheEffectivelyChanged) {
            writeDatamartToFile(this.matchEventsCache);
            sseService.sendUpdate(new ArrayList<>(this.matchEventsCache));
        }
    }

    private static class NormalizedApiDTO {
        private final String normalizedKey;
        private final MatchApiDataDTO dto;
        public NormalizedApiDTO(String normalizedKey, MatchApiDataDTO dto) { this.normalizedKey = normalizedKey; this.dto = dto; }
        public String getNormalizedKey() { return normalizedKey; }
        public MatchApiDataDTO getDto() { return dto; }
    }

    private void writeDatamartToFile(List<MatchEvent> datamartEvents) {
        if (this.currentDailyDataMartFilePath == null) return;
        try {
            String jsonDatamart = objectMapper.writeValueAsString(datamartEvents);
            Files.writeString(this.currentDailyDataMartFilePath, jsonDatamart, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            logger.error("Error writing DataMart to file {}: {}", this.currentDailyDataMartFilePath, e.getMessage(), e);
        }
    }

    private boolean areMatchEventListsEffectivelyEqual(List<MatchEvent> list1, List<MatchEvent> list2) {
        if (list1.size() != list2.size()) return false;
        Map<String, MatchEvent> map1 = list1.stream().filter(e -> e.getId() != null).collect(Collectors.toMap(MatchEvent::getId, Function.identity(), (e1, e2) -> e1));
        for (MatchEvent event2 : list2) {
            if (event2.getId() == null) return false;
            MatchEvent event1 = map1.get(event2.getId());
            if (event1 == null || !isMatchEventEffectivelyEqual(event1, event2)) return false;
        }
        return true;
    }

    private boolean isMatchEventEffectivelyEqual(MatchEvent e1, MatchEvent e2) {
        return Objects.equals(e1.getDateTimeString(), e2.getDateTimeString()) &&
                Double.compare(e1.getOddsDraw(), e2.getOddsDraw()) == 0 &&
                Double.compare(e1.getOddsAway(), e2.getOddsAway()) == 0 &&
                Double.compare(e1.getOddsHome(), e2.getOddsHome()) == 0 &&
                e1.getMatchStatus() == e2.getMatchStatus() &&
                Objects.equals(e1.getLiveTimeDisplay(), e2.getLiveTimeDisplay()) &&
                Objects.equals(e1.getStadium(), e2.getStadium()) &&
                Objects.equals(e1.getReferee(), e2.getReferee()) &&
                Objects.equals(e1.getOddsSource(), e2.getOddsSource()) &&
                Objects.equals(e1.getTeamHome(), e2.getTeamHome()) &&
                Objects.equals(e1.getTeamAway(), e2.getTeamAway());
    }

    private <T> List<T> readLastJsonArrayFromFile(Path filePath, TypeReference<List<T>> typeReference) {
        if (filePath == null || !Files.exists(filePath)) return List.of();
        try {
            List<String> lines = Files.readAllLines(filePath);
            if (!lines.isEmpty()) {
                String lastLineWithData = null;
                for (int i = lines.size() - 1; i >= 0; i--) {
                    if (!lines.get(i).trim().isEmpty()) { lastLineWithData = lines.get(i); break; }
                }
                if (lastLineWithData != null) return objectMapper.readValue(lastLineWithData, typeReference);
            }
        } catch (IOException e) {
            logger.error("Error reading or parsing JSON array from file {}: {}", filePath, e.getMessage());
        }
        return List.of();
    }

    public List<OddsHistoryPoint> getOddsHistoryForMatch(String targetTeamHome, String targetTeamAway, String originalMatchDateTimeString) {
        List<OddsHistoryPoint> history = new ArrayList<>();
        if (this.currentDailyOddsStatusFilePath == null || !Files.exists(this.currentDailyOddsStatusFilePath)) {
            logger.warn("Daily odds/status event file not found for history: {}", this.currentDailyOddsStatusFilePath);
            return history;
        }

        ZonedDateTime matchStartTimeForDate = DateTimeUtil.parseCustomDateTime(originalMatchDateTimeString);
        LocalDate matchDate = (matchStartTimeForDate != null) ? matchStartTimeForDate.toLocalDate() : LocalDate.now(ZoneId.systemDefault());

        logger.info("Fetching odds history for Home: {}, Away: {}, on Date: {}", targetTeamHome, targetTeamAway, matchDate);

        try (Stream<String> linesStream = Files.lines(this.currentDailyOddsStatusFilePath, StandardCharsets.UTF_8)) {
            linesStream.filter(line -> !line.trim().isEmpty()).forEach(line -> {
                try {
                    List<MatchEventDTO> dtosInLine = objectMapper.readValue(line, new TypeReference<List<MatchEventDTO>>() {});
                    for (MatchEventDTO dto : dtosInLine) {
                        if (targetTeamHome.equalsIgnoreCase(dto.getTeamHome()) &&
                                (targetTeamAway == null || targetTeamAway.equalsIgnoreCase(dto.getTeamAway()))) { // Hacer awayTeam opcional si no siempre está

                            ZonedDateTime dtoEventTime = dto.getTimeStamp() != null ? dto.getTimeStamp() : ZonedDateTime.now(ZoneId.systemDefault());
                            // Comprobar si el DTO es del mismo día del partido o si es un tiempo en vivo (que asumimos es del día)
                            boolean isSameDayOrLive = false;
                            ZonedDateTime dtoMatchTimeForDate = DateTimeUtil.parseCustomDateTime(dto.getDateTime());
                            if (LIVE_TIME_PATTERN.matcher(dto.getDateTime()).matches()) {
                                isSameDayOrLive = true; // Tiempo en vivo es del partido actual
                            } else if (dtoMatchTimeForDate != null && dtoMatchTimeForDate.toLocalDate().equals(matchDate)) {
                                isSameDayOrLive = true; // Hora de inicio programada del mismo día
                            }

                            if (isSameDayOrLive) {
                                history.add(new OddsHistoryPoint(
                                        dto.getDateTime(),
                                        dtoEventTime,
                                        dto.getOddsHome() != null ? dto.getOddsHome() : 0,
                                        dto.getOddsAway() != null ? dto.getOddsAway() : 0,
                                        dto.getOddsDraw() != null ? dto.getOddsDraw() : 0
                                ));
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error parsing JSON line for odds history: '{}'. Error: {}", line, e.getMessage());
                }
            });
        } catch (IOException e) {
            logger.error("Error reading daily odds/status event file for history: {}", this.currentDailyOddsStatusFilePath, e);
        }
        history.sort(Comparator.comparing(OddsHistoryPoint::getTimestampRecord));
        return history;
    }


    private String generateLogoUrlFromName(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) return "/images/logos/default_logo.png";
        return "/images/logos/" + teamName.replace(" ", "_") + ".png";
    }

    private MatchStatus determineStatusFromDateTimeString(String dateTimeString, ZonedDateTime eventTimestampForFallback) {
        if (dateTimeString != null && LIVE_TIME_PATTERN.matcher(dateTimeString).matches()) return MatchStatus.LIVE;
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime matchStartTime = DateTimeUtil.parseCustomDateTime(dateTimeString);
        if (matchStartTime == null) {
            if (eventTimestampForFallback != null) return eventTimestampForFallback.isBefore(now) ? MatchStatus.HISTORICAL : MatchStatus.SCHEDULED;
            return MatchStatus.SCHEDULED;
        }
        if (now.isBefore(matchStartTime)) return MatchStatus.UPCOMING;
        return MatchStatus.HISTORICAL;
    }

    public synchronized void processRealtimeEvent(MatchEvent eventDataFromMQ) {
        MatchStatus determinedStatus = determineStatusFromDateTimeString(eventDataFromMQ.getDateTimeString(), eventDataFromMQ.getTimeStamp());
        eventDataFromMQ.setMatchStatus(determinedStatus);
        if (determinedStatus == MatchStatus.LIVE) eventDataFromMQ.setLiveTimeDisplay(eventDataFromMQ.getDateTimeString());
        else eventDataFromMQ.setLiveTimeDisplay(null);

        if (eventDataFromMQ.getHomeTeamLogoUrl() == null && eventDataFromMQ.getTeamHome() != null) eventDataFromMQ.setHomeTeamLogoUrl(generateLogoUrlFromName(eventDataFromMQ.getTeamHome()));
        if (eventDataFromMQ.getAwayTeamLogoUrl() == null && eventDataFromMQ.getTeamAway() != null) eventDataFromMQ.setAwayTeamLogoUrl(generateLogoUrlFromName(eventDataFromMQ.getTeamAway()));

        String matchKey = generateMatchKeyFromEvent(eventDataFromMQ);
        Optional<MatchEvent> cachedEventOpt = matchEventsCache.stream().filter(e -> matchKey.equals(generateMatchKeyFromEvent(e))).findFirst();
        if (cachedEventOpt.isPresent()) {
            MatchEvent cachedEvent = cachedEventOpt.get();
            if (cachedEvent.getStadium() != null) eventDataFromMQ.setStadium(cachedEvent.getStadium());
            if (cachedEvent.getReferee() != null) eventDataFromMQ.setReferee(cachedEvent.getReferee());
        }

        updateOrAddByMatchKey(matchKey, eventDataFromMQ, true);
    }

    private String generateMatchKeyFromEvent(MatchEvent event) {
        return generateMatchKey(event.getTeamHome(), event.getTeamAway(), event.getDateTimeString(), event.getTimeStamp());
    }

    private synchronized boolean updateOrAddByMatchKey(String matchKey, MatchEvent newData, boolean isFreshestData) {
        Optional<MatchEvent> existingEventOpt = matchEventsCache.stream()
                .filter(e -> matchKey.equals(generateMatchKeyFromEvent(e)))
                .findFirst();

        boolean changed = false;
        if (existingEventOpt.isPresent()) {
            MatchEvent existing = existingEventOpt.get();
            if (isFreshestData || newData.getTimeStamp() == null || existing.getTimeStamp() == null || newData.getTimeStamp().isAfter(existing.getTimeStamp())) {
                if (Double.compare(existing.getOddsDraw(), newData.getOddsDraw()) != 0) { existing.setOddsDraw(newData.getOddsDraw()); changed = true;}
                if (Double.compare(existing.getOddsAway(), newData.getOddsAway()) != 0) { existing.setOddsAway(newData.getOddsAway()); changed = true;}
                if (Double.compare(existing.getOddsHome(), newData.getOddsHome()) != 0) { existing.setOddsHome(newData.getOddsHome()); changed = true;}
                if (existing.getMatchStatus() != newData.getMatchStatus()) { existing.setMatchStatus(newData.getMatchStatus()); changed = true;}
                if (!Objects.equals(existing.getLiveTimeDisplay(), newData.getLiveTimeDisplay())) { existing.setLiveTimeDisplay(newData.getLiveTimeDisplay()); changed = true;}
                if (!Objects.equals(existing.getHomeTeamLogoUrl(), newData.getHomeTeamLogoUrl())) { existing.setHomeTeamLogoUrl(newData.getHomeTeamLogoUrl()); changed = true;}
                if (!Objects.equals(existing.getAwayTeamLogoUrl(), newData.getAwayTeamLogoUrl())) { existing.setAwayTeamLogoUrl(newData.getAwayTeamLogoUrl()); changed = true;}
                if (newData.getStadium() != null && !Objects.equals(existing.getStadium(), newData.getStadium())) { existing.setStadium(newData.getStadium()); changed = true;}
                if (newData.getReferee() != null && !Objects.equals(existing.getReferee(), newData.getReferee())) { existing.setReferee(newData.getReferee()); changed = true;}
                if (!Objects.equals(existing.getOddsSource(), newData.getOddsSource())) { existing.setOddsSource(newData.getOddsSource()); changed = true;}
                existing.setSource(newData.getSource());
            }
        } else {
            matchEventsCache.add(newData);
            changed = true;
        }
        if (changed) logger.info("Cache updated/added for key {} from source {}. New event status: {}", matchKey, newData.getSource(), newData.getMatchStatus());
        return changed;
    }

    private String generateMatchKey(String teamHome, String teamAway, String dateTimeString, ZonedDateTime eventTimestamp) {
        String dateComponent;
        String normalizedHome = normalizeTeamNameForKey(teamHome);
        String normalizedAway = normalizeTeamNameForKey(teamAway);

        String safeHomeTeam = (normalizedHome == null || normalizedHome.startsWith("unknown")) ? "unknownhome_" + UUID.randomUUID().toString().substring(0,4) : normalizedHome;
        String safeAwayTeam = (normalizedAway == null || normalizedAway.startsWith("unknown")) ? "unknownaway_" + UUID.randomUUID().toString().substring(0,4) : normalizedAway;

        if (dateTimeString != null && LIVE_TIME_PATTERN.matcher(dateTimeString).matches()) {
            dateComponent = (eventTimestamp != null ? eventTimestamp.toLocalDate() : LocalDate.now(ZoneId.systemDefault())).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else {
            ZonedDateTime matchTime = DateTimeUtil.parseCustomDateTime(dateTimeString);
            if (matchTime != null) {
                dateComponent = matchTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            } else if (eventTimestamp != null) {
                dateComponent = eventTimestamp.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                dateComponent = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
        }
        return safeHomeTeam + "|" + safeAwayTeam + "|" + dateComponent;
    }

    private boolean refreshMatchStatusesGlobally() {
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
                    String currentDateTimeString = event.getDateTimeString();
                    if (currentDateTimeString != null && LIVE_TIME_PATTERN.matcher(currentDateTimeString).matches()) {
                        if (!Objects.equals(currentDateTimeString, oldLiveDisplay)) {
                            event.setLiveTimeDisplay(currentDateTimeString);
                            changedOverall = true;
                        }
                    } else if (oldLiveDisplay == null) {
                        event.setLiveTimeDisplay("En Directo");
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
        if (changedOverall) logger.info("Match statuses refreshed globally, changes detected in cache.");
        return changedOverall;
    }

    public List<MatchEvent> getLiveMatches() { refreshMatchStatusesGlobally(); synchronized (this.matchEventsCache) { return matchEventsCache.stream().filter(e->e.getMatchStatus() == MatchStatus.LIVE).collect(Collectors.toList()); } }
    public List<MatchEvent> getUpcomingMatches() { refreshMatchStatusesGlobally(); synchronized (this.matchEventsCache) { return matchEventsCache.stream().filter(e->e.getMatchStatus() == MatchStatus.UPCOMING || e.getMatchStatus() == MatchStatus.SCHEDULED).sorted((e1,e2)->{ ZonedDateTime d1=DateTimeUtil.parseCustomDateTime(e1.getDateTimeString()),d2=DateTimeUtil.parseCustomDateTime(e2.getDateTimeString()); if(d1==null&&d2==null)return 0;if(d1==null)return 1;if(d2==null)return -1;return d1.compareTo(d2); }).collect(Collectors.toList()); } }
    public List<MatchEvent> getHistoricalMatches() { refreshMatchStatusesGlobally(); synchronized (this.matchEventsCache) { return matchEventsCache.stream().filter(e->e.getMatchStatus() == MatchStatus.HISTORICAL).sorted((e1,e2)->{ ZonedDateTime d1=DateTimeUtil.parseCustomDateTime(e1.getDateTimeString()),d2=DateTimeUtil.parseCustomDateTime(e2.getDateTimeString()); if(d1==null&&d2==null)return 0;if(d1==null)return 1;if(d2==null)return -1;return d2.compareTo(d1); }).collect(Collectors.toList()); } }
    public List<MatchEvent> getAllMatchEvents() { refreshMatchStatusesGlobally(); synchronized (this.matchEventsCache) { return new ArrayList<>(this.matchEventsCache); } }
    public Optional<MatchEvent> getMatchEventById(String id) { if(id==null||id.trim().isEmpty())return Optional.empty();refreshMatchStatusesGlobally();synchronized(this.matchEventsCache){return matchEventsCache.stream().filter(e->Objects.equals(id,e.getId())).findFirst();} }
}