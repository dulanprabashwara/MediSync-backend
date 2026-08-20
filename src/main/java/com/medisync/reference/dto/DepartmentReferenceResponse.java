package com.medisync.reference.dto;

import java.util.UUID;

public record DepartmentReferenceResponse(UUID id, UUID hospitalId, String name) {
}

