package com.medprep.dto.request;

import com.medprep.model.StudySession;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateStudySessionRequest {

    private UUID specialtyId;

    @NotNull(message = "La duración es obligatoria")
    @Min(value = 1, message = "La duración debe ser al menos 1 minuto")
    private Integer durationMinutes;

    @NotNull(message = "El tipo de sesión es obligatorio")
    private StudySession.SessionType sessionType;

    private Instant startedAt;
    private Instant endedAt;
}
