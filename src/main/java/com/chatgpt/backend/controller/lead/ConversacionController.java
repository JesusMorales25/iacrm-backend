package com.chatgpt.backend.controller.lead;

import com.chatgpt.backend.dto.ConversacionesPorDiaDTO;
import com.chatgpt.backend.service.serviceInterface.ConversacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversaciones")
public class ConversacionController {

    private final ConversacionService conversacionService;

    @Autowired
    public ConversacionController(ConversacionService conversacionService) {
        this.conversacionService = conversacionService;
    }

    @GetMapping("/hoy")
    public Long obtenerConversacionesHoy() {
        return conversacionService.obtenerConversacionesHoy();
    }

    @GetMapping("/por-dia")
    public ResponseEntity<List<ConversacionesPorDiaDTO>> obtenerConversacionesPorDia() {
        return ResponseEntity.ok(conversacionService.obtenerConversacionesPorDia());
    }

    @GetMapping("/tiempo-promedio-respuesta")
    public ResponseEntity<Map<String, Object>> obtenerTiempoPromedioRespuesta() {
        double promedio = conversacionService.obtenerTiempoPromedioRespuesta();
        Map<String, Object> response = new HashMap<>();
        response.put("tiempo_promedio_min", promedio);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/total-contactos")
    public ResponseEntity<Long> obtenerTotalContactos() {
        return ResponseEntity.ok(conversacionService.getTotalContactos());
    }
}
