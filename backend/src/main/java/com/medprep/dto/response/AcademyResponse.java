package com.medprep.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AcademyResponse {
    private UUID id;
    private String name;
    private String description;
    private Instant createdAt;
}
