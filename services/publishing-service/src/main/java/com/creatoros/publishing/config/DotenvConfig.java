package com.creatoros.publishing.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Configuration
@Slf4j
public class DotenvConfig {

    @PostConstruct
    public void loadEnv() {
        try {
            // Try to find .env in the project root (two levels up from the service)
            File envFile = new File("../../.env");
            
            if (!envFile.exists()) {
                // Fallback to parent directory
                envFile = new File("../.env");
            }
            
            if (!envFile.exists()) {
                log.warn(".env file not found. Using system environment variables.");
                return;
            }

            log.info("Loading .env from: {}", envFile.getAbsolutePath());

            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                int count = 0;
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    // Parse KEY=VALUE
                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex > 0) {
                        String key = line.substring(0, equalsIndex).trim();
                        String value = line.substring(equalsIndex + 1).trim();
                        
                        // Only set if not already defined (system properties take precedence)
                        if (System.getProperty(key) == null && !key.isEmpty()) {
                            System.setProperty(key, value);
                            count++;
                        }
                    }
                }

                log.info("Successfully loaded {} environment variables from .env file", count);
            }
            
        } catch (IOException ex) {
            log.warn("Could not load .env file: {}. Using system environment variables.", ex.getMessage());
        }
    }
}
