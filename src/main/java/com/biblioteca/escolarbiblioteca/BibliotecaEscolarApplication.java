package com.biblioteca.escolarbiblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BibliotecaEscolarApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibliotecaEscolarApplication.class, args);
    }
}