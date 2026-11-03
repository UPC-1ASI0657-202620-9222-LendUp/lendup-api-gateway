package com.lendup.gateway;

import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/** Payments points to a port nobody listens on: the gateway must answer 503 with the services' error format. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UnavailableServiceTest {
  @DynamicPropertySource
  static void closedPort(DynamicPropertyRegistry registry) throws IOException {
    int port;
    try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
    registry.add("PAYMENTS_SERVICE_URL", () -> "http://localhost:" + port);
  }

  @LocalServerPort int port;

  @Test
  void unreachableServiceAnswers503() {
    WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build()
        .get().uri("/api/v1/medios-pago")
        .exchange()
        .expectStatus().isEqualTo(503)
        .expectHeader().exists("X-Correlation-ID")
        .expectBody()
        .jsonPath("$.status").isEqualTo(503)
        .jsonPath("$.error").isEqualTo("SERVICIO_NO_DISPONIBLE");
  }
}
