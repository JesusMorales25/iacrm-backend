package com.chatgpt.backend.repository.sp;

import com.chatgpt.backend.dto.LeadDTO;
import com.chatgpt.backend.dto.TotalLeadsHoyDTO;
import com.chatgpt.backend.persistence.DatosContacto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface LeadSPRepository extends JpaRepository<DatosContacto, Long> {

    @Query(value = "SELECT * FROM iacrm.sp_leads_hoy()", nativeQuery = true)
    List<Object[]> obtenerLeadsHoyRaw();

    @Query(value = "SELECT total_leads_hoy FROM iacrm.sp_total_leads_hoy()", nativeQuery = true)
    Long obtenerTotalLeadsHoy();

    @Query(value = "SELECT * FROM iacrm.sp_leads_por_estado()", nativeQuery = true)
    List<Object[]> obtenerLeadsPorEstado();
}


/*
@Repository
public class LeadSPRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<LeadDTO> obtenerLeadsHoy() {
        // Ahora usamos SELECT en lugar de CALL
        List<Object[]> resultados = entityManager
                .createNativeQuery("SELECT * FROM rinomix.sp_leads_hoy()")
                .getResultList();

        List<LeadDTO> leads = new ArrayList<>();
        for (Object[] fila : resultados) {
            leads.add(new LeadDTO(
                    (String) fila[0],
                    (String) fila[1],
                    (String) fila[2],
                    (String) fila[3],
                    ((Timestamp) fila[4]).toLocalDateTime()
            ));
        }
        return leads;
    }

    public Long obtenerTotalLeadsHoy() {
        Object resultado = entityManager
                .createNativeQuery("SELECT total_leads_hoy FROM rinomix.sp_total_leads_hoy()")
                .getSingleResult();

        return ((Number) resultado).longValue();
    }

    public List<Object[]> obtenerLeadsPorEstadoRaw() {
        return entityManager
                .createNativeQuery("SELECT * FROM rinomix.sp_leads_por_estado()")
                .getResultList();
    }
}*/
