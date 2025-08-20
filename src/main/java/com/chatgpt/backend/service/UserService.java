package com.chatgpt.backend.service;

import com.chatgpt.backend.dto.registro.RegisterRequestDTO;
import com.chatgpt.backend.persistence.User;

import java.util.Optional;

public interface UserService {
    User registerUser(RegisterRequestDTO request);
    Optional<User> findByUsername(String username);

}
