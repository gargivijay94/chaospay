package com.chaospay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChaosPayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChaosPayApplication.class, args);
    }
}
