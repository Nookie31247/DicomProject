package com.allegro.dicomback.repository;
import com.allegro.dicomback.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, String> {

    List<Patient> findByDoctorKeyAndNameContaining(Long doctorKey, String name);

    //최근 검사일 기준 조회
    List<Patient> findByDoctorKeyAndRecentStudyBetween(Long doctorKey, LocalDateTime start, LocalDateTime end);

    List<Patient> findByDoctorKeyAndNameContainingAndRecentStudyBetween(Long doctorKey, String name, LocalDateTime start, LocalDateTime end);

    Patient findByKey(Long key);
}
