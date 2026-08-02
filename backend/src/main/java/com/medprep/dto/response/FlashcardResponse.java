package com.medprep.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class FlashcardResponse {
    private UUID id;
    private UUID questionId;
    private String front;
    private String back;
    private String specialtyName;
    private boolean exported;
    private Instant createdAt;
}
