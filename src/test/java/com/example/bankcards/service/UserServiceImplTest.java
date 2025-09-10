package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.responce.AuthResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InvalidTokenException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private CustomUserDetails userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthenticationManager authManager;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword123";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final Long USER_ID = 1L;

    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setPassword(ENCODED_PASSWORD);
        testUser.setRole(Role.USER);

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(USERNAME)
                .password(ENCODED_PASSWORD)
                .roles("USER")
                .build();
    }

    @Test
    void registration_ShouldReturnAuthResponse_WhenValidRequest() {
        RegisterRequest request = new RegisterRequest(USERNAME, PASSWORD, Role.USER);

        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        when(jwtUtils.generateAccessToken(userDetails)).thenReturn(ACCESS_TOKEN);
        when(jwtUtils.generateRefreshToken(userDetails)).thenReturn(REFRESH_TOKEN);

        AuthResponse response = userService.registration(request);

        assertNotNull(response);
        assertEquals(ACCESS_TOKEN, response.accessToken());
        assertEquals(REFRESH_TOKEN, response.refreshToken());

        verify(passwordEncoder).encode(PASSWORD);
        verify(userDetailsService).saveUser(USERNAME, ENCODED_PASSWORD, Role.USER);
        verify(userDetailsService).loadUserByUsername(USERNAME);
        verify(jwtUtils).generateAccessToken(userDetails);
        verify(jwtUtils).generateRefreshToken(userDetails);
    }

    @Test
    void refresh_ShouldReturnNewAuthResponse_WhenValidRefreshToken() {
        RefreshRequest request = new RefreshRequest(REFRESH_TOKEN);

        when(jwtUtils.extractUsername(REFRESH_TOKEN)).thenReturn(USERNAME);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        when(jwtUtils.isTokenValid(REFRESH_TOKEN, userDetails)).thenReturn(true);
        when(jwtUtils.generateAccessToken(userDetails)).thenReturn("newAccessToken");
        when(jwtUtils.generateRefreshToken(userDetails)).thenReturn("newRefreshToken");

        AuthResponse response = userService.refresh(request);

        assertNotNull(response);
        assertEquals("newAccessToken", response.accessToken());
        assertEquals("newRefreshToken", response.refreshToken());

        verify(jwtUtils).extractUsername(REFRESH_TOKEN);
        verify(userDetailsService).loadUserByUsername(USERNAME);
        verify(jwtUtils).isTokenValid(REFRESH_TOKEN, userDetails);
        verify(jwtUtils).generateAccessToken(userDetails);
        verify(jwtUtils).generateRefreshToken(userDetails);
    }

    @Test
    void requireUserByUsername_ShouldReturnUser_WhenUserExists() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        User result = userService.requireUserByUsername(USERNAME);

        assertNotNull(result);
        assertEquals(USERNAME, result.getUsername());
        verify(userRepository).findByUsername(USERNAME);
    }

    @Test
    void requireUserById_ShouldReturnUser_WhenUserExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        User result = userService.requireUserById(USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
        verify(userRepository).findById(USER_ID);
    }

    @Test
    void refresh_ShouldThrowInvalidTokenException_WhenInvalidRefreshToken() {
        RefreshRequest request = new RefreshRequest("invalidToken");

        when(jwtUtils.extractUsername("invalidToken")).thenReturn(USERNAME);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        when(jwtUtils.isTokenValid("invalidToken", userDetails)).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> userService.refresh(request));

        verify(jwtUtils).extractUsername("invalidToken");
        verify(userDetailsService).loadUserByUsername(USERNAME);
        verify(jwtUtils).isTokenValid("invalidToken", userDetails);
        verify(jwtUtils, never()).generateAccessToken(any());
        verify(jwtUtils, never()).generateRefreshToken(any());
    }

    @Test
    void refresh_ShouldThrowInvalidTokenException_WhenTokenUsernameNotFound() {
        RefreshRequest request = new RefreshRequest(REFRESH_TOKEN);

        when(jwtUtils.extractUsername(REFRESH_TOKEN)).thenReturn("nonexistentuser");
        when(userDetailsService.loadUserByUsername("nonexistentuser"))
                .thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> userService.refresh(request));

        verify(jwtUtils).extractUsername(REFRESH_TOKEN);
        verify(userDetailsService).loadUserByUsername("nonexistentuser");
        verify(jwtUtils, never()).isTokenValid(anyString(), any());
    }

    @Test
    void requireUserByUsername_ShouldThrowUserNotFoundException_WhenUserNotExists() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                userService.requireUserByUsername("nonexistent"));

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void requireUserById_ShouldThrowUserNotFoundException_WhenUserNotExists() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                userService.requireUserById(999L));

        verify(userRepository).findById(999L);
    }

    @Test
    void refresh_ShouldThrowInvalidTokenException_WhenNullRefreshToken() {
        RefreshRequest request = new RefreshRequest(null);

        assertThrows(InvalidTokenException.class, () -> userService.refresh(request));

        verify(jwtUtils, never()).extractUsername(any());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void refresh_ShouldThrowInvalidTokenException_WhenEmptyRefreshToken() {
        RefreshRequest request = new RefreshRequest("");

        assertThrows(InvalidTokenException.class, () -> userService.refresh(request));

        verify(jwtUtils, never()).extractUsername(any());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void requireUserByUsername_ShouldThrowException_WhenUsernameIsNull() {
        assertThrows(UserNotFoundException.class, () ->
                userService.requireUserByUsername(null));

        verify(userRepository).findByUsername(null);
    }

    @Test
    void requireUserById_ShouldThrowException_WhenIdIsNull() {
        assertThrows(UserNotFoundException.class, () ->
                userService.requireUserById(null));

        verify(userRepository).findById(null);
    }
}
