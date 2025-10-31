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
    // Ejecutar la función que retorna el JSON completo, pasando parámetros como java.sql.Date
    String sql = "SELECT * from public.obtener_reporte_metricas(?, ?)";
    Query query = entityManager.createNativeQuery(sql);
    query.setParameter(1, new java.sql.Date(inicio.getTime()));
    query.setParameter(2, new java.sql.Date(fin.getTime()));
    Object result = query.getSingleResult();
    // El resultado es un String JSON
    return result != null ? result.toString() : "{}";
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