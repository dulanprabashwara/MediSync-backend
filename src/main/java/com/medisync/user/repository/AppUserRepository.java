package com.medisync.user.repository;

import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID>, JpaSpecificationExecutor<AppUser> {
    Optional<AppUser> findByAuthUserId(UUID authUserId);
    boolean existsByAuthUserId(UUID authUserId);
    List<AppUser> findByRoleAndStatus(UserRole role, AccountStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from AppUser user where user.id = :id")
    Optional<AppUser> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from AppUser user where user.authUserId = :authUserId")
    Optional<AppUser> findByAuthUserIdForUpdate(@Param("authUserId") UUID authUserId);
}
