package com.flightagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FlightAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlightAgentApplication.class, args);
    }
}
