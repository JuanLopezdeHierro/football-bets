package com.example.businessunit; // O tu paquete principal

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BusinessUnitApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessUnitApplication.class, args);
    }
}