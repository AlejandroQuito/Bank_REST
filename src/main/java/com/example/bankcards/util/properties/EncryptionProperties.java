package com.example.bankcards.util.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties {

    private String key;
    private String algorithm;
    private int ivLength;
    private int tagLength;
    private String charset;
    private String maskingPattern;
}
