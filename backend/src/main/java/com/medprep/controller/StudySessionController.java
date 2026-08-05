package com.medprep.controller;

import com.medprep.dto.request.CreateStudySessionRequest;
import com.medprep.dto.response.ApiResponse;
import com.medprep.dto.response.StudySessionResponse;
import com.medprep.service.StudySessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/study-sessions")
@RequiredArgsConstructor
@Tag(name = "Workspace", description = "Sesiones de estudio (Pomodoro / libre)")
public class StudySessionController {

    private final StudySessionService studySessionService;

    @PostMapping
    @Operation(summary = "Registrar una sesión de estudio")
    public ResponseEntity<ApiResponse<StudySessionResponse>> create(
            @Valid @RequestBody CreateStudySessionRequest req,
            @AuthenticationPrincipal String userId) {
        StudySessionResponse response = studySessionService.create(req, UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Sesión registrada"));
    }

    @GetMapping
    @Operation(summary = "Listar las sesiones de estudio del usuario")
    public ApiResponse<List<StudySessionResponse>> list(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(studySessionService.listForUser(UUID.fromString(userId)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Minutos estudiados en los últimos 7 días")
    public ApiResponse<Map<String, Long>> stats(@AuthenticationPrincipal String userId) {
        long minutes = studySessionService.minutesThisWeek(UUID.fromString(userId));
        return ApiResponse.ok(Map.of("minutesThisWeek", minutes));
    }
}
