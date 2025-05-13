package org.sofing;

import org.sofing.control.Controller;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Controller controller = new Controller();
        controller.start();

        Runtime.getRuntime().addShutdownHook(new Thread(controller::stop));
    }
}