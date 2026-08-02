package com.medprep.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subspecialty_id")
    private Subspecialty subspecialty;

    @Column(nullable = false, columnDefinition = "text")
    private String stem;

    @Column(name = "option_a", columnDefinition = "text")
    private String optionA;

    @Column(name = "option_b", columnDefinition = "text")
    private String optionB;

    @Column(name = "option_c", columnDefinition = "text")
    private String optionC;

    @Column(name = "option_d", columnDefinition = "text")
    private String optionD;

    @Column(name = "option_e", columnDefinition = "text")
    private String optionE;

    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption;

    @Column(columnDefinition = "text")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    private String source;

    private Integer year;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "keywords", columnDefinition = "text[]")
    private List<String> keywords;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public enum Difficulty {
        BAJA, MEDIA, ALTA
    }
}
