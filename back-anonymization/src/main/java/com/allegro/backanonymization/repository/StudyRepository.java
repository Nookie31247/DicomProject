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
    boolean existsByUid(String uid);
}
