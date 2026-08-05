package com.medprep.controller;

import com.medprep.dto.request.CreateAcademyRequest;
import com.medprep.dto.response.AcademyResponse;
import com.medprep.dto.response.ApiResponse;
import com.medprep.dto.response.DocumentResponse;
import com.medprep.service.AcademyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academies")
@RequiredArgsConstructor
@Tag(name = "Gestor de Conocimiento", description = "Academias y documentos")
public class AcademyController {

    private final AcademyService academyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear academia (solo admin)")
    public ResponseEntity<ApiResponse<AcademyResponse>> create(
            @Valid @RequestBody CreateAcademyRequest req,
            @AuthenticationPrincipal String userId) {
        AcademyResponse response = academyService.create(req, UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Academia creada"));
    }

    @GetMapping
    @Operation(summary = "Listar academias")
    public ApiResponse<List<AcademyResponse>> list() {
        return ApiResponse.ok(academyService.listAcademies());
    }

    @GetMapping("/{academyId}/documents")
    @Operation(summary = "Listar documentos de una academia")
    public ApiResponse<List<DocumentResponse>> listDocuments(@PathVariable UUID academyId) {
        return ApiResponse.ok(academyService.listDocuments(academyId));
    }

    @PostMapping(value = "/{academyId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir documento (PDF) a una academia")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @PathVariable UUID academyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic,
            @AuthenticationPrincipal String userId) {
        DocumentResponse response = academyService.uploadDocument(
                academyId, file, isPublic, UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Documento subido"));
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "Obtener URL firmada de descarga de un documento")
    public ApiResponse<Map<String, String>> download(@PathVariable UUID documentId) {
        return ApiResponse.ok(Map.of("url", academyService.getDownloadUrl(documentId)));
    }
}
