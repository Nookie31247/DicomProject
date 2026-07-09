package com.allegro.backanonymization.repository;

import com.allegro.backanonymization.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Study 엔티티를 위한 레포지토리 인터페이스입니다.
 */
public interface StudyRepository extends JpaRepository<Study, Long> {
    /**
     * 시작일과 종료일 사이에 생성된 연구를 찾습니다.
     *
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @return 연구 목록
     */
    List<Study> findStudiesByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 검색 매개변수를 사용하여 리서치용 연구를 찾습니다.
     *
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @param search 검색 문자열
     * @return 연구 목록
     */
    @Query("""
        select s
        from Study s
        where s.createdAt between :start and :end
          and (
            :search is null
            or :search = ''
            or lower(coalesce(s.description, '')) like lower(concat('%', :search, '%'))
          )
        order by s.createdAt desc
        """)
    List<Study> findStudiesForResearch(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("search") String search
    );

    /**
     * UID로 연구가 존재하는지 확인합니다.
     *
     * @param uid UID
     * @return 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByUid(String uid);
}
