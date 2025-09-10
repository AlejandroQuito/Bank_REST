package com.example.bankcards.service;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.dto.UserUpdateDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAdminServiceImpl userAdminService;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword123";
    private static final String NEW_USERNAME = "newuser";
    private static final String NEW_PASSWORD = "newpassword123";

    private User testUser;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setPassword(ENCODED_PASSWORD);
        testUser.setRole(Role.USER);

        pageable = Pageable.ofSize(10).withPage(0);
    }

    @Test
    void list_ShouldReturnAllUsers_WhenQueryIsNull() {
        Page<User> expectedPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<User> result = userAdminService.list(null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).findByUsernameContainingIgnoreCase(anyString(), any());
    }

    @Test
    void list_ShouldReturnAllUsers_WhenQueryIsBlank() {
        Page<User> expectedPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<User> result = userAdminService.list("   ", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).findByUsernameContainingIgnoreCase(anyString(), any());
    }

    @Test
    void list_ShouldReturnFilteredUsers_WhenQueryIsProvided() {
        String searchQuery = "test";
        Page<User> expectedPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findByUsernameContainingIgnoreCase(searchQuery, pageable)).thenReturn(expectedPage);

        Page<User> result = userAdminService.list(searchQuery, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findByUsernameContainingIgnoreCase(searchQuery, pageable);
        verify(userRepository, never()).findAll((Pageable) any());
    }

    @Test
    void create_ShouldCreateUser_WhenUsernameIsAvailable() {
        UserDTO dto = new UserDTO(USER_ID, USERNAME, PASSWORD, Role.USER);
        User newUser = User.builder()
                .username(USERNAME)
                .password(ENCODED_PASSWORD)
                .role(Role.USER)
                .build();

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        User result = userAdminService.create(dto);

        assertNotNull(result);
        assertEquals(USERNAME, result.getUsername());
        assertEquals(ENCODED_PASSWORD, result.getPassword());
        assertEquals(Role.USER, result.getRole());

        verify(userRepository).findByUsername(USERNAME);
        verify(passwordEncoder).encode(PASSWORD);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_ShouldThrowException_WhenUsernameAlreadyTaken() {
        UserDTO dto = new UserDTO(USER_ID, USERNAME, PASSWORD, Role.USER);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> userAdminService.create(dto));

        verify(userRepository).findByUsername(USERNAME);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_ShouldUpdateAllFields_WhenAllFieldsProvided() {
        UserUpdateDTO dto = new UserUpdateDTO(NEW_USERNAME, NEW_PASSWORD, Role.ADMIN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("encodedNewPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userAdminService.update(USER_ID, dto);

        assertNotNull(result);
        assertEquals(NEW_USERNAME, testUser.getUsername());
        assertEquals("encodedNewPassword", testUser.getPassword());
        assertEquals(Role.ADMIN, testUser.getRole());

        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository).save(testUser);
    }

    @Test
    void update_ShouldUpdateOnlyUsername_WhenOnlyUsernameProvided() {
        UserUpdateDTO dto = new UserUpdateDTO(NEW_USERNAME, null, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userAdminService.update(USER_ID, dto);

        assertNotNull(result);
        assertEquals(NEW_USERNAME, testUser.getUsername());
        assertEquals(ENCODED_PASSWORD, testUser.getPassword()); // unchanged
        assertEquals(Role.USER, testUser.getRole()); // unchanged

        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void update_ShouldUpdateOnlyPassword_WhenOnlyPasswordProvided() {
        UserUpdateDTO dto = new UserUpdateDTO(null, NEW_PASSWORD, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("encodedNewPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userAdminService.update(USER_ID, dto);

        assertNotNull(result);
        assertEquals(USERNAME, testUser.getUsername());
        assertEquals("encodedNewPassword", testUser.getPassword());
        assertEquals(Role.USER, testUser.getRole());

        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository).save(testUser);
    }

    @Test
    void update_ShouldUpdateOnlyRole_WhenOnlyRoleProvided() {
        UserUpdateDTO dto = new UserUpdateDTO(null, null, Role.ADMIN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userAdminService.update(USER_ID, dto);

        assertNotNull(result);
        assertEquals(USERNAME, testUser.getUsername());
        assertEquals(ENCODED_PASSWORD, testUser.getPassword());
        assertEquals(Role.ADMIN, testUser.getRole());

        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void update_ShouldNotChangeAnything_WhenAllFieldsNull() {
        UserUpdateDTO dto = new UserUpdateDTO(null, null, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userAdminService.update(USER_ID, dto);

        assertNotNull(result);
        assertEquals(USERNAME, testUser.getUsername());
        assertEquals(ENCODED_PASSWORD, testUser.getPassword());
        assertEquals(Role.USER, testUser.getRole());

        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void update_ShouldThrowException_WhenUserNotFound() {
        UserUpdateDTO dto = new UserUpdateDTO(NEW_USERNAME, NEW_PASSWORD, Role.ADMIN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userAdminService.update(USER_ID, dto));

        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_ShouldIgnoreBlankUsername() {
        UserUpdateDTO dto = new UserUpdateDTO("   ", NEW_PASSWORD, Role.ADMIN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("encodedNewPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userAdminService.update(USER_ID, dto);

        assertNotNull(result);
        assertEquals(USERNAME, testUser.getUsername());
        assertEquals("encodedNewPassword", testUser.getPassword());
        assertEquals(Role.ADMIN, testUser.getRole());

        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository).save(testUser);
    }

    @Test
    void update_ShouldIgnoreBlankPassword() {
        UserUpdateDTO dto = new UserUpdateDTO(NEW_USERNAME, "   ", Role.ADMIN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userAdminService.update(USER_ID, dto);

        assertNotNull(result);
        assertEquals(NEW_USERNAME, testUser.getUsername());
        assertEquals(ENCODED_PASSWORD, testUser.getPassword());
        assertEquals(Role.ADMIN, testUser.getRole());

        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void delete_ShouldDeleteUser_WhenUserExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        userAdminService.delete(USER_ID);

        verify(userRepository).findById(USER_ID);
        verify(userRepository).delete(testUser);
    }

    @Test
    void delete_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userAdminService.delete(USER_ID));

        verify(userRepository).findById(USER_ID);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void create_ShouldHandleEncodingError() {
        UserDTO dto = new UserDTO(USER_ID, USERNAME, PASSWORD, Role.USER);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenThrow(new RuntimeException("Encoding failed"));

        assertThrows(RuntimeException.class, () -> userAdminService.create(dto));

        verify(userRepository).findByUsername(USERNAME);
        verify(passwordEncoder).encode(PASSWORD);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_ShouldHandleEncodingError() {
        UserUpdateDTO dto = new UserUpdateDTO(null, NEW_PASSWORD, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenThrow(new RuntimeException("Encoding failed"));

        assertThrows(RuntimeException.class, () -> userAdminService.update(USER_ID, dto));

        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository, never()).save(any(User.class));
    }
}
