package com.medprep.repository;

import com.medprep.model.ClinicalPattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClinicalPatternRepository extends JpaRepository<ClinicalPattern, UUID> {
    Optional<ClinicalPattern> findByQuestionId(UUID questionId);
}
