package com.lendup.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/** Each backend is a separate WireMock that answers with its own name, so the test sees where every route lands. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "CORS_ORIGINS=http://localhost:5173")
class GatewayRoutingTest {
  static final Map<String, WireMockServer> BACKENDS = Map.of(
      "monolith", new WireMockServer(options().dynamicPort()),
      "reservations", new WireMockServer(options().dynamicPort()),
      "payments", new WireMockServer(options().dynamicPort()),
      "notifications", new WireMockServer(options().dynamicPort()),
      "catalog", new WireMockServer(options().dynamicPort()),
      "reputation", new WireMockServer(options().dynamicPort()));

  static {
    BACKENDS.values().forEach(WireMockServer::start);
  }

  @DynamicPropertySource
  static void backends(DynamicPropertyRegistry registry) {
    registry.add("MONOLITH_URL", () -> BACKENDS.get("monolith").baseUrl());
    registry.add("RESERVATIONS_SERVICE_URL", () -> BACKENDS.get("reservations").baseUrl());
    registry.add("PAYMENTS_SERVICE_URL", () -> BACKENDS.get("payments").baseUrl());
    registry.add("NOTIFICATIONS_SERVICE_URL", () -> BACKENDS.get("notifications").baseUrl());
    registry.add("CATALOG_SERVICE_URL", () -> BACKENDS.get("catalog").baseUrl());
    registry.add("REPUTATION_SERVICE_URL", () -> BACKENDS.get("reputation").baseUrl());
  }

  @LocalServerPort int port;
  WebTestClient client;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    BACKENDS.forEach((name, server) -> {
      server.resetAll();
      server.stubFor(any(anyUrl()).willReturn(okJson("{\"backend\":\"" + name + "\"}")));
    });
  }

  @AfterAll
  static void stop() { BACKENDS.values().forEach(WireMockServer::stop); }

  @ParameterizedTest(name = "{0} {1} -> {2}")
  @CsvSource({
      "GET,    /api/v1/medios-pago,                         payments",
      "POST,   /api/v1/pagos,                               payments",
      "POST,   /api/v1/garantias,                           payments",
      "GET,    /api/v1/prestamos/c3333333/transacciones,    payments",
      "POST,   /api/v1/webhooks/mercado-pago,               payments",
      "GET,    /api/v1/notificaciones,                      notifications",
      "PATCH,  /api/v1/notificaciones/9b1d2c3e,             notifications",
      "POST,   /api/v1/solicitudes,                         reservations",
      "POST,   /api/v1/solicitudes/abc/aceptacion,          reservations",
      "GET,    /api/v1/reservas,                            reservations",
      "GET,    /api/v1/catalogo/publicaciones,              catalog",
      "PATCH,  /api/v1/catalogo/publicaciones/p1/estado,    catalog",
      "POST,   /api/v1/prestamos/c3333333/calificaciones,   reputation",
      "GET,    /api/v1/estudiantes/u1/reputacion,           reputation",
      "GET,    /api/v1/estudiantes/u1,                      monolith",
      "GET,    /api/v1/prestamos/c3333333,                  monolith",
      "POST,   /api/v1/prestamos/c3333333/entrega,          monolith",
      "GET,    /api/v1/estudiantes/me,                      monolith",
      "GET,    /api/v1/objetos,                             monolith",
      "POST,   /api/v1/incidencias,                         monolith"
  })
  void routesEachPathToItsService(String method, String path, String backend) {
    client.method(HttpMethod.valueOf(method)).uri(path).contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
        .exchange()
        .expectStatus().isOk()
        .expectBody().jsonPath("$.backend").isEqualTo(backend);
    BACKENDS.get(backend).verify(1, anyRequestedFor(urlPathEqualTo(path)));
  }

  @Test
  void forwardsAuthorizationAndCreatesCorrelationId() {
    client.get().uri("/api/v1/notificaciones").header(HttpHeaders.AUTHORIZATION, "Bearer token-123")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().value("X-Correlation-ID", id -> assertThat(id).hasSize(36));
    BACKENDS.get("notifications").verify(getRequestedFor(urlPathEqualTo("/api/v1/notificaciones"))
        .withHeader("Authorization", equalTo("Bearer token-123"))
        .withHeader("X-Correlation-ID", matching("[0-9a-f-]{36}")));
  }

  @Test
  void keepsSafeCorrelationIdAndReplacesUnsafeOne() {
    client.get().uri("/api/v1/objetos").header("X-Correlation-ID", "req-123")
        .exchange().expectHeader().valueEquals("X-Correlation-ID", "req-123");
    client.get().uri("/api/v1/objetos").header("X-Correlation-ID", "bad value\twith spaces")
        .exchange().expectHeader().value("X-Correlation-ID", id -> assertThat(id).isNotEqualTo("bad value\twith spaces").hasSize(36));
  }

  @Test
  void mercadoPagoWebhookKeepsSignatureHeadersQueryAndBody() {
    String body = "{\"action\":\"payment.updated\",\"data\":{\"id\":\"123456\"},\"type\":\"payment\"}";
    client.post().uri("/api/v1/webhooks/mercado-pago?data.id=123456&type=payment")
        .header("x-signature", "ts=1704908010,v1=abc").header("x-request-id", "req-mp-1")
        .contentType(MediaType.APPLICATION_JSON).bodyValue(body)
        .exchange().expectStatus().isOk();
    BACKENDS.get("payments").verify(postRequestedFor(urlPathEqualTo("/api/v1/webhooks/mercado-pago"))
        .withQueryParam("data.id", equalTo("123456"))
        .withHeader("x-signature", equalTo("ts=1704908010,v1=abc"))
        .withHeader("x-request-id", equalTo("req-mp-1"))
        .withRequestBody(equalToJson(body)));
  }

  @Test
  void answersCorsPreflightOnceEvenIfTheServiceAlsoSendsCors() {
    BACKENDS.get("payments").stubFor(any(anyUrl()).willReturn(okJson("{\"backend\":\"payments\"}")
        .withHeader("Access-Control-Allow-Origin", "http://localhost:5173").withHeader("Vary", "Origin")));
    client.options().uri("/api/v1/pagos").header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST")
        .exchange().expectStatus().isOk()
        .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
    client.get().uri("/api/v1/medios-pago").header("Origin", "http://localhost:5173")
        .exchange().expectStatus().isOk()
        .expectHeader().values("Access-Control-Allow-Origin", values -> assertThat(values).containsExactly("http://localhost:5173"));
    client.options().uri("/api/v1/pagos").header("Origin", "https://evil.example").header("Access-Control-Request-Method", "POST")
        .exchange().expectStatus().isForbidden();
  }

  @Test
  void healthProbesArePublic() {
    client.get().uri("/actuator/health/liveness").exchange().expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP");
    client.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP");
  }
}
