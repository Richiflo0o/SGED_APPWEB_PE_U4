package org.uteq.backend.futbol.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.uteq.backend.common.dto.ApiResponse;
import org.uteq.backend.common.exception.ApiExternaException;
import org.uteq.backend.futbol.client.FootballDataClient;
import org.uteq.backend.futbol.config.FootballApiProperties;
import org.uteq.backend.futbol.dto.StandingDto;
import org.uteq.backend.futbol.dto.TablaPosicionesDto;
import org.uteq.backend.futbol.dto.externo.StandingsExternoDto;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Consumo de la API externa football-data.org con caché cache-aside en
 * Redis (Bloque 5.1, tarea 3). Estrategia:
 * 1. Lee "api_externa:standings:{LIGA}" en Redis.
 * 2. Hit -> deserializa, responde con meta.origen = "cache".
 * 3. Miss -> llama al proveedor, guarda en Redis con TTL 24h,
 *    responde con meta.origen = "api".
 * 4. Cualquier fallo del proveedor (timeout, red, 4xx, 5xx) -> responde
 *    HTTP 200 con datos de referencia y meta.origen = "fallback";
 *    degradado = true. El motivo del fallo solo se registra en el log,
 *    nunca se filtra al cliente.
 * Un Redis caido no debe tumbar el endpoint: las operaciones de cache
 * capturan DataAccessException (RedisConnectionFailureException incluida)
 * y el servicio sigue contra la API externa con meta.cache = "no disponible".
 */
@Service
@RequiredArgsConstructor
public class FutbolService {

    private static final Logger log = LoggerFactory.getLogger(FutbolService.class);

    private static final String CACHE_PREFIX = "api_externa:standings:";

    // Codigos de competicion disponibles en el plan gratuito de
    // football-data.org. Validar aqui, antes de salir a la red, evita
    // gastar una de las 10 peticiones/minuto en un codigo inexistente y
    // hace determinista el caso de error para Postman/tests.
    private static final Set<String> LIGAS_VALIDAS = Set.of(
            "PL", "PD", "SA", "BL1", "FL1", "DED", "PPL", "ELC", "BSA", "CL", "EC", "WC");

    private static final List<StandingDto> POSICIONES_FALLBACK = List.of(
            new StandingDto(1, "Equipo de referencia A", null, 0, 0, 0, 0, 0, 0, 0, 0),
            new StandingDto(2, "Equipo de referencia B", null, 0, 0, 0, 0, 0, 0, 0, 0),
            new StandingDto(3, "Equipo de referencia C", null, 0, 0, 0, 0, 0, 0, 0, 0));

    private final FootballDataClient client;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FootballApiProperties props;

    public ApiResponse<TablaPosicionesDto> posiciones(String ligaParam) {
        String liga = normalizarLiga(ligaParam);
        if (!LIGAS_VALIDAS.contains(liga)) {
            throw new IllegalArgumentException(
                    "Codigo de liga no soportado: '" + liga + "'. Ligas disponibles: " + LIGAS_VALIDAS);
        }

        String clave = CACHE_PREFIX + liga;
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("fuente", "football-data.org");
        meta.put("ttl", "24h");
        meta.put("consultadoEn", Instant.now().toString());

        TablaPosicionesDto enCache = leerCache(clave, meta);
        if (enCache != null) {
            meta.put("origen", "cache");
            meta.put("degradado", false);
            return ApiResponse.ok(enCache, "Tabla de posiciones obtenida exitosamente (cache)", meta);
        }

        try {
            StandingsExternoDto externo = client.obtenerPosiciones(liga);
            TablaPosicionesDto tabla = mapear(externo, liga);
            guardarCache(clave, tabla, meta);
            meta.put("origen", "api");
            meta.put("degradado", false);
            return ApiResponse.ok(tabla, "Tabla de posiciones obtenida exitosamente", meta);
        } catch (ApiExternaException e) {
            log.warn("Fallo al consumir football-data.org (motivo={}, liga={}): {}",
                    e.getMotivo(), liga, e.getMessage());
            meta.put("origen", "fallback");
            meta.put("degradado", true);
            return ApiResponse.ok(fallbackMock(liga),
                    "No se pudo contactar al proveedor de datos; se muestran datos de referencia.", meta);
        }
    }

    private String normalizarLiga(String ligaParam) {
        String liga = (ligaParam == null || ligaParam.isBlank()) ? props.ligaDefecto() : ligaParam;
        return liga.toUpperCase();
    }

    private TablaPosicionesDto leerCache(String clave, Map<String, Object> meta) {
        try {
            String json = redisTemplate.opsForValue().get(clave);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, TablaPosicionesDto.class);
        } catch (DataAccessException e) {
            log.warn("Redis no disponible al leer cache de futbol: {}", e.getMessage());
            meta.put("cache", "no disponible");
            return null;
        } catch (JsonProcessingException e) {
            log.warn("Entrada de cache de futbol corrupta, se ignora: {}", e.getMessage());
            return null;
        }
    }

    private void guardarCache(String clave, TablaPosicionesDto tabla, Map<String, Object> meta) {
        try {
            String json = objectMapper.writeValueAsString(tabla);
            redisTemplate.opsForValue().set(clave, json, Duration.ofSeconds(props.ttlCacheSeconds()));
        } catch (DataAccessException e) {
            log.warn("Redis no disponible al escribir cache de futbol: {}", e.getMessage());
            meta.put("cache", "no disponible");
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar la tabla de posiciones para cache: {}", e.getMessage());
        }
    }

    private TablaPosicionesDto mapear(StandingsExternoDto externo, String liga) {
        List<StandingDto> posiciones = extraerTablaTotal(externo).stream()
                .map(this::mapearFila)
                .toList();

        String competicion = externo.competition() != null ? externo.competition().name() : liga;
        String temporada = temporada(externo.season());

        return new TablaPosicionesDto(competicion, temporada, liga, posiciones);
    }

    private List<StandingsExternoDto.TeamStandingExterno> extraerTablaTotal(StandingsExternoDto externo) {
        if (externo.standings() == null) {
            return List.of();
        }
        return externo.standings().stream()
                .filter(grupo -> "TOTAL".equals(grupo.type()))
                .findFirst()
                .map(StandingsExternoDto.StandingGroupExterno::table)
                .orElse(List.of());
    }

    private StandingDto mapearFila(StandingsExternoDto.TeamStandingExterno fila) {
        String equipo = fila.team() != null ? fila.team().name() : "-";
        String escudo = fila.team() != null ? fila.team().crest() : null;
        return new StandingDto(
                fila.position(), equipo, escudo, fila.playedGames(),
                fila.won(), fila.draw(), fila.lost(),
                fila.goalsFor(), fila.goalsAgainst(), fila.goalDifference(), fila.points());
    }

    private String temporada(StandingsExternoDto.SeasonExterno season) {
        if (season == null || season.startDate() == null || season.endDate() == null) {
            return "-";
        }
        return anio(season.startDate()) + "/" + anio(season.endDate());
    }

    private String anio(String fechaIso) {
        return fechaIso.length() >= 4 ? fechaIso.substring(0, 4) : fechaIso;
    }

    private TablaPosicionesDto fallbackMock(String liga) {
        return new TablaPosicionesDto("Datos de referencia (proveedor no disponible)", "-", liga, POSICIONES_FALLBACK);
    }
}
