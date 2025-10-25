package com.chatgpt.backend.repository;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 * Implementación personalizada de ConversacionRepository
 * SIMPLIFICADO: Usa directamente schema public
 */
@Repository
public class ConversacionRepositoryImpl implements ConversacionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Cuenta las conversaciones de hoy - SIMPLIFICADO usando public schema
     */
    public Long contarConversacionesHoy() {
        String sql = "SELECT * FROM public.sp_conversaciones_hoy()";
        Query query = entityManager.createNativeQuery(sql);
        
        Object result = query.getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    /**
     * Obtiene conversaciones por día - SIMPLIFICADO usando public schema
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerConversacionesPorDiaRaw() {
        String sql = "SELECT * FROM public.sp_conversaciones_por_dia()";
        Query query = entityManager.createNativeQuery(sql);
        
        return query.getResultList();
    }

    /**
     * Obtiene tiempo promedio de respuesta - SIMPLIFICADO usando public schema
     */
    public Double obtenerTiempoPromedioRespuestaRaw() {
        String sql = "SELECT AVG(EXTRACT(EPOCH FROM (fecha_hora - fecha_hora))) FROM public.conversacion WHERE DATE(fecha_hora) = CURRENT_DATE";
        Query query = entityManager.createNativeQuery(sql);
        
        Object result = query.getSingleResult();
        return result != null ? ((Number) result).doubleValue() : 0.0;
    }

    /**
     * Obtiene total de contactos - SIMPLIFICADO usando public schema
     */
    public Long obtenerTotalContactos() {
        String sql = "SELECT * FROM public.sp_total_contactos()";
        Query query = entityManager.createNativeQuery(sql);
        
        Object result = query.getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    /**
     * Cuenta total de mensajes por número de usuario - SIMPLIFICADO usando public schema
     */
    public Long countByNumero(String numero) {
        String sql = "SELECT COUNT(*) FROM public.conversacion WHERE numero = ?";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, numero);
        
        Object result = query.getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }
}