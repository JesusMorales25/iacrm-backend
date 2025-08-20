package com.chatgpt.backend.service.serviceInterface;

import com.chatgpt.backend.dto.LeadDTO;
import com.chatgpt.backend.dto.LeadsPorEstadoDTO;
import com.chatgpt.backend.dto.TotalLeadsHoyDTO;

import java.util.List;

public interface LeadService {
    List<LeadDTO> getLeadsDatos();
    List<LeadsPorEstadoDTO> obtenerLeadsPorEstado();
    TotalLeadsHoyDTO obtenerTotalLeadsHoy();
}
