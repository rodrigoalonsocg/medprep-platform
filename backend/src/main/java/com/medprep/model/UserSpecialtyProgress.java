package com.medprep.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_specialty_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "specialty_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSpecialtyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @Column(name = "total_attempts")
    @Builder.Default
    private int totalAttempts = 0;

    @Column(name = "correct_attempts")
    @Builder.Default
    private int correctAttempts = 0;

    @Column(name = "accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal accuracyPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "traffic_light")
    private TrafficLight trafficLight;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    public void recalculate() {
        if (totalAttempts == 0) {
            accuracyPercentage = BigDecimal.ZERO;
            trafficLight = TrafficLight.ROJO;
        } else {
            double pct = (double) correctAttempts / totalAttempts * 100;
            accuracyPercentage = BigDecimal.valueOf(pct).setScale(2, java.math.RoundingMode.HALF_UP);
            if (pct > 75) trafficLight = TrafficLight.VERDE;
            else if (pct >= 60) trafficLight = TrafficLight.AMARILLO;
            else trafficLight = TrafficLight.ROJO;
        }
        lastUpdated = Instant.now();
    }

    public enum TrafficLight {
        VERDE, AMARILLO, ROJO
    }
}
