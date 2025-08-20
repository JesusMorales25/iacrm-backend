package com.chatgpt.backend.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(name = "conversacion", schema = "iacrm")
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numero;

    @Column(name = "thread_id")
    private String threadId;

    @Column(name = "mensaje_usuario", columnDefinition = "TEXT")
    private String mensajeUsuario;

    @Column(name="respuesta_bot", columnDefinition = "TEXT")
    private String respuestaBot;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;
}
