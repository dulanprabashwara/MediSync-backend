package com.medisync.consultation.repository;

import com.medisync.consultation.entity.ConsultationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsultationMessageRepository extends JpaRepository<ConsultationMessage, UUID> {
    Page<ConsultationMessage> findByConsultationIdOrderBySentAtDescIdDesc(UUID consultationId, Pageable pageable);
}
