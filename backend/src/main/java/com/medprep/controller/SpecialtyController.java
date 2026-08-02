package com.medprep.controller;

import com.medprep.dto.response.ApiResponse;
import com.medprep.model.Specialty;
import com.medprep.repository.SpecialtyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
@RequiredArgsConstructor
@Tag(name = "Especialidades", description = "Especialidades y subespecialidades médicas")
public class SpecialtyController {

    private final SpecialtyRepository specialtyRepository;

    @GetMapping
    @Operation(summary = "Listar todas las especialidades con sus subespecialidades")
    public ApiResponse<List<Specialty>> list() {
        return ApiResponse.ok(specialtyRepository.findAll());
    }
}
