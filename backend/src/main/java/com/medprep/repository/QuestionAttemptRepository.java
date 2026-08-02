package com.medprep.repository;

import com.medprep.model.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, UUID> {

    List<QuestionAttempt> findByUserIdOrderByAttemptedAtDesc(UUID userId);

    @Query("""
            SELECT a FROM QuestionAttempt a
            WHERE a.userId = :userId
            AND a.question.specialty.id = :specialtyId
            ORDER BY a.attemptedAt DESC
            """)
    List<QuestionAttempt> findByUserAndSpecialty(
            @Param("userId") UUID userId,
            @Param("specialtyId") UUID specialtyId);

    @Query("""
            SELECT COUNT(a) FROM QuestionAttempt a
            WHERE a.userId = :userId
            AND a.question.specialty.id = :specialtyId
            AND a.correct = true
            """)
    long countCorrectByUserAndSpecialty(
            @Param("userId") UUID userId,
            @Param("specialtyId") UUID specialtyId);

    @Query("""
            SELECT COUNT(a) FROM QuestionAttempt a
            WHERE a.userId = :userId
            AND a.question.specialty.id = :specialtyId
            """)
    long countTotalByUserAndSpecialty(
            @Param("userId") UUID userId,
            @Param("specialtyId") UUID specialtyId);
}
