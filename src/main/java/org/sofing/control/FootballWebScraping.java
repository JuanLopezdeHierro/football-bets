package org.sofing.control;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.lang.model.util.Elements;
import javax.swing.*;
import java.io.IOException;
import java.util.List;

public class FootballWebScraping {
    public void betfairScraping() {
        String url = "https://www.betfair.es/sport/football/premier-league-inglesa/10932509";
        try {
            Document connection = Jsoup.connect(url).get();
            System.out.println("Conexión establecida");

            List<Element> teams = connection.select("span.team-name");
            List<Element> odds = connection.select("span.ui-runner-price.ui-display-decimal-price");
            List<Element> dateTime = connection.select("span.date.ui-countdown");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTeamName(Element team) {
        return team.text();
    }

    public double getOdds(Element odd) {
        return Double.parseDouble(odd.text());
    }

    public String getDateTime(Element dateTime) {
        return dateTime.text();
    }
}

