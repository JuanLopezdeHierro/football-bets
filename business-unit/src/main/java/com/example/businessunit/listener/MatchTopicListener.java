package com.example.businessunit.listener;

import com.example.businessunit.model.MatchEvent;
import com.example.businessunit.model.MatchEventDTO; // Asumiendo que el mensaje contiene este DTO
import com.example.businessunit.model.MatchStatus;
import com.example.businessunit.service.MatchDataService; // O un servicio dedicado a procesar eventos en vivo
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    public MatchTopicListener(ObjectMapper objectMapper, MatchDataService matchDataService) {
        this.objectMapper = objectMapper;
        this.matchDataService = matchDataService;
    }

    @JmsListener(destination = "Match_Topic", containerFactory = "topicListenerFactory")
    public void receiveMatchUpdate(Message message) {
        if (message instanceof TextMessage) {
            try {
                String jsonMessage = ((TextMessage) message).getText();
                logger.debug("Mensaje JSON recibido de Match_Topic: {}", jsonMessage);

                // Intentar deserializar como una lista primero
                try {
                    List<MatchEventDTO> eventDTOs = objectMapper.readValue(jsonMessage, new TypeReference<List<MatchEventDTO>>() {});
                    for (MatchEventDTO dto : eventDTOs) {
                        // Crear MatchEvent pasando un MatchStatus inicial (será refinado en el servicio)
                        MatchEvent event = new MatchEvent(
                                dto.timeStamp,
                                dto.dateTime, // Este campo del DTO se mapea a dateTimeString en MatchEvent
                                dto.oddsDraw,
                                dto.teamAway,
                                dto.oddsAway,
                                dto.teamHome,
                                dto.source,
                                dto.oddsHome,
                                MatchStatus.UPCOMING // Pasar un estado inicial/temporal
                        );
                        matchDataService.processRealtimeEvent(event);
                        logger.info("Evento en tiempo real (de lista en JSON) procesado: {} vs {}", event.getTeamHome(), event.getTeamAway());
                    }
                } catch (Exception e_list) {
                    // Si falla como lista, intentar como objeto único
                    logger.warn("No se pudo deserializar mensaje de ActiveMQ como lista, intentando como objeto único. Error de lista: {}", e_list.getMessage());
                    try {
                        MatchEventDTO eventDTO = objectMapper.readValue(jsonMessage, MatchEventDTO.class);
                        // Crear MatchEvent pasando un MatchStatus inicial
                        MatchEvent event = new MatchEvent(
                                eventDTO.timeStamp,
                                eventDTO.dateTime, // Este campo del DTO se mapea a dateTimeString en MatchEvent
                                eventDTO.oddsDraw,
                                eventDTO.teamAway,
                                eventDTO.oddsAway,
                                eventDTO.teamHome,
                                eventDTO.source,
                                eventDTO.oddsHome,
                                MatchStatus.UPCOMING // Pasar un estado inicial/temporal
                        );
                        matchDataService.processRealtimeEvent(event);
                        logger.info("Evento en tiempo real (objeto único JSON) procesado: {} vs {}", event.getTeamHome(), event.getTeamAway());
                    } catch (Exception e_single) {
                        logger.error("Error deserializando mensaje de Match_Topic como lista o como objeto único. Error de objeto único: {}", e_single.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.error("Error general procesando mensaje de Match_Topic", e);
            }
        } else {
            logger.warn("Mensaje recibido de Match_Topic de tipo no esperado: {}", message.getClass().getName());
        }
    }
}
