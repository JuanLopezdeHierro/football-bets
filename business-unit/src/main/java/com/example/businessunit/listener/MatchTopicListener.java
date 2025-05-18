package com.example.businessunit.listener;

import com.example.businessunit.model.MatchEvent;
import com.example.businessunit.model.MatchEventDTO;
// import com.example.businessunit.model.MatchStatus; // No es necesario determinar el estado aquí si MatchDataService lo hace
import com.example.businessunit.service.MatchDataService;
import com.example.businessunit.service.MatchSseService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MatchTopicListener {
    private static final Logger logger = LoggerFactory.getLogger(MatchTopicListener.class);

    private final ObjectMapper objectMapper;
    private final MatchDataService matchDataService;
    private final MatchSseService sseService;

    @Autowired
    public MatchTopicListener(ObjectMapper objectMapper,
                              MatchDataService matchDataService,
                              MatchSseService sseService) {
        this.objectMapper = objectMapper;
        this.matchDataService = matchDataService;
        this.sseService = sseService;
    }

    @JmsListener(destination = "Match_Topic", containerFactory = "topicListenerFactory")
    public void receiveMatchUpdate(Message message) {
        if (!(message instanceof TextMessage)) {
            logger.warn("Received non-TextMessage on Match_Topic: {}", message.getClass().getName());
            return;
        }
        String json = ""; // Para logging en caso de error
        try {
            json = ((TextMessage) message).getText();
            logger.debug("Received JSON on Match_Topic: {}", json);

            List<MatchEventDTO> dtos;
            try {
                dtos = objectMapper.readValue(json, new TypeReference<List<MatchEventDTO>>() {});
            } catch (Exception e) {
                logger.debug("Failed to parse as List<MatchEventDTO>, attempting to parse as single MatchEventDTO. Error: {}", e.getMessage());
                MatchEventDTO singleDto = objectMapper.readValue(json, MatchEventDTO.class);
                dtos = List.of(singleDto);
            }

            boolean anyChangeProcessed = false;
            for (MatchEventDTO dto : dtos) {
                // Convertir DTO a MatchEvent. MatchDataService se encargará de determinar el estado y logos.
                // El constructor de MatchEvent ahora toma URLs de logo.
                // Es importante que el DTO, o la lógica aquí, pueda proveerlas o generarlas.
                String homeLogoUrl = generateLogoUrlFromName(dto.getTeamHome()); // O null si se maneja después
                String awayLogoUrl = generateLogoUrlFromName(dto.getTeamAway()); // O null si se maneja después

                MatchEvent event = new MatchEvent(
                        dto.getTimeStamp(),
                        dto.getDateTime(),
                        dto.getOddsDraw() != null ? dto.getOddsDraw() : 0.0,
                        dto.getTeamAway(),
                        dto.getOddsAway() != null ? dto.getOddsAway() : 0.0,
                        dto.getTeamHome(),
                        "MQ_LIVE:" + (dto.getSource() != null ? dto.getSource() : "UnknownMQSource"), // Fuente más específica
                        dto.getOddsHome() != null ? dto.getOddsHome() : 0.0,
                        null, // Dejar que MatchDataService.processRealtimeEvent determine el estado inicial
                        homeLogoUrl,
                        awayLogoUrl
                );
                matchDataService.processRealtimeEvent(event); // processRealtimeEvent debería devolver si hubo cambio o no
                anyChangeProcessed = true; // Asumimos que procesar implica un potencial cambio. Para ser más precisos, processRealtimeEvent podría devolver un booleano.
            }

            if (anyChangeProcessed) { // Solo enviar SSE si algo se procesó
                List<MatchEvent> snapshot = matchDataService.getAllMatchEvents();
                sseService.sendUpdate(snapshot);
                logger.debug("SSE update sent after processing MQ message.");
            }

        } catch (JMSException jmse) {
            logger.error("Error reading JMS text from Match_Topic", jmse);
        } catch (Exception ex) {
            logger.error("Error processing message from Match_Topic. JSON: '{}'", json, ex);
        }
    }

    // Método duplicado de MatchDataService. Considera moverlo a una clase Util si se usa en múltiples sitios.
    private String generateLogoUrlFromName(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) {
            return "/images/logos/default_logo.png"; // O null, o un logo por defecto real
        }
        return "/images/logos/" + teamName.replace(" ", "_") + ".png";
    }
}