package org.uteq.backend.deportivo.entrenador.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorPageResponse;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorRequest;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorResponse;
import org.uteq.backend.deportivo.entrenador.service.EntrenadorService;

/**
 * CRUD de Entrenador. El alta y la baja crean o retiran el vinculo con una
 * cuenta de usuario, por lo que son operaciones administrativas; la consulta
 * la comparten ADMINISTRADOR y ENTRENADOR.
 */
@RestController
@RequestMapping("/api/entrenadores")
@RequiredArgsConstructor
@Tag(name = "Entrenadores", description = "CRUD de entrenadores vinculados a cuentas de usuario")
public class EntrenadorController {

    private final EntrenadorService entrenadorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Operation(summary = "Listar entrenadores paginados")
    @ApiResponse(responseCode = "200", description = "Pagina de entrenadores")
    public ResponseEntity<EntrenadorPageResponse<EntrenadorResponse>> listar(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(entrenadorService.listar(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    @Operation(summary = "Buscar entrenador por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrenador encontrado"),
            @ApiResponse(responseCode = "404", description = "Entrenador no encontrado")
    })
    public ResponseEntity<EntrenadorResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(entrenadorService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear entrenador", description = "Solo ADMINISTRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entrenador creado"),
            @ApiResponse(responseCode = "400", description = "Validacion de datos fallida")
    })
    public ResponseEntity<EntrenadorResponse> crear(@Valid @RequestBody EntrenadorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entrenadorService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar entrenador", description = "Solo ADMINISTRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrenador actualizado"),
            @ApiResponse(responseCode = "404", description = "Entrenador no encontrado")
    })
    public ResponseEntity<EntrenadorResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody EntrenadorRequest request) {
        return ResponseEntity.ok(entrenadorService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Eliminar entrenador", description = "Solo ADMINISTRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Eliminado"),
            @ApiResponse(responseCode = "404", description = "Entrenador no encontrado")
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        entrenadorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}