package com.medisync.specialization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "specializations")
public class Specialization {

    @Id
    private UUID id;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Specialization() {
    }

    public Specialization(String name, String description, boolean active) {
        this(null, name, description, active);
    }

    public Specialization(UUID departmentId, String name, String description, boolean active) {
        this.id = UUID.randomUUID();
        update(departmentId, name, description, active);
    }

    public void update(String name, String description, boolean active) {
        update(departmentId, name, description, active);
    }

    public void update(UUID departmentId, String name, String description, boolean active) {
        this.departmentId = departmentId;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public UUID getDepartmentId() { return departmentId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
