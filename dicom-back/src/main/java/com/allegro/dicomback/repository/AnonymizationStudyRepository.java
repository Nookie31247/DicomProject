package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.anonymization.AnonymizationStudy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnonymizationStudyRepository extends JpaRepository<AnonymizationStudy, String> {

    Optional<AnonymizationStudy> findByStudyStudyKey(String sopInstanceUID);

    Optional<AnonymizationStudy> findByAnonPId(String anonPId);


}
