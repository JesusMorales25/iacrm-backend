package com.chatgpt.backend.repository.sp;

import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 * Implementación personalizada de LeadSPRepository
 * SIMPLIFICADO: Usa directamente schema public
 */
@Repository
public class LeadSPRepositoryImpl implements LeadSPRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Obtiene leads de hoy - SIMPLIFICADO usando public schema
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerLeadsHoyRaw() {
        String sql = "SELECT * FROM public.sp_total_leads_hoy()";
        Query query = entityManager.createNativeQuery(sql);
        
        return query.getResultList();
    }

    /**
     * Cuenta leads por categoría - SIMPLIFICADO usando public schema
     */
    public Long contarPorCategoria(String categoria) {
        String sql = "SELECT COUNT(*) FROM public.datos_contacto WHERE categoria = ?";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, categoria);
        
        Object result = query.getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    /**
     * Obtiene total de leads de hoy - SIMPLIFICADO usando public schema
     */
    public Long obtenerTotalLeadsHoy() {
        String sql = "SELECT * FROM public.sp_total_leads_hoy()";
        Query query = entityManager.createNativeQuery(sql);
        
        Object result = query.getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    /**
     * Obtiene leads por estado - SIMPLIFICADO usando public schema
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerLeadsPorEstado() {
        String sql = "SELECT * FROM public.sp_leads_por_estado()";
        Query query = entityManager.createNativeQuery(sql);
        
        return query.getResultList();
    }

    /**
     * Obtiene leads por estado (método raw) - SIMPLIFICADO usando public schema
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerLeadsPorEstadoRaw() {
        return obtenerLeadsPorEstado();
    }
}