package com.medisync.appointment.dto;

import com.medisync.appointment.entity.AppointmentSymptoms;

public record AppointmentSymptomsResponse(
        String reasonForVisit,
        String symptoms,
        String symptomDuration,
        String additionalNotes
) {
    public static AppointmentSymptomsResponse from(AppointmentSymptoms symptoms) {
        return new AppointmentSymptomsResponse(symptoms.getReasonForVisit(), symptoms.getSymptoms(),
                symptoms.getSymptomDuration(), symptoms.getAdditionalNotes());
    }
}
