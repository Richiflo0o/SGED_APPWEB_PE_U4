package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.dto.ApiResponse;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.futbol.controller.FutbolController;
import org.uteq.backend.futbol.dto.StandingDto;
import org.uteq.backend.futbol.dto.TablaPosicionesDto;
import org.uteq.backend.futbol.service.FutbolService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato ApiResponse del endpoint /api/futbol/posiciones (Bloque 5.1,
 * tarea 2). standaloneSetup + GlobalExceptionHandler, igual que el resto
 * de *ControllerTest del proyecto: standaloneSetup no evalua
 * @PreAuthorize/permitAll (no hay contexto de Spring Security), asi que el
 * caracter publico del endpoint se verifica con curl real en
 * scripts/audit-owasp.sh, no aqui.
 */
@ExtendWith(MockitoExtension.class)
class FutbolControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FutbolService futbolService;

    @InjectMocks
    private FutbolController futbolController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(futbolController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/futbol/posiciones - 200 con el sobre {success,data,message,errors,meta}")
    void getPosiciones_200_devuelveSobreApiResponse() throws Exception {
        StandingDto fila = new StandingDto(1, "Arsenal FC", "crest-url", 10, 8, 1, 1, 20, 10, 10, 25);
        TablaPosicionesDto tabla = new TablaPosicionesDto("Premier League", "2025/2026", "PL", List.of(fila));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("origen", "cache");
        meta.put("degradado", false);
        when(futbolService.posiciones("PL")).thenReturn(
                ApiResponse.ok(tabla, "Tabla de posiciones obtenida exitosamente", meta));

        mockMvc.perform(get("/api/futbol/posiciones").param("liga", "PL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.posiciones[0].equipo").value("Arsenal FC"))
                .andExpect(jsonPath("$.meta.origen").value("cache"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("GET /api/futbol/posiciones - liga invalida da 400 con ProblemDetail")
    void getPosiciones_ligaInvalida_400ProblemDetail() throws Exception {
        when(futbolService.posiciones("XXXXX"))
                .thenThrow(new IllegalArgumentException("Codigo de liga no soportado: 'XXXXX'"));

        mockMvc.perform(get("/api/futbol/posiciones").param("liga", "XXXXX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
