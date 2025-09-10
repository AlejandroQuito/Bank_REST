package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.responce.AuthResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.security.JwtUtils;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {
        UserController.class,
        UserService.class,
        JwtAuthFilter.class,
        JwtUtils.class,
        UserRepository.class,
        CustomUserDetails.class
})
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CustomUserDetails customUserDetails;

    @MockitoBean
    private JwtUtils jwtUtils;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    void registration_ShouldReturnCreatedStatusAndAuthResponse() throws Exception {
        RegisterRequest registerRequest =
                new RegisterRequest("test@example.com", "password123", Role.USER);

        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token");

        when(userService.registration(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/v1/users/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void registration_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "short", Role.USER);

        mockMvc.perform(post("/v1/users/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldReturnAuthResponse() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token");

        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void login_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post("/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_ShouldReturnAuthResponse() throws Exception {
        RefreshRequest refreshRequest = new RefreshRequest("old-refresh-token");

        AuthResponse authResponse = new AuthResponse("new-access-token", "new-refresh-token");

        when(userService.refresh(any(RefreshRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/v1/users/refresh-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refresh_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        RefreshRequest invalidRequest = new RefreshRequest("");

        mockMvc.perform(post("/v1/users/refresh-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
