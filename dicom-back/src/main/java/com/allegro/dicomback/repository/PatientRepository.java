package com.allegro.dicomback.repository;
import com.allegro.dicomback.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    // 검사 기간으로 검색
    List<Patient> findByDoctorKey_KeyAndRecentStudyBetween(Long doctorKey, LocalDateTime start, LocalDateTime end);

    // 검사 기간과 이름 검색어로 검색
    List<Patient> findByDoctorKey_KeyAndNameContainingAndRecentStudyBetween(Long doctorKey, String name, LocalDateTime start, LocalDateTime end);

    // 환자 키 리스트 기반으로 검색하여 hiddenFlag를 변경함
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
    """
        update Patient p
        set p.hiddenFlag = :isHidden
        where p.doctorKey.key = :doctorKey
            and p.key in :patientKeys
    """)
    int changeHiddenFlag(
            @Param("doctorKey") Long doctorKey,
            @Param("patientKeys") List<Long> patientKeys,
            @Param("isHidden") boolean isHidden
    );

    // 특정 의사에게 등록된 환자
    // 검색어가 있으면 name 조건 걸고, 없으면 조건x
    // 최근 검사일이 시작일과 종료일 안에 들어갔거나, 아예 존재하지 않는 경우
    @Query("""
        select p from Patient p
        where p.doctorKey.key = :doctorKey
          and (:name is null or p.name like %:name%)
          and (p.recentStudy is null or p.recentStudy between :start and :end)
    """)
    List<Patient> findByDoctorKeyWithOptionalRecentStudy(
            @Param("doctorKey") Long doctorKey,
            @Param("name") String name,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}

