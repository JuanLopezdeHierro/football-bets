package com.example.businessunit.listener;

import com.example.businessunit.model.MatchEventDTO; // Asumiendo que el mensaje contiene este DTO
import com.example.businessunit.service.MatchDataService; // O un servicio dedicado a procesar eventos en vivo
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class MatchTopicListener {

    private static final Logger logger = LoggerFactory.getLogger(MatchTopicListener.class);

    @Autowired
    private ObjectMapper objectMapper; // Para deserializar el JSON del mensaje

    @Autowired
    private MatchDataService matchDataService; // Para actualizar la caché de partidos

    // El 'destination' debe ser el nombre EXACTO de tu tópico en ActiveMQ
    // El containerFactory es opcional si usas la configuración por defecto,
    // pero es útil si necesitas configuraciones específicas para el listener del tópico.
    @JmsListener(destination = "Match_Topic", containerFactory = "topicListenerFactory")
    public void receiveMatchUpdate(Message message) {
        if (message instanceof TextMessage) {
            try {
                String jsonMessage = ((TextMessage) message).getText();
                logger.info("Mensaje recibido de Match_Topic: {}", jsonMessage);

                // Asumimos que el mensaje es un JSON que representa un MatchEventDTO
                // o una lista de ellos si un mensaje puede contener múltiples actualizaciones.
                // Por ahora, supongamos un solo MatchEventDTO por mensaje para simplificar.
                MatchEventDTO eventDTO = objectMapper.readValue(jsonMessage, MatchEventDTO.class);

                // Aquí necesitarás lógica para:
                // 1. Convertir MatchEventDTO a MatchEvent.
                // 2. Determinar si es un partido nuevo, una actualización, si está en vivo, etc.
                //    Esto podría venir de campos adicionales en tu MatchEventDTO o inferirse.
                // 3. Llamar a un método en MatchDataService para añadir/actualizar el partido en la caché.
                //    Ej: matchDataService.processRealtimeEvent(eventDTO);

                // Ejemplo simple:
                // MatchEvent liveEvent = new MatchEvent(eventDTO.timeStamp, eventDTO.dateTime, ...);
                // liveEvent.setStatus(MatchStatus.LIVE); // Necesitarías el enum MatchStatus
                // matchDataService.addOrUpdateMatch(liveEvent);

                // TODO: Implementar la lógica de procesamiento completa aquí.

            } catch (Exception e) {
                logger.error("Error procesando mensaje de Match_Topic", e);
            }
        } else {
            logger.warn("Mensaje recibido de tipo no esperado: {}", message.getClass().getName());
        }
    }
}
