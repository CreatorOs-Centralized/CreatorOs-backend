package com.creatoros.profile.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database Configuration
 * 
 * Configures JPA repositories, auditing, and transaction management
 * for the Profile Service.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.creatoros.profile.repositories")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {
    // Additional database configuration can be added here if needed
}
