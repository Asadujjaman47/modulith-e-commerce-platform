package com.company.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * Entry point for the E-Commerce Platform.
 *
 * <p>The application is a Modular Monolith built with Spring Modulith. Each business capability
 * lives in its own top-level package (module) under {@code com.company.ecommerce} and communicates
 * with other modules through published events or public module APIs only.
 */
@Modulithic(systemName = "E-Commerce Platform")
@SpringBootApplication
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}