package com.medprep.dto.response;

import com.medprep.model.Question;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class QuestionResponse {
    private UUID id;
    private UUID specialtyId;
    private String specialtyName;
    private UUID subspecialtyId;
    private String subspecialtyName;
    private String stem;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String optionE;
    private String explanation;
    private Question.Difficulty difficulty;
    private String source;
    private Integer year;
    private List<String> keywords;
    private Instant createdAt;
}
