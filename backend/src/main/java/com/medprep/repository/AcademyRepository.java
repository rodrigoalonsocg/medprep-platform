package com.medprep.repository;

import com.medprep.model.Academy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademyRepository extends JpaRepository<Academy, UUID> {
    List<Academy> findAllByOrderByNameAsc();
}
