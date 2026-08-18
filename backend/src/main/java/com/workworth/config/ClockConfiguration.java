package com.workworth.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

    @Bean
    Clock clock(@Value("${workworth.time-zone:Europe/Madrid}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
