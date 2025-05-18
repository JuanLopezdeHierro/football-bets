package org.sofing.control;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONArray;

public class EventProvider {
    private final String url;
    private final String subject;

    public EventProvider(String url, String subject) {
        this.url     = url;
        this.subject = subject;
    }

    public void matchInfoArray(JSONArray jsonArray) throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(url);
        Connection connection     = factory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(subject);

        MessageProducer producer = session.createProducer(destination);
        TextMessage message      = session.createTextMessage(jsonArray.toString());

        producer.send(message);
        System.out.println("Mensaje enviado a " + subject + ": " + message.getText());

        connection.close();
    }
}
