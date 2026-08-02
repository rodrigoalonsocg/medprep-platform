package com.medprep.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PatternResponse {
    private UUID questionId;
    private List<String> keywords;
    private String diagnosis;
    private String pearl;
    private List<String> distractors;
}
