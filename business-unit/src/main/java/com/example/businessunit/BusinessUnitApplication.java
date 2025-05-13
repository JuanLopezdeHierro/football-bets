package com.example.businessunit; // Asegúrate que el paquete sea correcto

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BusinessUnitApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessUnitApplication.class, args);
        System.out.println("✅ Business Unit iniciada en: http://localhost:8080/laliga/matches"); // Ruta actualizada
    }
}