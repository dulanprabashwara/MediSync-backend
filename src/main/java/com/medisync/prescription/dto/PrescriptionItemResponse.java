package com.medisync.prescription.dto;

import com.medisync.prescription.entity.PrescriptionItem;

import java.util.UUID;

public record PrescriptionItemResponse(
        UUID id,
        int position,
        String medicineName,
        String strength,
        String medicineForm,
        String dosage,
        String frequency,
        String duration,
        String quantity,
        String route,
        String instructions
) {
    public static PrescriptionItemResponse from(PrescriptionItem item) {
        return new PrescriptionItemResponse(item.getId(), item.getPosition(), item.getMedicineName(),
                item.getStrength(), item.getMedicineForm(), item.getDosage(), item.getFrequency(),
                item.getDuration(), item.getQuantity(), item.getRoute(), item.getInstructions());
    }
}
