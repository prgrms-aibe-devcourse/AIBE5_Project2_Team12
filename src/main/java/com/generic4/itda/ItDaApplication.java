package com.generic4.itda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class ItDaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItDaApplication.class, args);
    }

}
