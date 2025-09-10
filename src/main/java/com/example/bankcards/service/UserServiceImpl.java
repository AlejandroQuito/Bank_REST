package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.responce.AuthResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InvalidTokenException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final CustomUserDetails userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authManager;

    public AuthResponse registration(RegisterRequest request) {
        log.info("Registering new user: {}", request.username());

        userDetailsService.saveUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.role()
        );

        UserDetails user = userDetailsService
                .loadUserByUsername(request.username());

        return generateAuthResponse(user);
    }

    @Cacheable(value = "auth_responses", key = "#request.username + '_login'")
    public AuthResponse login(LoginRequest request) {
        checkUserExists(request.username());
        log.info("Login attempt for user: {}", request.username());

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        UserDetails user = userDetailsService.loadUserByUsername(request.username());
        return generateAuthResponse(user);
    }

    @Cacheable(value = "auth_responses", key = "#request.refreshToken + '_refresh'")
    public AuthResponse refresh(RefreshRequest request) {
        log.info("Refreshing token");

        String refreshToken = request.refreshToken();
        validateRefreshToken(refreshToken);
        String username = jwtUtils.extractUsername(refreshToken);

        UserDetails user = userDetailsService.loadUserByUsername(username);

        if (!jwtUtils.isTokenValid(refreshToken, user)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        return generateAuthResponse(user);
    }

    @Cacheable(value = "users", key = "#username")
    public User requireUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    @Cacheable(value = "users", key = "#userId")
    public User requireUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token cannot be null or empty");
        }
    }

    private void checkUserExists(String username) {
        try {
            userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            throw new UserNotFoundException("User not found: " + username);
        }
    }

    private AuthResponse generateAuthResponse(UserDetails user) {
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken);
    }
}
