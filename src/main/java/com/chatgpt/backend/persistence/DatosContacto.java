package com.chatgpt.backend.persistence;

import com.chatgpt.backend.enums.CategoriaContacto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "datos_contacto", schema = "iacrm")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatosContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String correo;
    private String telefono;
    @Column(name = "numero_usuario")
    private String numeroUsuario;
    @Column(name = "thread_id")
    private String threadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaContacto categoria;

}