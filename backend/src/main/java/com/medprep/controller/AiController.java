package com.medprep.controller;

import com.medprep.dto.response.ApiResponse;
import com.medprep.dto.response.FlashcardResponse;
import com.medprep.dto.response.PatternResponse;
import com.medprep.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "IA", description = "Análisis de patrones clínicos y generación de flashcards")
public class AiController {

    private final AiService aiService;

    @PostMapping("/analyze/{questionId}")
    @Operation(summary = "Modo Patrón: extrae keywords, diagnóstico y perla médica de una pregunta")
    public CompletableFuture<ApiResponse<PatternResponse>> analyzePattern(
            @PathVariable UUID questionId) {
        return aiService.analyzePattern(questionId)
                .thenApply(ApiResponse::ok);
    }

    @PostMapping("/flashcards/{questionId}")
    @Operation(summary = "Genera flashcards IA para una pregunta y las guarda en la biblioteca del usuario")
    public CompletableFuture<ApiResponse<List<FlashcardResponse>>> generateFlashcards(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal String userId) {
        return aiService.generateFlashcards(questionId, UUID.fromString(userId))
                .thenApply(ApiResponse::ok);
    }

    @GetMapping("/flashcards")
    @Operation(summary = "Listar todas las flashcards del usuario")
    public ApiResponse<List<FlashcardResponse>> getFlashcards(
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(aiService.getUserFlashcards(UUID.fromString(userId)));
    }

    @GetMapping("/flashcards/export")
    @Operation(summary = "Exportar flashcards pendientes en formato Anki (texto plano separado por ;)")
    public ResponseEntity<String> exportToAnki(@AuthenticationPrincipal String userId) {
        String content = aiService.exportToAnki(UUID.fromString(userId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"flashcards-medprep.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }
}
