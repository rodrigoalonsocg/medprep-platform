package com.medprep.repository;

import com.medprep.model.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
    List<Flashcard> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Flashcard> findByUserIdAndExportedFalse(UUID userId);
}
