package com.chatgpt.backend.repository.sp;

import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 * Implementación personalizada de ReportesRepository
 * SIMPLIFICADO: Usa directamente schema public
 */
@Repository
public class ReportesRepositoryImpl implements ReportesRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Obtiene reporte de métricas - SIMPLIFICADO usando public schema
     */
    public String obtenerReporte(java.sql.Timestamp inicio, java.sql.Timestamp fin) {
    // Reporte básico con estadísticas del período
    String sql = "SELECT COUNT(*) as total FROM public.conversacion WHERE fecha_hora BETWEEN ? AND ?";
    Query query = entityManager.createNativeQuery(sql);
    query.setParameter(1, inicio);
    query.setParameter(2, fin);

    Object result = query.getSingleResult();
    Long total = result != null ? ((Number) result).longValue() : 0L;

    // Retornar un JSON válido
    return String.format("{\"total\":%d,\"inicio\":\"%s\",\"fin\":\"%s\"}",
        total, inicio.toString(), fin.toString());
    }

    /**
     * Obtiene estadísticas generales - SIMPLIFICADO usando public schema
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerEstadisticasGenerales() {
        String sql = "SELECT * FROM public.sp_estadisticas_generales()";
        Query query = entityManager.createNativeQuery(sql);
        
        return query.getResultList();
    }

    /**
     * Obtiene conversaciones del último mes
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerConversacionesUltimoMes() {
        String sql = "SELECT * FROM public.sp_conversaciones_ultimo_mes()";
        Query query = entityManager.createNativeQuery(sql);
        
        return query.getResultList();
    }

    /**
     * Obtiene leads del último mes
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerLeadsUltimoMes() {
        String sql = "SELECT * FROM public.sp_leads_ultimo_mes()";
        Query query = entityManager.createNativeQuery(sql);
        
        return query.getResultList();
    }
}