package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.anonymization.AnonymizationStudy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnonymizationStudyRepository extends JpaRepository<AnonymizationStudy, Long> {

    Optional<AnonymizationStudy> findByStudy_StudyKey(Long studyKey);

    Optional<AnonymizationStudy> findByAnonPId(String anonPId);


}
