package com.allegro.dicomback.repository;
import com.allegro.dicomback.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, String> {

    //환자 이름 검색
    List<Patient> findBypName(String pName);
    //최근 검사일 기준 조회
    List<Patient> findBypTimeBetween(
            LocalDateTime start,
            LocalDateTime end);

    List<Patient> findByDelFlag(Integer delFlag);
}
