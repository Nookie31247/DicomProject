package com.allegro.dicomback.repository;
import com.allegro.dicomback.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, String> {

    List<Patient> findByNameContains(String name);

    //최근 검사일 기준 조회
    List<Patient> findByRecentStudyBetween(LocalDateTime start, LocalDateTime end);

    List<Patient> findByNameContainsAndRecentStudyBetween(String name, LocalDateTime start, LocalDateTime end);
}
