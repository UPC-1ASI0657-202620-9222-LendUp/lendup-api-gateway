package com.lendup.gateway.config;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Guarantees an X-Correlation-ID on every request: keeps the client's value when it is safe, otherwise creates one.
 * The services log it and copy it into the correlation_id of the events they publish.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
  public static final String HEADER = "X-Correlation-ID";
  private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,100}");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
    String correlationId = incoming != null && SAFE.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
    ServerWebExchange forwarded = exchange.mutate()
        .request(request -> request.headers(headers -> headers.set(HEADER, correlationId)))
        .build();
    forwarded.getResponse().getHeaders().set(HEADER, correlationId);
    return chain.filter(forwarded);
  }

  @Override
  public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}
