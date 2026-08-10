package org.uteq.backend.futbol.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia football-data.org (Bloque 5.1, tarea 1: timeouts).
 * Los timeouts no se configuran en el propio RestClient: requieren un
 * ClientHttpRequestFactory. RestClient viene incluido en Spring 6.1 dentro
 * de Boot 3.2.5, no hace falta agregar dependencias al pom.xml.
 */
@Configuration
@EnableConfigurationProperties(FootballApiProperties.class)
public class FootballApiConfig {

    @Bean
    public RestClient footballRestClient(FootballApiProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.timeoutConnectMs());
        factory.setReadTimeout(props.timeoutReadMs());

        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .defaultHeader("X-Auth-Token", props.key())
                .build();
    }
}
