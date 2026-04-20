package com.example.toycontent;

import com.example.toycontent.app.reward.exp.config.RewardProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RewardProperties.class)
public class ToyContentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToyContentApplication.class, args);
    }

}
