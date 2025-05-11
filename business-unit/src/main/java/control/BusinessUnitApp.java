package control;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BusinessUnitApp {

    public static void main(String[] args) {
        SpringApplication.run(BusinessUnitApp.class, args);
        System.out.println("✅ Business Unit iniciada en: http://localhost:8080");
    }
}
