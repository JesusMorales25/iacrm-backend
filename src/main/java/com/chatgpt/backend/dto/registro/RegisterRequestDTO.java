package com.chatgpt.backend.dto.registro;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String username;
    private String password;
    private String role;      // ADMIN, USER, BOT
    private String empresaId; // para multiempresa
}
