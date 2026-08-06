package com.medprep.repository;

import com.medprep.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    Page<Question> findBySpecialtyId(UUID specialtyId, Pageable pageable);

    Page<Question> findBySpecialtyIdAndSubspecialtyId(UUID specialtyId, UUID subspecialtyId, Pageable pageable);

    Page<Question> findByDifficulty(Question.Difficulty difficulty, Pageable pageable);

    @Query("""
            SELECT q FROM Question q
            WHERE (:specialtyId IS NULL OR q.specialty.id = :specialtyId)
            AND (:subspecialtyId IS NULL OR q.subspecialty.id = :subspecialtyId)
            AND (:difficulty IS NULL OR q.difficulty = :difficulty)
            AND (:academy IS NULL OR q.academy = :academy)
            """)
    Page<Question> findWithFilters(
            @Param("specialtyId") UUID specialtyId,
            @Param("subspecialtyId") UUID subspecialtyId,
            @Param("difficulty") Question.Difficulty difficulty,
            @Param("academy") String academy,
            Pageable pageable);

    @Query("SELECT DISTINCT q.academy FROM Question q WHERE q.academy IS NOT NULL AND q.academy <> '' ORDER BY q.academy")
    List<String> findDistinctAcademies();

    @Query("""
            SELECT q FROM Question q
            WHERE q.id IN (
                SELECT a.question.id FROM QuestionAttempt a
                WHERE a.userId = :userId
                AND a.status IN ('INCORRECTA', 'DUDOSA')
                AND a.attemptedAt >= :since
            )
            """)
    Page<Question> findErrorQuestionsByUser(
            @Param("userId") UUID userId,
            @Param("since") java.time.Instant since,
            Pageable pageable);

    @Query(value = """
            SELECT * FROM questions
            WHERE (:specialtyId IS NULL OR specialty_id = CAST(:specialtyId AS uuid))
            ORDER BY RANDOM()
            LIMIT :limit
            """, nativeQuery = true)
    List<Question> findRandomBySpecialty(
            @Param("specialtyId") String specialtyId,
            @Param("limit") int limit);
}
