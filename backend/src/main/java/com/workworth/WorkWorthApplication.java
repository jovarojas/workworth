package com.workworth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkWorthApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkWorthApplication.class, args);
    }
}
