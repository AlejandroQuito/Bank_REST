package com.example.bankcards.dto.responce;

import java.math.BigDecimal;
import java.util.Date;

public record CardResponse(
        Long id,
        String cardNumber,
        String owner,
        Date expiryDate,
        String status,
        BigDecimal balance
) {
}
