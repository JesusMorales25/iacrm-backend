package com.chatgpt.backend.service.serviceImpl;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.chatgpt.backend.repository.sp.ReportesRepository;
import com.chatgpt.backend.service.serviceInterface.ReportesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
public class ReportesServiceImpl implements ReportesService {

    @Autowired
    private ReportesRepository repo;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public JsonNode obtenerReporte(LocalDateTime inicio, LocalDateTime fin) {
        String json = repo.obtenerReporte(
                Timestamp.valueOf(inicio),
                Timestamp.valueOf(fin)
        );

        try {
            JsonNode base = mapper.readTree(json);
            // Devolver siempre un objeto con 'metricas' como array vacío
            ObjectNode response = mapper.createObjectNode();
            response.set("metricas", mapper.createArrayNode());
            response.set("total", base.get("total"));
            response.set("inicio", base.get("inicio"));
            response.set("fin", base.get("fin"));
            return response;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al procesar el JSON del reporte", e);
        }
    }
}
