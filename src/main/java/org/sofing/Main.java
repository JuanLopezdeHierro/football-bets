package org.sofing;

import org.sofing.control.FootballApiClientImpl;
import org.sofing.control.FootballWebScraping;
import org.sofing.model.Match;

public class Main {
    public static void main(String[] args) {
        FootballApiClientImpl footballApiClient = new FootballApiClientImpl();
        FootballWebScraping footballWebScraping = new FootballWebScraping();
        Match match = footballWebScraping.betfairScraping();
        System.out.println(footballApiClient.getMatchFields());
    }
}