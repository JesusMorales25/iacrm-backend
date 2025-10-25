package com.chatgpt.backend.repository;

import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

/**
 * Implementación personalizada de DatosContactoRepository
 * SIMPLIFICADO: Usa directamente schema public
 */
@Repository
public class DatosContactoRepositoryImpl implements DatosContactoRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Obtiene leads usando esquema public simplificado
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerLeads() {
        String sql = "SELECT id, nombre, telefono, correo, categoria, thread_id FROM public.datos_contacto ORDER BY id DESC";
        Query query = entityManager.createNativeQuery(sql);
        
        return query.getResultList();
    }

    /**
     * Obtiene leads usando esquema public simplificado (método raw)
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerLeadsRaw() {
        return obtenerLeads();
    }

    /**
     * Cuenta total de contactos por categoría
     */
    public Long countByCategoria(String categoria) {
        String sql = "SELECT COUNT(*) FROM public.datos_contacto WHERE categoria = ?";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, categoria);
        
        Object result = query.getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }
}