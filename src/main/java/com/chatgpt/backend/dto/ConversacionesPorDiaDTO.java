package com.chatgpt.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConversacionesPorDiaDTO {
    private String dia;
    private Long total_conversaciones;

    public ConversacionesPorDiaDTO(String dia, Long totalConversaciones) {
        this.dia = dia;
        this.total_conversaciones = totalConversaciones;
    }

}
