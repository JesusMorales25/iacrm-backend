package com.chatgpt.backend.repository.sp;

import com.chatgpt.backend.persistence.DatosContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadSPRepository extends JpaRepository<DatosContacto, Long>, LeadSPRepositoryCustom {
}
