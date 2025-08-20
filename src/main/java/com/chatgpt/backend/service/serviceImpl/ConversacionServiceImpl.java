package com.chatgpt.backend.service.serviceImpl;

import com.chatgpt.backend.dto.ConversacionesPorDiaDTO;
import com.chatgpt.backend.repository.ConversacionRepository;
import com.chatgpt.backend.service.serviceInterface.ConversacionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConversacionServiceImpl implements ConversacionService {

    private final ConversacionRepository conversacionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ConversacionServiceImpl(ConversacionRepository conversacionRepository) {
        this.conversacionRepository = conversacionRepository;
    }

    @Override
    public Long obtenerConversacionesHoy() {
        return conversacionRepository.contarConversacionesHoy();
    }

    @Override
    public List<ConversacionesPorDiaDTO> obtenerConversacionesPorDia() {
        List<Object[]> resultados = conversacionRepository.obtenerConversacionesPorDiaRaw();
        List<ConversacionesPorDiaDTO> lista = new ArrayList<>();

        for (Object[] fila : resultados) {
            String dia = fila[0].toString();
            Long total = ((Number) fila[1]).longValue();
            lista.add(new ConversacionesPorDiaDTO(dia, total));
        }

        return lista;
    }

    @Override
    public double obtenerTiempoPromedioRespuesta() {
        Double result = conversacionRepository.obtenerTiempoPromedioRespuestaRaw();
        return result != null ? result : 0.0;
    }

    public Long getTotalContactos() {
        Long total = conversacionRepository.obtenerTotalContactos();
        return total != null ? total : 0L;
    }
}
