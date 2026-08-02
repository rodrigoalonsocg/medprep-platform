package com.medprep.dto.response;

import com.medprep.model.UserSpecialtyProgress;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ProgressResponse {
    private UUID specialtyId;
    private String specialtyName;
    private int totalAttempts;
    private int correctAttempts;
    private BigDecimal accuracyPercentage;
    private UserSpecialtyProgress.TrafficLight trafficLight;
    private Instant lastUpdated;
    private boolean sprintRequired;
}
