package com.example.businessunit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.format.DateTimeFormatter;

@Configuration
public class DateTimeFormatterConfig {

    @Bean
    public DateTimeFormatter timeFormatter() {
        // Este bean no parece estar siendo usado directamente en el código que has mostrado,
        // pero lo mantenemos por si lo usas en otro lado o es para configuración global.
        return DateTimeFormatter.ofPattern("HH:mm");
    }
}