package com.example.bankcards.controller;

import com.example.bankcards.dto.responce.UserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.UserAdminService;
import com.example.bankcards.util.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.MediaType;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@ContextConfiguration(classes = {
        AdminUserController.class,
        UserAdminService.class,
        UserMapper.class
})
@ExtendWith(MockitoExtension.class)
public class AdminUserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserAdminService userAdminService;

    @MockitoBean
    private UserMapper userMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listsUsers_shouldReturnPageOfUsers() throws Exception {
        UserResponse userResponse = new UserResponse(userId, "userName", Role.ADMIN);

        when(userAdminService.list(any(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(User.builder()
                        .id(userId)
                        .username("admin")
                        .role(Role.ADMIN)
                        .build())));

        when(userMapper.toResponse(any())).thenReturn(userResponse);

        mockMvc.perform(get("/admin/users")
                        .param("q", "admin")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON)))
                .andExpect(result -> status().isOk())
                .andExpect(result -> jsonPath("$.content[0].id").value(userId))
                .andExpect(result -> jsonPath("$.content[0].username").value("admin"))
                .andExpect(result -> jsonPath("$.content[0].role").value("ADMIN"));

    }
}
