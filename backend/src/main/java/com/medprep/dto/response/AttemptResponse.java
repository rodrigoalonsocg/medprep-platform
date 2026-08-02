package com.medprep.dto.response;

import com.medprep.model.QuestionAttempt;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AttemptResponse {
    private UUID id;
    private UUID questionId;
    private String selectedOption;
    private String correctOption;
    private boolean correct;
    private QuestionAttempt.AttemptStatus status;
    private String explanation;
    private Instant attemptedAt;
}
