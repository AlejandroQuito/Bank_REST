package com.example.bankcards.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record TransferDTO(
        @NotNull Long fromCardId,
        @NotNull Long toCardId,
        @NotNull
        @PositiveOrZero
        BigDecimal amount
) {
}
