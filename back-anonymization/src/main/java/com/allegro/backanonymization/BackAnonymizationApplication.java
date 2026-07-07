package com.allegro.backanonymization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BackAnonymizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackAnonymizationApplication.class, args);
    }

}
