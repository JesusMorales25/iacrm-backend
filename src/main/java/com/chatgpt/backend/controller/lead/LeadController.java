package com.chatgpt.backend.controller.lead;

import com.chatgpt.backend.dto.LeadDTO;
import com.chatgpt.backend.dto.LeadsPorEstadoDTO;
import com.chatgpt.backend.dto.TotalLeadsHoyDTO;
import com.chatgpt.backend.service.serviceInterface.LeadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
@CrossOrigin(origins = "*")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping("/datos")
    public ResponseEntity<List<LeadDTO>> listarLeads() {
        return ResponseEntity.ok(leadService.getLeadsDatos());
    }

    @GetMapping("/por-estado")
    public List<LeadsPorEstadoDTO> obtenerLeadsPorEstado() {
        return leadService.obtenerLeadsPorEstado();
    }

    @GetMapping("/total-hoy")
    public TotalLeadsHoyDTO obtenerTotalLeadsHoy() {
        return leadService.obtenerTotalLeadsHoy();
    }
}
