package org.sofing.control;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONArray;

public class EventProvider {
    private static String url = "tcp://localhost:61616";
    private static String subject = "Match_Topic";

    public void matchInfoArray(JSONArray jsonArray) throws JMSException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Destination destination = session.createQueue(subject);

        MessageProducer producer = session.createProducer(destination);

        TextMessage message = session.createTextMessage(jsonArray.toString());
        producer.send(message);

        System.out.println("Mensaje enviado al topic: " + message.getText());
        connection.close();
    }
}