package com.lendup.gateway.config;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * When a destination service cannot be reached the gateway answers 503 with the same error body the services use
 * ({@code status}, {@code error}, {@code message}), instead of a generic 500. Other errors keep the default handling.
 */
@Component
@Order(-2) // before Spring Boot's default error handler (-1)
public class UnavailableServiceHandler implements WebExceptionHandler {
  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
    if (!isConnectionFailure(error) || exchange.getResponse().isCommitted()) return Mono.error(error);
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String body = "{\"status\":503,\"error\":\"SERVICIO_NO_DISPONIBLE\",\"message\":\"El servicio no está disponible. Intenta de nuevo en unos minutos.\",\"timestamp\":\""
        + Instant.now() + "\"}";
    return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
  }

  /** Refused connection, or a service name that does not resolve (in Docker, a stopped container). */
  private static boolean isConnectionFailure(Throwable error) {
    for (Throwable current = error; current != null; current = current.getCause()) {
      if (current instanceof ConnectException || current instanceof UnknownHostException) return true;
    }
    return false;
  }
}
