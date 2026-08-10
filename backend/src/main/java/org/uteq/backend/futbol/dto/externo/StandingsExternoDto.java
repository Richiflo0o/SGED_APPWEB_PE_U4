package org.uteq.backend.futbol.dto.externo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Mapeo parcial de la respuesta real de
 * {@code GET /v4/competitions/{code}/standings} de football-data.org.
 * Cada nivel lleva {@code ignoreUnknown = true} porque la respuesta real
 * trae decenas de campos que no se usan (filters, area, form, group...);
 * sin esto Jackson lanza UnrecognizedPropertyException.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StandingsExternoDto(
        CompetitionExterno competition,
        SeasonExterno season,
        List<StandingGroupExterno> standings) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CompetitionExterno(String name, String code) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeasonExterno(String startDate, String endDate) {
    }

    /**
     * El JSON real anida standings[].table[], y standings[] trae varias
     * entradas por "type" (TOTAL, HOME, AWAY): hay que filtrar por
     * type == "TOTAL", nunca asumir el indice 0.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StandingGroupExterno(
            String type,
            List<TeamStandingExterno> table) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamStandingExterno(
            int position,
            TeamExterno team,
            int playedGames,
            int won,
            int draw,
            int lost,
            int points,
            int goalsFor,
            int goalsAgainst,
            int goalDifference) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamExterno(String name, String crest) {
    }
}
