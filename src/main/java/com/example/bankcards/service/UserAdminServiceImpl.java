package com.example.bankcards.service;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.dto.UserUpdateDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<User> list(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByUsernameContainingIgnoreCase(q, pageable);
    }

    public User create(UserDTO dto) {
        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        User u = new User();
        u.setUsername(dto.username());
        u.setPassword(passwordEncoder.encode(dto.password()));
        u.setRole(dto.role());
        return userRepository.save(u);
    }

    public User update(Long id, UserUpdateDTO dto) {
        User u = userRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (dto.username() != null && !dto.username().isBlank()) {
            u.setUsername(dto.username());
        }
        if (dto.password() != null && !dto.password().isBlank()) {
            u.setPassword(passwordEncoder.encode(dto.password()));
        }
        if (dto.role() != null) {
            u.setRole(dto.role());
        }
        return userRepository.save(u);
    }

    public void delete(Long id) {
        User u = userRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        userRepository.delete(u);
    }
}
