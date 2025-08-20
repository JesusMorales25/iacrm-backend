package com.chatgpt.backend.controller;

import com.chatgpt.backend.dto.login.AuthResponseDTO;
import com.chatgpt.backend.dto.login.LoginRequestDTO;
import com.chatgpt.backend.dto.registro.RegisterRequestDTO;
import com.chatgpt.backend.persistence.User;
import com.chatgpt.backend.security.JwtUtil;
import com.chatgpt.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService; // Para registrar y buscar usuarios

    @PostMapping("/login")
    public AuthResponseDTO login(@RequestBody LoginRequestDTO request) {
        // 1. Autenticar usuario
        var auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. Buscar usuario en BD (acceder a empresaId)
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 3. Generar token con empresaId incluido
        String token = jwtUtil.generateToken(
                user.getUsername(),
                auth.getAuthorities().iterator().next().getAuthority(),
                user.getEmpresaId()
        );

        return new AuthResponseDTO(token);
    }

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequestDTO request) {
        return userService.registerUser(request);
    }
}
