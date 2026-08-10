package org.uteq.backend.futbol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del cliente de football-data.org (Bloque 5.1).
 * Registrada explicitamente en {@link FootballApiConfig} via
 * {@code @EnableConfigurationProperties}: BackendApplication no tiene
 * {@code @ConfigurationPropertiesScan}, asi que un record anotado solo con
 * {@code @ConfigurationProperties} no se registraria como bean.
 */
@ConfigurationProperties(prefix = "football.api")
public record FootballApiProperties(
        String baseUrl,
        String key,
        String ligaDefecto,
        int timeoutConnectMs,
        int timeoutReadMs,
        int maxIntentos,
        long backoffInicialMs,
        long ttlCacheSeconds) {
}
