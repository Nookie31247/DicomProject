package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link Patient} 엔티티를 위한 레포지토리 인터페이스입니다.
 * 표준 CRUD 및 사용자 정의 쿼리 작업을 제공하기 위해 JpaRepository를 확장합니다.
 */
public interface PatientRepository extends JpaRepository<Patient, Long> {
    /**
     * 특정 의사에게 할당된 여러 환자의 숨김 플래그를 업데이트합니다.
     *
     * @param doctorKey 의사 키
     * @param patientKeys 업데이트할 환자 키 목록
     * @param isHidden 새로운 숨김 플래그 상태
     * @return 업데이트된 레코드 수
     */
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

    /**
     * 특정 의사에게 등록된 환자를 검색합니다.
     * 제공된 경우 이름이 포함되었는지 선택적으로 필터링하고, 최근 검사 날짜로 필터링합니다
     * (범위 내에 있거나 환자의 최근 검사가 없는 경우).
     *
     * @param doctorKey 의사 키
     * @param name 필터링할 이름 (선택 사항)
     * @param start 범위의 시작 날짜
     * @param end 범위의 종료 날짜
     * @return 조건과 일치하는 {@link Patient} 목록
     */
    @Query("""
        select p from Patient p
        where p.doctorKey.key = :doctorKey
          and (:name is null or p.name like %:name%)
          and (p.recentStudy is null or p.recentStudy between :start and :end)
        order by p.recentStudy desc
    """)
    List<Patient> findByDoctorKeyWithOptionalRecentStudy(
            @Param("doctorKey") Long doctorKey,
            @Param("name") String name,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
