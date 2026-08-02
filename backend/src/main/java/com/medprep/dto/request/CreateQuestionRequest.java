package com.medprep.dto.request;

import com.medprep.model.Question;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateQuestionRequest {

    @NotNull(message = "La especialidad es obligatoria")
    private UUID specialtyId;

    private UUID subspecialtyId;

    @NotBlank(message = "El enunciado es obligatorio")
    private String stem;

    @NotBlank(message = "La opción A es obligatoria")
    private String optionA;

    @NotBlank(message = "La opción B es obligatoria")
    private String optionB;

    private String optionC;
    private String optionD;
    private String optionE;

    @NotBlank(message = "La opción correcta es obligatoria")
    @Pattern(regexp = "[ABCDE]", message = "La opción correcta debe ser A, B, C, D o E")
    private String correctOption;

    private String explanation;

    @NotNull(message = "La dificultad es obligatoria")
    private Question.Difficulty difficulty;

    private String source;
    private Integer year;
    private List<String> keywords;
}
