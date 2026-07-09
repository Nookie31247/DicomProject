package com.allegro.backanonymization.repository;

import com.allegro.backanonymization.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StudyRepository extends JpaRepository<Study, Long> {
    List<Study> findStudiesByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

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

    boolean existsByUid(String uid);
}
