package com.chatgpt.backend.repository;

import com.chatgpt.backend.persistence.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    @Query(value = "SELECT * FROM iacrm.sp_conversaciones_hoy()", nativeQuery = true)
    Long contarConversacionesHoy();

    @Query(value = "SELECT * FROM iacrm.sp_conversaciones_por_dia()", nativeQuery = true)
    List<Object[]> obtenerConversacionesPorDiaRaw();

    @Query(value = "SELECT * FROM iacrm.sp_tiempo_promedio_respuesta()", nativeQuery = true)
    Double obtenerTiempoPromedioRespuestaRaw();

    @Query(value = "SELECT * FROM iacrm.sp_total_contactos()", nativeQuery = true)
    Long obtenerTotalContactos();
}



/*
@Repository
public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    @Query(value = "SELECT COUNT(DISTINCT c.thread_id) FROM conversacion c WHERE DATE(c.fecha_hora) = CURDATE()", nativeQuery = true)
    Long contarConversacionesHoy();

    @Query(value = """
        SELECT 
            DATE(c.fecha_hora) AS dia,
            COUNT(DISTINCT c.thread_id) AS totalConversaciones
        FROM conversacion c
        WHERE c.fecha_hora >= CURDATE() - INTERVAL 7 DAY
        GROUP BY dia
        ORDER BY dia DESC
        """, nativeQuery = true)
    List<Object[]> obtenerConversacionesPorDiaRaw();

*/
