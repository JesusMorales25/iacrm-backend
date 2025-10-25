package com.chatgpt.backend.repository;

import com.chatgpt.backend.persistence.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversacionRepository extends JpaRepository<Conversacion, Long>, ConversacionRepositoryCustom {
}
