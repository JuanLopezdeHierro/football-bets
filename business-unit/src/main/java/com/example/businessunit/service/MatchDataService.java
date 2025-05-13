package com.example.businessunit.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.businessunit.model.MatchEvent;
import com.example.businessunit.model.MatchEventDTO;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MatchDataService {

    private static final Logger logger = LoggerFactory.getLogger(MatchDataService.class);
    private final ObjectMapper objectMapper;
    private List<MatchEvent> matchEventsCache = new ArrayList<>();

    private final Path EVENTS_DIRECTORY_PATH = Paths.get("datalake/eventstore/Match_Topic/default");

    @Autowired
    public MatchDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeMatchEvents() {
        logger.info("Inicializando carga de eventos de partidos...");
        List<MatchEvent> allMatches = new ArrayList<>();
        List<Path> eventFiles = new ArrayList<>();

        if (!Files.isDirectory(EVENTS_DIRECTORY_PATH)) {
            logger.warn("El directorio de eventos no existe o no es un directorio: {}", EVENTS_DIRECTORY_PATH.toAbsolutePath());
            this.matchEventsCache = Collections.unmodifiableList(allMatches);
            // Log después de inicializar la caché, incluso si está vacía y no se encontraron archivos.
            logger.info("✅ {} eventos de partidos cargados. No se encontraron archivos en el directorio especificado.", matchEventsCache.size());
            return;
        }

        try (Stream<Path> paths = Files.walk(EVENTS_DIRECTORY_PATH)) {
            eventFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".events"))
                    .collect(Collectors.toList());

            if (eventFiles.isEmpty()) {
                logger.info("No se encontraron archivos .events en el directorio: {}", EVENTS_DIRECTORY_PATH);
            }

            for (Path filePath : eventFiles) {
                logger.debug("Procesando archivo: {}", filePath);
                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        try {
                            // ASUMIENDO QUE CADA LÍNEA ES UN SOLO MatchEventDTO AHORA
                            // O si cada línea es una LISTA, vuelve al TypeReference y itera
                            List<MatchEventDTO> dtos = objectMapper.readValue(line, new TypeReference<List<MatchEventDTO>>() {});
                            for (MatchEventDTO dto : dtos) { // Si es una lista por línea
                                allMatches.add(new MatchEvent(
                                        dto.dateTime,  // AÑADIDO
                                        dto.oddsDraw,
                                        dto.teamAway,
                                        dto.oddsAway,
                                        dto.teamHome,
                                        dto.source,
                                        dto.oddsHome
                                ));
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

        this.matchEventsCache = Collections.unmodifiableList(allMatches);
        // Ahora eventFiles es accesible aquí
        logger.info("✅ {} eventos de partidos cargados desde {}.", matchEventsCache.size(), eventFiles.size() > 0 ? eventFiles.size() + " archivos" : "0 archivos");
    }

    public List<MatchEvent> getAllMatchEvents() {
        return new ArrayList<>(matchEventsCache);
    }

    public Optional<MatchEvent> getMatchEventById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return matchEventsCache.stream()
                .filter(event -> id.equals(event.getId()))
                .findFirst();
    }
}