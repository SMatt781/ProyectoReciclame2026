package com.example.proyectoreciclame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProyectoReciclameApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProyectoReciclameApplication.class, args);
    }

}
