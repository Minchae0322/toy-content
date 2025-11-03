package com.example.toycontent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ToyContentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToyContentApplication.class, args);
    }

}
