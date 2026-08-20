package com.medisync.department.repository;

import com.medisync.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findAllByOrderByNameAsc();
    List<Department> findByHospitalIdAndActiveTrueOrderByNameAsc(UUID hospitalId);
    boolean existsByHospitalIdAndNameIgnoreCase(UUID hospitalId, String name);
    boolean existsByHospitalIdAndNameIgnoreCaseAndIdNot(UUID hospitalId, String name, UUID id);
}

