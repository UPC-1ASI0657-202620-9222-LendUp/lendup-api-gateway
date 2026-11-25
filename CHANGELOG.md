# Changelog

## Unreleased

- Rutas de Préstamos (`/prestamos/**` y `/calendario`) hacia `LOANS_SERVICE_URL`, con fallback al monolito. Pruebas de enrutamiento de cada ruta.
- Evidencias e Incidencias con ruta propia (`EVIDENCE_SERVICE_URL`, con fallback al monolito), antes de la general de Préstamos: su microservicio se conecta sin tocar código.
- `/prestamos/{id}/desglose-pago` hacia Pagos (RF37).
- Corregido: un servicio reiniciado con otra IP quedaba inaccesible hasta 10 minutos y respondía 500. La caché de DNS dura 5 s (`GATEWAY_DNS_CACHE_TTL`) y mientras tanto se responde 503.
- API Gateway inicial con Spring Cloud Gateway (Java 21 · Spring Boot 4 · Spring Cloud 2025.1).
- Rutas a Pagos (incluido el webhook público de Mercado Pago y `/prestamos/{id}/transacciones`), Notificaciones, Reservas, Catálogo, Reputación y monolito, configurables por variables de entorno. Catálogo y Reputación siguen yendo al monolito mientras no se configure su URL.
- `X-Correlation-ID` en todas las peticiones, CORS centralizado sin cabeceras duplicadas y health checks de liveness y readiness.
- Pruebas de enrutamiento con WireMock, imagen sin privilegios y workflows de CI y publicación en Docker Hub.
