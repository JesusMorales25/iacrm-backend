package com.chatgpt.backend.dto.login;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String username;
    private String password;
}
