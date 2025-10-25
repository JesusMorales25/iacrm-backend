package com.chatgpt.backend.repository.sp;

import java.sql.Timestamp;

/**
 * Interface personalizada para métodos dinámicos de ReportesRepository
 */
public interface ReportesRepositoryCustom {
    
    /**
     * Obtiene reporte de métricas usando el esquema configurado
     */
    String obtenerReporte(Timestamp inicio, Timestamp fin);
}