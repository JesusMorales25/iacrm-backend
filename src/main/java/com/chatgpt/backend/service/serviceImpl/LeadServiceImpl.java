package com.chatgpt.backend.service.serviceImpl;

import com.chatgpt.backend.dto.LeadDTO;
import com.chatgpt.backend.dto.LeadsPorEstadoDTO;
import com.chatgpt.backend.dto.TotalLeadsHoyDTO;
import com.chatgpt.backend.repository.DatosContactoRepository;
import com.chatgpt.backend.repository.sp.LeadSPRepository;
import com.chatgpt.backend.service.serviceInterface.LeadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeadServiceImpl implements LeadService {

    private final LeadSPRepository leadSPRepository;
    private final DatosContactoRepository repository;

    public LeadServiceImpl(LeadSPRepository leadSPRepository,
                           DatosContactoRepository repository) {
        this.leadSPRepository = leadSPRepository;
        this.repository = repository;
    }

    @Override
    public List<LeadDTO> getLeadsDatos() {
    List<Object[]> resultados = repository.obtenerLeads();
    return resultados.stream()
        .map(r -> new LeadDTO(
            (String) r[1], // nombre
            r[2] != null ? r[2].toString() : null, // numeroUsuario (telefono)
            r[2] != null ? r[2].toString() : null, // telefono (igual que numeroUsuario)
            (String) r[3], // correo
            (String) r[4]  // categoria
        ))
        .toList();
    }

    @Override
    public List<LeadsPorEstadoDTO> obtenerLeadsPorEstado() {
        List<Object[]> resultados = leadSPRepository.obtenerLeadsPorEstado();
        List<LeadsPorEstadoDTO> leadsPorEstado = new ArrayList<>();

        for (Object[] fila : resultados) {
            String categoria = (String) fila[0];
            Long total = ((Number) fila[1]).longValue();
            leadsPorEstado.add(new LeadsPorEstadoDTO(categoria, total));
        }
        return leadsPorEstado;
    }

    @Override
    public TotalLeadsHoyDTO obtenerTotalLeadsHoy() {
        Long total = leadSPRepository.obtenerTotalLeadsHoy();
        return new TotalLeadsHoyDTO(total);
    }
}
