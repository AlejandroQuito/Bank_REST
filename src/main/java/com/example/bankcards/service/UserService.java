package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.responce.AuthResponse;
import com.example.bankcards.entity.User;

public interface UserService {

    AuthResponse registration(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    User requireUserByUsername(String username);

    User requireUserById(Long userId);
}
