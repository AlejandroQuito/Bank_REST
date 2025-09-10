package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.responce.AuthResponse;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registration(@Valid @RequestBody RegisterRequest request) {
        return userService.registration(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/refresh-tokens")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return userService.refresh(request);
    }
}
