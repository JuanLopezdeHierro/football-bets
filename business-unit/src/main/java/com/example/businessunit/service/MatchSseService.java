package com.example.businessunit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MatchSseService {
    private static final Logger logger = LoggerFactory.getLogger(MatchSseService.class);
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitter.onCompletion(() -> {
            logger.debug("SSE Emitter completed: {}", emitter);
            emitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            logger.debug("SSE Emitter timed out: {}", emitter);
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(ex -> {
            logger.error("SSE Emitter error for emitter: {}. Error: {}", emitter, ex.getMessage());
            emitters.remove(emitter);
        });

        emitters.add(emitter);
        logger.info("New SSE Emitter created. Total emitters: {}", emitters.size());

        try {
            emitter.send(SseEmitter.event().name("connection-established").data("SSE connection established"));
        } catch (IOException e) {
            logger.warn("Failed to send initial connection event to new emitter. Removing. Error: {}", e.getMessage());
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void sendUpdate(Object data) {
        if (emitters.isEmpty()) {
            return;
        }
        logger.debug("Sending update to {} SSE emitters.", emitters.size());
        List<SseEmitter> failedEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .name("match-update")
                        .data(data, MediaType.APPLICATION_JSON);
                emitter.send(event);
            } catch (Exception e) { // Capturar Exception más genérica
                logger.warn("Failed to send update to an SSE emitter (will be removed). Emitter: {}, Error: {}", emitter, e.getMessage());
                failedEmitters.add(emitter);
            }
        }

        if (!failedEmitters.isEmpty()) {
            emitters.removeAll(failedEmitters);
            logger.info("Removed {} failed SSE emitters. Total emitters remaining: {}", failedEmitters.size(), emitters.size());
        }
    }
}