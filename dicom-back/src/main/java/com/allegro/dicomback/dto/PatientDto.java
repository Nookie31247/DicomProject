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
                patient.getId(),
                patient.getName(),
                patient.getBirth() != null ? patient.getBirth().toString() : null,
                patient.getSex(),
                patient.getRecentStudy() != null ? patient.getRecentStudy().toString() : null,
                patient.getStudyCount()
        );
    }
}