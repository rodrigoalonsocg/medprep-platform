package com.medprep.controller;

import com.medprep.dto.request.UpdateProfileRequest;
import com.medprep.dto.response.ApiResponse;
import com.medprep.dto.response.UserProfileResponse;
import com.medprep.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Usuario", description = "Perfil del usuario autenticado")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Obtener el perfil del usuario autenticado")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(userService.getProfile(UUID.fromString(userId)));
    }

    @PutMapping
    @Operation(summary = "Actualizar el perfil del usuario autenticado")
    public ApiResponse<UserProfileResponse> updateMe(
            @Valid @RequestBody UpdateProfileRequest req,
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(userService.updateProfile(UUID.fromString(userId), req),
                "Perfil actualizado");
    }
}
