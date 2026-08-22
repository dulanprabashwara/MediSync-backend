package com.medisync.user.repository;

import com.medisync.user.entity.UserAccountBan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountBanRepository extends JpaRepository<UserAccountBan, UUID> {
    Optional<UserAccountBan> findByUserIdAndUnbannedAtIsNull(UUID userId);
    List<UserAccountBan> findByUserIdOrderByBannedAtDesc(UUID userId);
}
