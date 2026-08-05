package com.medprep.repository;

import com.medprep.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    List<StudySession> findByUserIdOrderByStartedAtDesc(UUID userId);

    @Query("""
            SELECT COALESCE(SUM(s.durationMinutes), 0) FROM StudySession s
            WHERE s.userId = :userId AND s.startedAt >= :since
            """)
    long sumMinutesSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
