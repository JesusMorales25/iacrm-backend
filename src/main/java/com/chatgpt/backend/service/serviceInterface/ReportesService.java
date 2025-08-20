package com.chatgpt.backend.service.serviceInterface;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public interface ReportesService {
    JsonNode obtenerReporte(LocalDateTime inicio, LocalDateTime fin);
}
