package com.chatgpt.backend.repository.sp;

import com.chatgpt.backend.persistence.DatosContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public interface ReportesRepository extends JpaRepository<DatosContacto, Long>, ReportesRepositoryCustom {
}
