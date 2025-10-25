package com.chatgpt.backend.repository.sp;

import java.util.List;

/**
 * Interface personalizada para métodos dinámicos de LeadSPRepository
 */
public interface LeadSPRepositoryCustom {
    
    /**
     * Obtiene leads de hoy usando el esquema configurado
     */
    List<Object[]> obtenerLeadsHoyRaw();
    
    /**
     * Obtiene total de leads de hoy usando el esquema configurado
     */
    Long obtenerTotalLeadsHoy();
    
    /**
     * Obtiene leads por estado usando el esquema configurado
     */
    List<Object[]> obtenerLeadsPorEstado();
}