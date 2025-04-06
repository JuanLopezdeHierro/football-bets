package org.sofing;

import org.json.JSONObject;
import org.sofing.control.Controller;
import org.sofing.control.FootballWebScraping;
import org.sofing.control.FootballWebScrapingImpl;
import org.sofing.model.Match;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        //Scanner scanner = new Scanner(System.in);
        //System.out.print("Por favor, introduce tu API key: ");
        //String apiKey = scanner.nextLine();

        //Controller controller = new Controller(apiKey);
        //controller.start();

        //Runtime.getRuntime().addShutdownHook(new Thread(controller::stop));

        FootballWebScrapingImpl footballWebScraping = new FootballWebScrapingImpl();
        Match match = footballWebScraping.betfairScraping();
        footballWebScraping.dataToJson(match);
    }
}