package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Date;

public record CardDTO(
        @NotBlank
        @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
        String number,
        @NotNull Long ownerId,
        @NotNull Date expiration,
        @NotNull @PositiveOrZero BigDecimal balance
) {
}
