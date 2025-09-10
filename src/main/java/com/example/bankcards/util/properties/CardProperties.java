package com.example.bankcards.util.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "card")
public class CardProperties {

    private Status status = new Status();
    private Number number = new Number();

    @Getter
    @Setter
    public static class Status {
        private String active;
        private String blocked;
        private String expired;
    }

    @Getter
    @Setter
    public static class Number {
        private String maskPattern;
        private int visibleDigits;
    }
}
