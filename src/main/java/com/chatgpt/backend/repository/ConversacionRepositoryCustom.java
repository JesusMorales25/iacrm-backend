package com.chatgpt.backend.repository;

import java.util.List;

/**
 * Interface personalizada para métodos dinámicos de ConversacionRepository
 */
public interface ConversacionRepositoryCustom {
    
    /**
     * Cuenta las conversaciones de hoy usando el esquema configurado
     */
    Long contarConversacionesHoy();
    
    /**
     * Obtiene conversaciones por día usando el esquema configurado
     */
    List<Object[]> obtenerConversacionesPorDiaRaw();
    
    /**
     * Obtiene tiempo promedio de respuesta usando el esquema configurado
     */
    Double obtenerTiempoPromedioRespuestaRaw();
    
    /**
     * Obtiene total de contactos usando el esquema configurado
     */
    Long obtenerTotalContactos();
    
    /**
     * Cuenta total de mensajes por número de usuario
     */
    Long countByNumero(String numero);
}