package org.uteq.backend.futbol.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "TablaPosicionesDto", description = "Tabla de posiciones de una liga de futbol")
public record TablaPosicionesDto(
        @Schema(example = "Premier League") String competicion,
        @Schema(example = "2025/2026") String temporada,
        @Schema(example = "PL") String liga,
        List<StandingDto> posiciones) {
}
