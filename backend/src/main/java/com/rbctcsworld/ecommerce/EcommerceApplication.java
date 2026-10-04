package com.rbctcsworld.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * UserDetailsServiceAutoConfiguration is excluded because authentication is JWT-based;
 * otherwise Spring prints an unused "generated security password" on every start.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class EcommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
