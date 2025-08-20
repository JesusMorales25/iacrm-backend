package com.chatgpt.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TotalLeadsHoyDTO {
    private Long total;

    public TotalLeadsHoyDTO() {}

    public TotalLeadsHoyDTO(Long total) {
        this.total = total;
    }

}
