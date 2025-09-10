package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;
import jakarta.validation.constraints.Size;

public record UserUpdateDTO(
        @Size(min = 3, max = 50)
        String username,
        @Size(min = 6, max = 100)
        String password,
        Role role
) {
}
