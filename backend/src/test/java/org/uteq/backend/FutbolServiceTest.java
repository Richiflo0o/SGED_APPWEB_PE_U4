package org.uteq.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.uteq.backend.common.dto.ApiResponse;
import org.uteq.backend.common.exception.ApiExternaException;
import org.uteq.backend.common.exception.ApiExternaException.Motivo;
import org.uteq.backend.futbol.client.FootballDataClient;
import org.uteq.backend.futbol.config.FootballApiProperties;
import org.uteq.backend.futbol.dto.TablaPosicionesDto;
import org.uteq.backend.futbol.dto.externo.StandingsExternoDto;
import org.uteq.backend.futbol.service.FutbolService;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cache-aside en Redis y manejo de errores del proveedor externo
 * (Bloque 5.1, tarea 3). El ObjectMapper es real (no mockeado): la
 * serializacion/deserializacion de TablaPosicionesDto no tiene tipos
 * especiales, y probar contra el ObjectMapper real evita falsos positivos.
 */
@ExtendWith(MockitoExtension.class)
class FutbolServiceTest {

    private static final String CLAVE_PL = "api_externa:standings:PL";

    @Mock
    private FootballDataClient client;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private ObjectMapper objectMapper;
    private FootballApiProperties props;
    private FutbolService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        props = new FootballApiProperties(
                "https://api.football-data.org/v4", "TEST_KEY", "PL", 3000, 5000, 3, 400L, 86400L);
        service = new FutbolService(client, redisTemplate, objectMapper, props);
    }

    private StandingsExternoDto externoConUnEquipo(String nombreEquipo) {
        return new StandingsExternoDto(
                new StandingsExternoDto.CompetitionExterno("Premier League", "PL"),
                new StandingsExternoDto.SeasonExterno("2025-08-15", "2026-05-24"),
                List.of(new StandingsExternoDto.StandingGroupExterno("TOTAL", List.of(
                        new StandingsExternoDto.TeamStandingExterno(
                                1, new StandingsExternoDto.TeamExterno(nombreEquipo, "crest-url"),
                                10, 8, 1, 1, 25, 20, 10, 10)))));
    }

    @Test
    @DisplayName("posiciones - cache hit no llama al cliente HTTP")
    void posiciones_cacheHit_noLlamaAlCliente() throws Exception {
        TablaPosicionesDto cacheado = new TablaPosicionesDto("Premier League", "2025/2026", "PL", List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(CLAVE_PL)).thenReturn(objectMapper.writeValueAsString(cacheado));

        ApiResponse<TablaPosicionesDto> resp = service.posiciones("PL");

        assertEquals("cache", resp.meta().get("origen"));
        assertEquals(false, resp.meta().get("degradado"));
        verify(client, never()).obtenerPosiciones(anyString());
    }

    @Test
    @DisplayName("posiciones - cache miss llama al cliente y guarda en Redis con TTL de 24h")
    void posiciones_cacheMiss_llamaClienteYGuardaConTtl24h() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(CLAVE_PL)).thenReturn(null);
        when(client.obtenerPosiciones("PL")).thenReturn(externoConUnEquipo("Arsenal FC"));

        ApiResponse<TablaPosicionesDto> resp = service.posiciones("PL");

        assertEquals("api", resp.meta().get("origen"));
        assertEquals("Arsenal FC", resp.data().posiciones().get(0).equipo());
        verify(valueOps).set(eq(CLAVE_PL), anyString(), eq(Duration.ofSeconds(86400L)));
    }

    @Test
    @DisplayName("posiciones - timeout del proveedor degrada a fallback con HTTP 200")
    void posiciones_timeout_devuelveFallbackMock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(CLAVE_PL)).thenReturn(null);
        when(client.obtenerPosiciones("PL")).thenThrow(
                new ApiExternaException(Motivo.TIMEOUT, HttpStatus.BAD_GATEWAY, "timeout"));

        ApiResponse<TablaPosicionesDto> resp = service.posiciones("PL");

        assertTrue(resp.success());
        assertEquals("fallback", resp.meta().get("origen"));
        assertEquals(true, resp.meta().get("degradado"));
        assertEquals("Equipo de referencia A", resp.data().posiciones().get(0).equipo());
    }

    @Test
    @DisplayName("posiciones - error 4xx del proveedor degrada a fallback (no propaga el error)")
    void posiciones_error4xx_devuelveFallbackMock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(CLAVE_PL)).thenReturn(null);
        when(client.obtenerPosiciones("PL")).thenThrow(
                new ApiExternaException(Motivo.NO_ENCONTRADO, HttpStatus.BAD_GATEWAY, "no encontrado"));

        ApiResponse<TablaPosicionesDto> resp = service.posiciones("PL");

        assertTrue(resp.success());
        assertEquals("fallback", resp.meta().get("origen"));
        assertEquals(true, resp.meta().get("degradado"));
    }

    @Test
    @DisplayName("posiciones - error 5xx del proveedor degrada a fallback (no propaga el error)")
    void posiciones_error5xx_devuelveFallbackMock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(CLAVE_PL)).thenReturn(null);
        when(client.obtenerPosiciones("PL")).thenThrow(
                new ApiExternaException(Motivo.SERVIDOR, HttpStatus.BAD_GATEWAY, "error servidor"));

        ApiResponse<TablaPosicionesDto> resp = service.posiciones("PL");

        assertTrue(resp.success());
        assertEquals("fallback", resp.meta().get("origen"));
        assertEquals(true, resp.meta().get("degradado"));
    }

    @Test
    @DisplayName("posiciones - liga no soportada lanza 400 sin tocar cache ni cliente HTTP")
    void posiciones_ligaNoValida_lanza400SinLlamarAlCliente() {
        assertThrows(IllegalArgumentException.class, () -> service.posiciones("XXXXX"));

        verify(client, never()).obtenerPosiciones(anyString());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("posiciones - Redis caido no tumba el endpoint: sigue respondiendo desde la API")
    void posiciones_redisCaido_sigueRespondiendoDesdeLaApi() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(CLAVE_PL)).thenThrow(new RedisConnectionFailureException("Redis caido"));
        doThrow(new RedisConnectionFailureException("Redis caido"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));
        when(client.obtenerPosiciones("PL")).thenReturn(externoConUnEquipo("Arsenal FC"));

        ApiResponse<TablaPosicionesDto> resp = service.posiciones("PL");

        assertTrue(resp.success());
        assertEquals("api", resp.meta().get("origen"));
        assertEquals("no disponible", resp.meta().get("cache"));
    }

    @Test
    @DisplayName("posiciones - sin parametro usa la liga por defecto configurada (PL)")
    void posiciones_sinParametro_usaLigaPorDefecto() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(CLAVE_PL)).thenReturn(null);
        when(client.obtenerPosiciones("PL")).thenReturn(externoConUnEquipo("Arsenal FC"));

        ApiResponse<TablaPosicionesDto> resp = service.posiciones(null);

        assertEquals("PL", resp.data().liga());
        verify(valueOps).get(eq(CLAVE_PL));
    }

    @Test
    @DisplayName("posiciones - filtra standings por type=TOTAL, descarta HOME/AWAY")
    void posiciones_filtraPorTypeTotal_descartaHomeYAway() {
        StandingsExternoDto.TeamStandingExterno filaHome = new StandingsExternoDto.TeamStandingExterno(
                1, new StandingsExternoDto.TeamExterno("Solo en casa", null), 5, 5, 0, 0, 15, 12, 2, 10);
        StandingsExternoDto.TeamStandingExterno filaTotal = new StandingsExternoDto.TeamStandingExterno(
                1, new StandingsExternoDto.TeamExterno("Arsenal FC", "crest-url"), 10, 8, 1, 1, 25, 20, 10, 10);
        StandingsExternoDto externo = new StandingsExternoDto(
                new StandingsExternoDto.CompetitionExterno("Premier League", "PL"),
                new StandingsExternoDto.SeasonExterno("2025-08-15", "2026-05-24"),
                List.of(
                        new StandingsExternoDto.StandingGroupExterno("HOME", List.of(filaHome)),
                        new StandingsExternoDto.StandingGroupExterno("AWAY", List.of(filaHome)),
                        new StandingsExternoDto.StandingGroupExterno("TOTAL", List.of(filaTotal))));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(CLAVE_PL)).thenReturn(null);
        when(client.obtenerPosiciones("PL")).thenReturn(externo);

        ApiResponse<TablaPosicionesDto> resp = service.posiciones("PL");

        assertEquals(1, resp.data().posiciones().size());
        assertEquals("Arsenal FC", resp.data().posiciones().get(0).equipo());
        assertEquals(25, resp.data().posiciones().get(0).puntos());
    }
}
