package com.medisync.user.repository;

import com.medisync.user.entity.PharmacistProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PharmacistProfileRepository extends JpaRepository<PharmacistProfile, UUID> {
}
