package org.sofing.control;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class EventReceiverApi {

    // Broker y topic para la API
    private static final String URL     = "tcp://localhost:61616";
    private static final String SUBJECT = "MatchApi_Topic";

    public static void main(String[] args) throws JMSException {
        // 1) Conectar con ActiveMQ
        ConnectionFactory factory = new ActiveMQConnectionFactory(URL);
        Connection connection     = factory.createConnection();
        connection.setClientID("ApiListener");
        connection.start();

        // 2) Crear sesión y consumidor sobre el queue SUBJECT
        Session session       = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination= session.createQueue(SUBJECT);
        MessageConsumer consumer = session.createConsumer(destination);

        // 3) Listener que vuelca cada mensaje a disco
        consumer.setMessageListener(message -> {
            try {
                String content = ((TextMessage) message).getText();

                // Fecha actual en formato YYYYMMDD
                long ts = Instant.now().getEpochSecond();
                LocalDate date = Instant.ofEpochSecond(ts)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                String dateString = date.format(DateTimeFormatter.BASIC_ISO_DATE);

                // Recuperar nombre del topic (debería ser SUBJECT)
                String topicName = ((Queue) message.getJMSDestination()).getQueueName();

                // Subdirectorio (igual que en tu otro receiver)
                String ss = "default";

                // Path: datalake/eventstore/<topicName>/default/<YYYYMMDD>.events
                Path outputDir = Paths.get("datalake", "eventstore", topicName, ss);
                Files.createDirectories(outputDir);

                Path file = outputDir.resolve(dateString + ".events");
                Files.writeString(
                        file,
                        content + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );

                System.out.println("Evento API guardado en: " + file);

            } catch (JMSException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("EventReceiverApi corriendo y escuchando en " + SUBJECT);
        // Nota: el proceso se mantiene vivo mientras el listener esté activo
    }
}
