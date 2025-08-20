package com.chatgpt.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class LeadDTO {
    private String nombre;
    private String numeroUsuario;
    private String telefono;
    private String correo;
    private String categoria;

    public LeadDTO(String nombre, String numeroUsuario, String telefono, String correo, String categoria) {
        this.nombre = nombre;
        this.numeroUsuario = numeroUsuario;
        this.telefono = telefono;
        this.correo = correo;
        this.categoria = categoria;
    }

}
