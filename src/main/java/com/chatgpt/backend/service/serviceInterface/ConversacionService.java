package com.chatgpt.backend.service.serviceInterface;

import com.chatgpt.backend.dto.ConversacionesPorDiaDTO;

import java.util.List;

public interface ConversacionService {
    Long obtenerConversacionesHoy();
    List<ConversacionesPorDiaDTO> obtenerConversacionesPorDia();
    double obtenerTiempoPromedioRespuesta();
    Long getTotalContactos();
}
