package com.medprep.controller;

import com.medprep.dto.response.ApiResponse;
import com.medprep.exception.MedPrepException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** Rutas administrativas ( /api/v1/admin/** ya exige rol ADMIN en SecurityConfig ). */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Gestión de administradores")
public class AdminController {

    private final JdbcTemplate jdbc;

    @PostMapping("/grant")
    @Operation(summary = "Dar rol admin a un usuario por su correo")
    public ApiResponse<String> grant(@RequestParam String email) {
        int n = jdbc.update(
                "UPDATE user_profiles SET role='admin' WHERE id=(SELECT id FROM auth.users WHERE email=?)",
                email);
        if (n == 0) throw MedPrepException.notFound("Usuario con correo", email);
        jdbc.update(
                "UPDATE auth.users SET raw_app_meta_data = coalesce(raw_app_meta_data,'{}'::jsonb) || '{\"role\":\"admin\"}'::jsonb WHERE email=?",
                email);
        return ApiResponse.ok(email + " ahora es admin (debe cerrar sesión y volver a entrar).");
    }
}
