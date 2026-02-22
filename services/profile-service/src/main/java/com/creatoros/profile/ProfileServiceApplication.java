package com.creatoros.profile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Profile Service Application
 * 
 * Main entry point for the Creator Profile Management Service.
 * This service handles creator profiles, bio, social links, and related profile metadata.
 */
@SpringBootApplication
public class ProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileServiceApplication.class, args);
    }
}
