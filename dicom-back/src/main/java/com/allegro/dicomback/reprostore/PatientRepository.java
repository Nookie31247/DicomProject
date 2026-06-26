package com.allegro.dicomback.reprostore;
import com.allegro.dicomback.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, String> {

    //환자 이름 검색
    List<Patient> findByName(String pName);
    //최근 검사일 기준 조회
    List<Patient> findByPTimeBetween(
            LocalDateTime start,
            LocalDateTime end);

    List<Patient> findByDelFlag(String delFlag);
}
