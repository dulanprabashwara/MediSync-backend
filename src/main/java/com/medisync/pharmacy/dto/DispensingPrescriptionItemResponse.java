package com.medisync.pharmacy.dto;

import com.medisync.prescription.entity.PrescriptionItem;

public record DispensingPrescriptionItemResponse(
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
    public static DispensingPrescriptionItemResponse from(PrescriptionItem item) {
        return new DispensingPrescriptionItemResponse(item.getPosition(), item.getMedicineName(), item.getStrength(),
                item.getMedicineForm(), item.getDosage(), item.getFrequency(), item.getDuration(), item.getQuantity(),
                item.getRoute(), item.getInstructions());
    }
}
