package org.uteq.backend.academico.estudiante.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(name = "EstudianteResponse", description = "Estudiante tal como se expone en la API")
public record EstudianteResponse(
        @Schema(example = "1") Long idEstudiante,
        @Schema(example = "Juan") String nombrePersona,
        @Schema(example = "Perez") String apellidoPersona,
        @Schema(example = "Futbol") String nombreCategoria,
        @Schema(example = "Activo") String nombreEstadoGeneral,
        @Schema(example = "2023001234") String codigoEstudiante,
        @Schema(example = "2024-09-02", format = "date") LocalDate fechaIngreso,
        @Schema(example = "62.50") BigDecimal peso,
        @Schema(example = "1.72") BigDecimal altura,
        @Schema(example = "true") Boolean activo,
        @Schema(description = "Fecha de creacion (UTC)") Instant createdAt
) {}