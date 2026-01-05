package com.helpdesk.departement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class DepartementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DepartementServiceApplication.class, args);
    }

}
