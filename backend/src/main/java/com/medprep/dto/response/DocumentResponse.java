package com.medprep.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {
    private UUID id;
    private UUID academyId;
    private String fileName;
    private Long fileSizeBytes;
    private boolean isPublic;
    private Instant createdAt;
}
