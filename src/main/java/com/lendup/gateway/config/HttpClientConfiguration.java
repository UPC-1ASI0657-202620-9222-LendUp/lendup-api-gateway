package com.lendup.gateway.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.transport.NameResolverProvider.NameResolverSpec;

/**
 * Netty keeps every resolved address for as long as the DNS server says (10 minutes in Docker), so a service that
 * restarted with another IP stayed unreachable. A short cache makes the gateway find it again within seconds (RNF02).
 */
@Configuration
public class HttpClientConfiguration {
  @Bean
  HttpClientCustomizer dnsCacheCustomizer(@Value("${GATEWAY_DNS_CACHE_TTL:5s}") Duration ttl) {
    return client -> client.resolver(spec -> shortDnsCache(spec, ttl));
  }

  public static void shortDnsCache(NameResolverSpec spec, Duration ttl) {
    spec.cacheMaxTimeToLive(ttl).cacheNegativeTimeToLive(Duration.ofSeconds(1));
  }
}
