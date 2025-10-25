package com.chatgpt.backend.repository;

import java.util.List;

/**
 * Interface personalizada para métodos dinámicos de DatosContactoRepository
 */
public interface DatosContactoRepositoryCustom {
    
    /**
     * Obtiene leads usando el esquema configurado
     */
    List<Object[]> obtenerLeads();
}