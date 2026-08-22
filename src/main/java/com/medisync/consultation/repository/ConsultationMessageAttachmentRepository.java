package com.medisync.consultation.repository;

import com.medisync.consultation.entity.ConsultationMessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ConsultationMessageAttachmentRepository
        extends JpaRepository<ConsultationMessageAttachment, UUID> {
    List<ConsultationMessageAttachment> findByMessageIdInOrderByMessageIdAscPositionAsc(Collection<UUID> messageIds);
}
