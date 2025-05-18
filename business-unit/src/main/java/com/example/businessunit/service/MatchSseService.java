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
            emitter.complete(); // Asegurar que se complete en timeout
        });
        emitter.onError(ex -> {
            logger.error("SSE Emitter error: {}", emitter, ex);
            emitters.remove(emitter);
            // No es necesario llamar a complete() aquí ya que onCompletion se activará si la conexión se rompe.
        });

        emitters.add(emitter);
        logger.info("New SSE Emitter created. Total emitters: {}", emitters.size());

        // Enviar un evento inicial de conexión si es necesario
        try {
            emitter.send(SseEmitter.event().name("connection-established").data("SSE connection established"));
        } catch (IOException e) {
            logger.warn("Failed to send initial connection event to new emitter", e);
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void sendUpdate(Object data) {
        if (emitters.isEmpty()) {
            // logger.trace("No active SSE emitters to send update to."); // Puede ser muy verboso
            return;
        }
        logger.debug("Sending update to {} SSE emitters.", emitters.size());

        // Usar un iterador para permitir la eliminación segura durante la iteración
        // Aunque CopyOnWriteArrayList es seguro para iterar mientras se modifica,
        // eliminar explícitamente por índice o referencia después de un fallo es más claro.
        List<SseEmitter> failedEmitters = null;

        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .name("match-update") // Nombre del evento que el cliente JS escucha
                        .data(data, MediaType.APPLICATION_JSON);
                emitter.send(event);
            } catch (IOException e) {
                logger.warn("Failed to send update to an SSE emitter, scheduling for removal. Error: {}", e.getMessage());
                if (failedEmitters == null) {
                    failedEmitters = new ArrayList<>();
                }
                failedEmitters.add(emitter);
            } catch (Exception e) { // Capturar otras posibles excepciones (ej. IllegalStateException si ya está completo)
                logger.warn("Unexpected error sending update to an SSE emitter, scheduling for removal. Error: {}", e.getMessage());
                if (failedEmitters == null) {
                    failedEmitters = new ArrayList<>();
                }
                failedEmitters.add(emitter);
            }
        }

        if (failedEmitters != null) {
            emitters.removeAll(failedEmitters);
            logger.info("Removed {} failed SSE emitters. Total emitters: {}", failedEmitters.size(), emitters.size());
        }
    }
}