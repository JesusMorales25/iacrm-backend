package com.chatgpt.backend.repository;

import com.chatgpt.backend.persistence.DatosContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatosContactoRepository extends JpaRepository<DatosContacto, Long> {
    Optional<DatosContacto> findByThreadId(String threadId);
    boolean existsByNumeroUsuario(String numeroUsuario);

    @Query(value = "SELECT * FROM iacrm.obtener_leads()", nativeQuery = true)
    List<Object[]> obtenerLeads();
}

