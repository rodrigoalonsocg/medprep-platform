package com.medprep.dto.response;

import com.medprep.model.StudySession;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class StudySessionResponse {
    private UUID id;
    private UUID specialtyId;
    private String specialtyName;
    private Integer durationMinutes;
    private StudySession.SessionType sessionType;
    private Instant startedAt;
    private Instant endedAt;
}
