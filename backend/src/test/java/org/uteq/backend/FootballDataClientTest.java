package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.uteq.backend.common.exception.ApiExternaException;
import org.uteq.backend.common.exception.ApiExternaException.Motivo;
import org.uteq.backend.futbol.client.FootballDataClient;
import org.uteq.backend.futbol.config.FootballApiProperties;
import org.uteq.backend.futbol.dto.externo.StandingsExternoDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Cliente de football-data.org (Bloque 5.1, tarea 5). Usa
 * MockRestServiceServer.bindTo(RestClient.Builder) en vez de mocks de
 * Mockito sobre la API fluida de RestClient (get().uri().retrieve().body()):
 * es mas corto, mas legible, y verifica la URI real construida, que es
 * justo lo que habria detectado un bug de "/v4/v4" duplicado.
 */
class FootballDataClientTest {

    private static final String BASE_URL = "http://localhost:9999/v4";
    private static final String URI_STANDINGS_PL = BASE_URL + "/competitions/PL/standings";

    private FootballApiProperties props(int maxIntentos) {
        return new FootballApiProperties(BASE_URL, "TEST_KEY", "PL", 200, 200, maxIntentos, 1L, 86400L);
    }

    private FootballDataClient clienteConServidor(MockRestServiceServer[] serverOut, int maxIntentos) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        serverOut[0] = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        return new FootballDataClient(restClient, props(maxIntentos));
    }

    private static final String JSON_STANDINGS_TOTAL = """
            {
              "filters": {"season": "2025"},
              "area": {"id": 2072, "name": "England"},
              "competition": {"id": 2021, "name": "Premier League", "code": "PL"},
              "season": {"id": 2411, "startDate": "2025-08-15", "endDate": "2026-05-24", "currentMatchday": 10},
              "standings": [
                {
                  "stage": "REGULAR_SEASON",
                  "type": "TOTAL",
                  "group": null,
                  "table": [
                    {
                      "position": 1,
                      "team": {"id": 57, "name": "Arsenal FC", "shortName": "Arsenal", "tla": "ARS",
                               "crest": "https://crests.football-data.org/57.png"},
                      "playedGames": 10,
                      "form": "W,W,D,W,W",
                      "won": 8,
                      "draw": 1,
                      "lost": 1,
                      "points": 25,
                      "goalsFor": 20,
                      "goalsAgainst": 10,
                      "goalDifference": 10
                    }
                  ]
                }
              ]
            }
            """;

    @Test
    @DisplayName("obtenerPosiciones - respuesta 200 mapea la tabla y llama exactamente a la URI esperada")
    void obtenerPosiciones_respuesta200_mapeaLaTabla() {
        MockRestServiceServer[] serverRef = new MockRestServiceServer[1];
        FootballDataClient client = clienteConServidor(serverRef, 3);
        // Assert de la URI exacta: blindaje contra la duplicacion "/v4/v4"
        // (base-url ya incluye "/v4"; la URI del cliente debe ser relativa).
        serverRef[0].expect(requestTo(URI_STANDINGS_PL))
                .andRespond(withSuccess(JSON_STANDINGS_TOTAL, MediaType.APPLICATION_JSON));

        StandingsExternoDto dto = client.obtenerPosiciones("PL");

        assertEquals("Premier League", dto.competition().name());
        assertEquals("PL", dto.competition().code());
        assertEquals("2025-08-15", dto.season().startDate());
        assertEquals(1, dto.standings().size());
        StandingsExternoDto.TeamStandingExterno fila = dto.standings().get(0).table().get(0);
        assertEquals(1, fila.position());
        assertEquals("Arsenal FC", fila.team().name());
        assertEquals(8, fila.won());
        assertEquals(25, fila.points());
        serverRef[0].verify();
    }

    @Test
    @DisplayName("obtenerPosiciones - 503 repetido reintenta hasta max-intentos y luego lanza SERVIDOR")
    void obtenerPosiciones_error503_reintentaYLanza() {
        MockRestServiceServer[] serverRef = new MockRestServiceServer[1];
        FootballDataClient client = clienteConServidor(serverRef, 3);
        serverRef[0].expect(requestTo(URI_STANDINGS_PL)).andRespond(withServerError());
        serverRef[0].expect(requestTo(URI_STANDINGS_PL)).andRespond(withServerError());
        serverRef[0].expect(requestTo(URI_STANDINGS_PL)).andRespond(withServerError());

        ApiExternaException ex = assertThrows(ApiExternaException.class, () -> client.obtenerPosiciones("PL"));

        assertEquals(Motivo.SERVIDOR, ex.getMotivo());
        serverRef[0].verify(); // confirma que se hicieron exactamente los 3 intentos declarados
    }

    @Test
    @DisplayName("obtenerPosiciones - 429 (rate limit) no reintenta: quemaria la cuota de 10 req/min")
    void obtenerPosiciones_error429_noReintenta() {
        MockRestServiceServer[] serverRef = new MockRestServiceServer[1];
        FootballDataClient client = clienteConServidor(serverRef, 3);
        serverRef[0].expect(requestTo(URI_STANDINGS_PL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        ApiExternaException ex = assertThrows(ApiExternaException.class, () -> client.obtenerPosiciones("PL"));

        assertEquals(Motivo.RATE_LIMIT, ex.getMotivo());
        serverRef[0].verify(); // una sola peticion declarada; si hubiera reintentado, fallaria aqui
    }

    @Test
    @DisplayName("obtenerPosiciones - 404 no reintenta: error determinista")
    void obtenerPosiciones_error404_noReintenta() {
        MockRestServiceServer[] serverRef = new MockRestServiceServer[1];
        FootballDataClient client = clienteConServidor(serverRef, 3);
        serverRef[0].expect(requestTo(URI_STANDINGS_PL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        ApiExternaException ex = assertThrows(ApiExternaException.class, () -> client.obtenerPosiciones("PL"));

        assertEquals(Motivo.NO_ENCONTRADO, ex.getMotivo());
        serverRef[0].verify();
    }

    @Test
    @DisplayName("obtenerPosiciones - 401 no reintenta: la key invalida no se arregla reintentando")
    void obtenerPosiciones_error401_noReintenta() {
        MockRestServiceServer[] serverRef = new MockRestServiceServer[1];
        FootballDataClient client = clienteConServidor(serverRef, 3);
        serverRef[0].expect(requestTo(URI_STANDINGS_PL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ApiExternaException ex = assertThrows(ApiExternaException.class, () -> client.obtenerPosiciones("PL"));

        assertEquals(Motivo.NO_AUTORIZADO, ex.getMotivo());
        serverRef[0].verify();
    }

    @Test
    @DisplayName("obtenerPosiciones - ignora campos desconocidos de la respuesta real (no revienta el mapeo)")
    void obtenerPosiciones_ignoraCamposDesconocidos() {
        String jsonConCamposExtra = """
                {
                  "filters": {"season": "2025"},
                  "area": {"id": 2072, "name": "England", "campoDesconocido": true},
                  "competition": {"id": 2021, "name": "Premier League", "code": "PL", "otroCampo": "x"},
                  "season": {"id": 2411, "startDate": "2025-08-15", "endDate": "2026-05-24", "winner": null},
                  "standings": [
                    {
                      "stage": "REGULAR_SEASON",
                      "type": "TOTAL",
                      "group": null,
                      "table": [
                        {
                          "position": 1,
                          "team": {"id": 57, "name": "Arsenal FC", "address": "N5", "website": "https://x"},
                          "playedGames": 10, "form": "W,W,D,W,W", "won": 8, "draw": 1, "lost": 1,
                          "points": 25, "goalsFor": 20, "goalsAgainst": 10, "goalDifference": 10
                        }
                      ]
                    }
                  ]
                }
                """;
        MockRestServiceServer[] serverRef = new MockRestServiceServer[1];
        FootballDataClient client = clienteConServidor(serverRef, 3);
        serverRef[0].expect(requestTo(URI_STANDINGS_PL))
                .andRespond(withSuccess(jsonConCamposExtra, MediaType.APPLICATION_JSON));

        StandingsExternoDto dto = client.obtenerPosiciones("PL");

        assertNotNull(dto);
        assertEquals("Arsenal FC", dto.standings().get(0).table().get(0).team().name());
    }

    @Test
    @DisplayName("obtenerPosiciones - conserva todos los grupos de standings (TOTAL/HOME/AWAY) sin asumir el primero")
    void obtenerPosiciones_conservaTodosLosGruposDeStandings() {
        String jsonTresGrupos = """
                {
                  "competition": {"name": "Premier League", "code": "PL"},
                  "season": {"startDate": "2025-08-15", "endDate": "2026-05-24"},
                  "standings": [
                    { "type": "HOME", "table": [] },
                    { "type": "AWAY", "table": [] },
                    { "type": "TOTAL", "table": [
                        {"position": 1, "team": {"name": "Arsenal FC"}, "playedGames": 10,
                         "won": 8, "draw": 1, "lost": 1, "points": 25,
                         "goalsFor": 20, "goalsAgainst": 10, "goalDifference": 10}
                    ]}
                  ]
                }
                """;
        MockRestServiceServer[] serverRef = new MockRestServiceServer[1];
        FootballDataClient client = clienteConServidor(serverRef, 3);
        serverRef[0].expect(requestTo(URI_STANDINGS_PL))
                .andRespond(withSuccess(jsonTresGrupos, MediaType.APPLICATION_JSON));

        StandingsExternoDto dto = client.obtenerPosiciones("PL");

        assertEquals(3, dto.standings().size());
        // El grupo TOTAL no esta en el indice 0: la deserializacion no debe perderlo
        // ni asumir que el primero de la lista es el relevante.
        assertTrue(dto.standings().stream().anyMatch(g -> "TOTAL".equals(g.type()) && !g.table().isEmpty()));
    }
}
