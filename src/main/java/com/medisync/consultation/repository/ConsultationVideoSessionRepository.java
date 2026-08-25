package com.medisync.consultation.repository;

import com.medisync.consultation.entity.ConsultationVideoSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationVideoSessionRepository extends JpaRepository<ConsultationVideoSession, UUID> {
    Optional<ConsultationVideoSession> findByConsultationId(UUID consultationId);
}
