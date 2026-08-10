package org.uteq.backend.futbol.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.uteq.backend.common.exception.ApiExternaException;
import org.uteq.backend.common.exception.ApiExternaException.Motivo;
import org.uteq.backend.futbol.config.FootballApiProperties;
import org.uteq.backend.futbol.dto.externo.StandingsExternoDto;

/**
 * Cliente HTTP de football-data.org (Bloque 5.1, tarea 1).
 * Reintentos con backoff exponencial solo ante fallos transitorios: un
 * 429 (rate limit del plan free, 10 req/min) NO se reintenta porque
 * agotaria la cuota y garantizaria otro 429; 401/403/404 tampoco, porque
 * no se arreglan reintentando.
 */
@Component
public class FootballDataClient {

    private static final Logger log = LoggerFactory.getLogger(FootballDataClient.class);

    private final RestClient footballRestClient;
    private final FootballApiProperties props;

    public FootballDataClient(RestClient footballRestClient, FootballApiProperties props) {
        this.footballRestClient = footballRestClient;
        this.props = props;
    }

    /**
     * @param codigoLiga codigo de competicion de football-data.org (p. ej. "PL").
     *                   La URI es relativa al base-url configurado (que ya
     *                   incluye "/v4"): usar una ruta absoluta duplicaria el
     *                   segmento y produciria 404 permanente.
     */
    public StandingsExternoDto obtenerPosiciones(String codigoLiga) {
        long esperaMs = props.backoffInicialMs();
        ApiExternaException ultimoFallo = null;

        for (int intento = 1; intento <= props.maxIntentos(); intento++) {
            try {
                return footballRestClient.get()
                        .uri("/competitions/{codigo}/standings", codigoLiga)
                        .retrieve()
                        .body(StandingsExternoDto.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                throw new ApiExternaException(Motivo.RATE_LIMIT, HttpStatus.TOO_MANY_REQUESTS,
                        "El proveedor externo aplico limite de peticiones (plan gratuito)");
            } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
                throw new ApiExternaException(Motivo.NO_AUTORIZADO, HttpStatus.BAD_GATEWAY,
                        "La clave de la API externa es invalida o no fue configurada");
            } catch (HttpClientErrorException.NotFound e) {
                throw new ApiExternaException(Motivo.NO_ENCONTRADO, HttpStatus.BAD_GATEWAY,
                        "El proveedor externo no tiene datos para esa liga");
            } catch (HttpServerErrorException e) {
                ultimoFallo = new ApiExternaException(Motivo.SERVIDOR, HttpStatus.BAD_GATEWAY,
                        "El proveedor externo respondio con un error de servidor");
            } catch (ResourceAccessException e) {
                ultimoFallo = new ApiExternaException(Motivo.TIMEOUT, HttpStatus.BAD_GATEWAY,
                        "Tiempo de espera agotado al contactar al proveedor externo");
            } catch (HttpClientErrorException e) {
                // Otros 4xx (400, 422, ...): error determinista, no se reintenta.
                throw new ApiExternaException(Motivo.RED, HttpStatus.BAD_GATEWAY,
                        "El proveedor externo rechazo la peticion: " + e.getStatusCode());
            }

            log.warn("football-data.org intento {}/{} fallido para liga={}", intento, props.maxIntentos(), codigoLiga);

            if (intento < props.maxIntentos()) {
                dormir(esperaMs);
                esperaMs *= 2;
            }
        }

        throw ultimoFallo;
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiExternaException(Motivo.RED, HttpStatus.BAD_GATEWAY,
                    "Interrumpido mientras se reintentaba la peticion externa");
        }
    }
}
