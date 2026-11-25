package com.lendup.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendup.gateway.config.HttpClientConfiguration;
import com.lendup.gateway.config.UnavailableServiceHandler;
import java.net.NoRouteToHostException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.netty.transport.NameResolverProvider;

/** A service that restarts with another IP (Docker, ECS) must be reachable again within seconds (RNF02). */
class RestartedServiceTest {
  @Test
  void resolvedAddressesExpireInSeconds() {
    var spec = Mockito.mock(NameResolverProvider.NameResolverSpec.class, Mockito.RETURNS_SELF);
    HttpClientConfiguration.shortDnsCache(spec, Duration.ofSeconds(5));
    Mockito.verify(spec).cacheMaxTimeToLive(Duration.ofSeconds(5));
    Mockito.verify(spec).cacheNegativeTimeToLive(Duration.ofSeconds(1));
  }

  @Test
  void anOldAddressAnswers503InsteadOf500() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/prestamos"));
    new UnavailableServiceHandler().handle(exchange, new IllegalStateException(new NoRouteToHostException("loans/172.18.0.14:8087"))).block();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exchange.getResponse().getBodyAsString().block()).contains("SERVICIO_NO_DISPONIBLE");
  }
}
