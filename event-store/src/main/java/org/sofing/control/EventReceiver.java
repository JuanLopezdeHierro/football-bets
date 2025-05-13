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

public class EventReceiver {

    private static String url = "tcp://localhost:61616";
    private static String subject = "Match_Topic";

    public static void main(String[] args) throws JMSException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        Connection connection = connectionFactory.createConnection();
        connection.setClientID("Paco");
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(subject);
        MessageConsumer consumer = session.createConsumer(destination);

        consumer.setMessageListener(message -> {
            try {
                String content = ((TextMessage) message).getText();

                long ts = Instant.now().getEpochSecond();
                LocalDate date = Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate();
                String dateString = date.format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD

                String topicName = ((Queue) message.getJMSDestination()).getQueueName();

                String ss = "default";

                Path outputPath = Paths.get("datalake", "eventstore", topicName, ss);
                Files.createDirectories(outputPath);
                Path file = outputPath.resolve(dateString + ".events");

                Files.writeString(file, content + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                System.out.println("Evento guardado en: " + file);

            } catch (JMSException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("running");
    }
}