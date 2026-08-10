package org.uteq.backend.futbol.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StandingDto", description = "Fila de la tabla de posiciones de una liga")
public record StandingDto(
        @Schema(example = "1") int posicion,
        @Schema(example = "Arsenal FC") String equipo,
        @Schema(example = "https://crests.football-data.org/57.png") String escudo,
        @Schema(example = "10") int partidosJugados,
        @Schema(example = "8") int ganados,
        @Schema(example = "1") int empatados,
        @Schema(example = "1") int perdidos,
        @Schema(example = "20") int golesFavor,
        @Schema(example = "10") int golesContra,
        @Schema(example = "10") int diferenciaGoles,
        @Schema(example = "25") int puntos) {
}
