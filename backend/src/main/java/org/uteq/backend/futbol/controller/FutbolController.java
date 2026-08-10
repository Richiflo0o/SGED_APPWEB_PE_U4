package org.uteq.backend.futbol.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.futbol.dto.TablaPosicionesDto;
import org.uteq.backend.futbol.service.FutbolService;

/**
 * Consumo de la API externa football-data.org (Bloque 5.1). Endpoint
 * publico: expone datos deportivos de terceros sin informacion personal,
 * el mismo criterio que ya aplica el proyecto a /api/docs y
 * /actuator/health (ver SecurityConfig).
 *
 * Nota de nombres: el tipo de retorno {@code org.uteq.backend.common.dto.ApiResponse}
 * (el sobre {success, data, message, errors, meta}) comparte nombre simple
 * con la anotacion {@code io.swagger.v3.oas.annotations.responses.ApiResponse}
 * que se usa para documentar codigos HTTP; por eso el tipo de retorno se
 * referencia por su nombre completamente calificado.
 */
@RestController
@RequestMapping("/api/futbol")
@RequiredArgsConstructor
@Tag(name = "Futbol (API externa)", description = "Consumo de football-data.org con cache en Redis (Bloque 5.1)")
public class FutbolController {

    private final FutbolService futbolService;

    @GetMapping("/posiciones")
    @Operation(summary = "Tabla de posiciones de una liga",
            description = "Consulta football-data.org con cache-aside en Redis (TTL 24h). "
                    + "Si el proveedor externo falla, responde 200 con datos de referencia "
                    + "(meta.degradado = true) en vez de propagar el error.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tabla de posiciones (origen: api, cache o fallback)"),
            @ApiResponse(responseCode = "400", description = "Codigo de liga no soportado por el plan gratuito")
    })
    public ResponseEntity<org.uteq.backend.common.dto.ApiResponse<TablaPosicionesDto>> posiciones(
            @Parameter(description = "Codigo de competicion, p. ej. PL, PD, SA, BL1. Si se omite, usa la liga por defecto.")
            @RequestParam(required = false) String liga) {
        return ResponseEntity.ok(futbolService.posiciones(liga));
    }
}
