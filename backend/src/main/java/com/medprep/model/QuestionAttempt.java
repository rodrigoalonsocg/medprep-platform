package com.medprep.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "question_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "selected_option", length = 1)
    private String selectedOption;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    @Column(name = "attempted_at", updatable = false)
    private Instant attemptedAt;

    @PrePersist
    void prePersist() {
        attemptedAt = Instant.now();
    }

    public enum AttemptStatus {
        CORRECTA, INCORRECTA, DUDOSA
    }
}
