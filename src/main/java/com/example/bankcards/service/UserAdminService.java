package com.example.bankcards.service;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.dto.UserUpdateDTO;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAdminService {

    Page<User> list(String q, Pageable pageable);

    User create(UserDTO dto);

    User update(Long id, UserUpdateDTO dto);

    void delete(Long id);
}
