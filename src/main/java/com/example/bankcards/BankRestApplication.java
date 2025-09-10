package com.example.bankcards;

import com.example.bankcards.util.properties.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(JwtProperties.class)
@EnableJpaRepositories(basePackages = "com.example.bankcards.repository")
public class BankRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankRestApplication.class, args);
    }
}
