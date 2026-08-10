package org.uteq.backend.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Sobre de respuesta estandar para integraciones con terceros (Bloque 5.1).
 * No es especifico de futbol: cualquier controlador del PFC puede adoptarlo,
 * por eso vive en {@code common.dto} y no en el paquete {@code futbol}.
 *
 * Nota de nombres: coexiste en el mismo archivo fuente que
 * {@code io.swagger.v3.oas.annotations.responses.ApiResponse} (la anotacion
 * que ya usan los 8 controladores existentes para documentar codigos HTTP).
 * Son dos tipos distintos con el mismo nombre simple; donde ambos se
 * necesiten en el mismo archivo, la anotacion de swagger se referencia por
 * su nombre completamente calificado.
 */
@Schema(name = "ApiResponse", description = "Sobre estandar de respuesta para endpoints que integran servicios externos")
public record ApiResponse<T>(
        @Schema(example = "true") boolean success,
        T data,
        @Schema(example = "Tabla de posiciones obtenida exitosamente") String message,
        List<String> errors,
        Map<String, Object> meta) {

    public static <T> ApiResponse<T> ok(T data, String message, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, message, List.of(), meta);
    }
}
