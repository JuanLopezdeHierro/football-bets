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

    private static final String URL     = "tcp://localhost:61616";
    private static final String SUBJECT = "MatchApi_Topic";

    public static void main(String[] args) throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(URL);
        Connection connection     = factory.createConnection();
        connection.setClientID("ApiListener");
        connection.start();

        Session session       = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination= session.createQueue(SUBJECT);
        MessageConsumer consumer = session.createConsumer(destination);

        consumer.setMessageListener(message -> {
            try {
                String content = ((TextMessage) message).getText();

                long ts = Instant.now().getEpochSecond();
                LocalDate date = Instant.ofEpochSecond(ts)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                String dateString = date.format(DateTimeFormatter.BASIC_ISO_DATE);

                String topicName = ((Queue) message.getJMSDestination()).getQueueName();

                String ss = "default";

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
    }
}
