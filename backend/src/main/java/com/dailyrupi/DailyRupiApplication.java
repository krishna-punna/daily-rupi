package com.dailyrupi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DailyRupiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyRupiApplication.class, args);
    }
}
