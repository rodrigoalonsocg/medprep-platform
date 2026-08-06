package com.medprep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** Resultado de importar un examen con IA: cuántas preguntas se guardaron y cuáles. */
@Data
@AllArgsConstructor
public class ExamImportResponse {
    private int imported;
    private List<QuestionResponse> questions;
}
