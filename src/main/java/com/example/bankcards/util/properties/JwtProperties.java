package com.example.bankcards.util.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "spring.security.jwt")
public class JwtProperties {

    private String secret;
    private long accessExpirationMs;
    private long refreshExpirationMs;
}
