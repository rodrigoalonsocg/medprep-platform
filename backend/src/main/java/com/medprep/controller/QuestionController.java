package com.medprep.controller;

import com.medprep.dto.request.CreateQuestionRequest;
import com.medprep.dto.request.SubmitAttemptRequest;
import com.medprep.dto.response.ApiResponse;
import com.medprep.dto.response.AttemptResponse;
import com.medprep.dto.response.ExamImportResponse;
import com.medprep.dto.response.QuestionResponse;
import com.medprep.model.Question;
import com.medprep.service.AiService;
import com.medprep.service.PdfService;
import com.medprep.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "Q-Bank", description = "Banco de preguntas")
public class QuestionController {

    private final QuestionService questionService;
    private final AiService aiService;
    private final PdfService pdfService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear pregunta (solo admin)")
    public ResponseEntity<ApiResponse<QuestionResponse>> create(
            @Valid @RequestBody CreateQuestionRequest req,
            @AuthenticationPrincipal String userId) {
        QuestionResponse response = questionService.createQuestion(req, UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Pregunta creada exitosamente"));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importar examen (PDF): la IA detecta y clasifica las preguntas por especialidad (solo admin)")
    public ApiResponse<ExamImportResponse> importExam(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "academy", required = false) String academy,
            @AuthenticationPrincipal String userId) {
        String text = pdfService.extractText(file);
        pdfService.storeBestEffort(file);
        ExamImportResponse result = aiService.importFromExam(text, UUID.fromString(userId), academy);
        return ApiResponse.ok(result, "Se importaron " + result.getImported() + " preguntas");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar pregunta (solo admin)")
    public ApiResponse<String> delete(@PathVariable UUID id) {
        questionService.deleteQuestion(id);
        return ApiResponse.ok("Pregunta eliminada");
    }

    @GetMapping
    @Operation(summary = "Listar preguntas con filtros opcionales")
    public ApiResponse<Page<QuestionResponse>> list(
            @RequestParam(required = false) UUID specialtyId,
            @RequestParam(required = false) UUID subspecialtyId,
            @RequestParam(required = false) Question.Difficulty difficulty,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(questionService.findQuestions(specialtyId, subspecialtyId, difficulty, pageable));
    }

    @GetMapping("/errors")
    @Operation(summary = "Filtro de Erre: preguntas falladas/dudosas de las últimas 3 semanas")
    public ApiResponse<Page<QuestionResponse>> getErrorQuestions(
            @AuthenticationPrincipal String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(questionService.findErrorQuestions(UUID.fromString(userId), pageable));
    }

    @GetMapping("/simulacro")
    @Operation(summary = "Generar simulacro aleatorio")
    public ApiResponse<List<QuestionResponse>> simulacro(
            @RequestParam(required = false) UUID specialtyId,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(questionService.generateSimulacro(specialtyId, limit));
    }

    @PostMapping("/attempt")
    @Operation(summary = "Registrar intento de respuesta")
    public ApiResponse<AttemptResponse> submitAttempt(
            @Valid @RequestBody SubmitAttemptRequest req,
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(questionService.submitAttempt(req, UUID.fromString(userId)));
    }
}
