package org.uteq.backend.seguridad.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "SesionResponse", description = "Datos de la sesion activa")
public class SesionResponse {
    @Schema(example = "admin")
    private String username;
    @Schema(example = "Admin UTEQ")
    private String nombre;
    @Schema(example = "ADMINISTRADOR", description = "Rol de la sesion")
    private String rol;
    @Schema(description = "Access token JWT (solo en /login)", writeOnly = true)
    private String accessToken;
    @Schema(description = "Refresh token JWT (solo en /login)", writeOnly = true)
    private String refreshToken;
}