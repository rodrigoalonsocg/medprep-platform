package com.medprep.controller;

import com.medprep.dto.response.ApiResponse;
import com.medprep.dto.response.ProgressResponse;
import com.medprep.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@Tag(name = "Mi Progreso", description = "Progreso y analítica por especialidad")
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping
    @Operation(summary = "Obtener progreso por todas las especialidades del usuario")
    public ApiResponse<List<ProgressResponse>> getProgress(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(progressService.getUserProgress(UUID.fromString(userId)));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Especialidades en rojo que requieren Sprint de Recuperación")
    public ApiResponse<List<ProgressResponse>> getAlerts(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(progressService.getRedAlerts(UUID.fromString(userId)));
    }
}
