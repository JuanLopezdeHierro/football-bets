package org.sofing;

import org.sofing.control.Controller;

public class Main {
    public static void main(String[] args) {
        Controller controller = new Controller();
        controller.start();
        System.out.println("Feeder iniciado. Presiona Ctrl+C para detener.");

        Runtime.getRuntime()
                .addShutdownHook(new Thread(() -> {
                    System.out.println("Deteniendo el feeder...");
                    controller.stop();
                    System.out.println("Feeder detenido.");
                }));
    }
}
