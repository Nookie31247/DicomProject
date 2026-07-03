package com.allegro.dicomback.dto;

import com.allegro.dicomback.entity.Patient;

public record PatientDto(
        String pId,
        String pName,
        String pBirth,
        String pSex,
        String pTime,
        Integer pStudyCount
) {
    public static PatientDto fromEntity(Patient patient) {
        return new PatientDto(
                patient.getPId(),
                patient.getPName(),
                patient.getPBirth() != null ? patient.getPBirth().toString() : null,
                patient.getPSex(),
                patient.getPTime() != null ? patient.getPTime().toString() : null,
                patient.getPStudyCount()
        );
    }
}