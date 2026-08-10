package org.uteq.backend.seguridad.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "Credenciales de inicio de sesion")
public record LoginRequest(
        @NotBlank @Schema(example = "admin", description = "Nombre de usuario") String username,
        @NotBlank @Size(min = 6)
        @Schema(example = "Admin123456*", format = "password", writeOnly = true)
        String password
) {}
