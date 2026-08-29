package com.medisync.specialization.repository;

import com.medisync.specialization.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {
    List<Specialization> findAllByOrderByNameAsc();
    List<Specialization> findByActiveTrueOrderByNameAsc();
    boolean existsByDepartmentIdAndNameIgnoreCase(UUID departmentId, String name);
    boolean existsByDepartmentIdAndNameIgnoreCaseAndIdNot(UUID departmentId, String name, UUID id);

    @Query("""
            select specialization from Specialization specialization
            where specialization.active = true
              and specialization.departmentId = :departmentId
            order by specialization.name asc
            """)
    List<Specialization> findActiveForDepartment(@Param("departmentId") UUID departmentId);
}
