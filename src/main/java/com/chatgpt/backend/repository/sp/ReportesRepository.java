package com.chatgpt.backend.repository.sp;

import com.chatgpt.backend.persistence.DatosContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReportesRepository extends JpaRepository<DatosContacto, Long> {

    @Query(value = "SELECT iacrm.obtener_reporte_metricas(:inicio, :fin)", nativeQuery = true)
    String obtenerReporte(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);
}
