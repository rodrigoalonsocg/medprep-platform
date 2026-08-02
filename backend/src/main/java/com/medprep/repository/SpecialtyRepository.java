package com.medprep.repository;

import com.medprep.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpecialtyRepository extends JpaRepository<Specialty, UUID> {
    Optional<Specialty> findByCode(String code);
    boolean existsByCode(String code);
}
