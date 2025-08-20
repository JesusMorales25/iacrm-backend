package com.chatgpt.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LeadsPorEstadoDTO {
    private String categoria;
    private Long total;

    public LeadsPorEstadoDTO(String categoria, Long total) {
        this.categoria = categoria;
        this.total = total;
    }

}
