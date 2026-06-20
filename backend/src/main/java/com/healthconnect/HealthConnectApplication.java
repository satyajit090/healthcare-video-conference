package com.healthconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HealthConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthConnectApplication.class, args);
    }
}
