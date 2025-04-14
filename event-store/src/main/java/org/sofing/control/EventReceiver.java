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
    private static String subject = "Match_Queue";

    public static void main(String[] args) throws JMSException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        Connection connection = connectionFactory.createConnection();
        connection.setClientID("octavio");
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(subject);
        MessageConsumer consumer = session.createConsumer(destination);

        consumer.setMessageListener(message -> {
            try {
                String content = ((TextMessage) message).getText();

                // Extraer timestamp y convertirlo a fecha YYYYMMDD
                long ts = Instant.now().getEpochSecond(); // Aquí puedes usar el timestamp del evento si lo tienes
                LocalDate date = Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate();
                String dateString = date.format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD

                // Obtener nombre del tópico (queue)
                String topicName = ((Queue) message.getJMSDestination()).getQueueName();

                // Obtener campo "ss" del JSON o valor por defecto
                String ss = "default"; // Modificar esto según los datos del mensaje

                // Construir ruta: datalake/eventstore/{topic}/{ss}/{YYYYMMDD}.events
                Path outputPath = Paths.get("datalake", "eventstore", topicName, ss);
                Files.createDirectories(outputPath);
                Path file = outputPath.resolve(dateString + ".events");

                // Escribir una línea (append)
                Files.writeString(file, content + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                System.out.println("Evento guardado en: " + file.toString());

            } catch (JMSException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("running");
    }
}