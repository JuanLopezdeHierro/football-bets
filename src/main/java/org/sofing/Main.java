package org.sofing;

import org.sofing.control.DataStorage;
import org.sofing.control.FootballApiClientImpl;
import org.sofing.control.FootballWebScraping;
import org.sofing.model.Match;

public class Main {
    public static void main(String[] args) {
        FootballWebScraping footballWebScraping = new FootballWebScraping();
        Match match = footballWebScraping.betfairScraping();

        FootballApiClientImpl footballApiClient = new FootballApiClientImpl();
        footballApiClient.updateMatchFields(match);

        DataStorage dataStorage = new DataStorage();
        dataStorage.insertMatch(match);

        System.out.println("Datos guardados en la base de datos.");
    }
}