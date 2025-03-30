package org.sofing;

import org.sofing.control.Controller;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Por favor, introduce tu API key: ");
        String apiKey = scanner.nextLine();

        Controller controller = new Controller(apiKey);
        controller.start();

        // Agregar un shutdown hook para detener el scheduler al finalizar la aplicación
        Runtime.getRuntime().addShutdownHook(new Thread(controller::stop));
    }
}