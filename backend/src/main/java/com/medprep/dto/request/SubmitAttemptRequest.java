package com.medprep.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class SubmitAttemptRequest {

    @NotNull(message = "El ID de la pregunta es obligatorio")
    private UUID questionId;

    @NotBlank(message = "La opción seleccionada es obligatoria")
    @Pattern(regexp = "[ABCDE]", message = "La opción debe ser A, B, C, D o E")
    private String selectedOption;

    private Integer timeSpentSeconds;
}
