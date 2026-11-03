# Changelog

## Unreleased

- API Gateway inicial con Spring Cloud Gateway (Java 21 · Spring Boot 4 · Spring Cloud 2025.1).
- Rutas a Pagos (incluido el webhook público de Mercado Pago y `/prestamos/{id}/transacciones`), Notificaciones, Reservas, Catálogo, Reputación y monolito, configurables por variables de entorno. Catálogo y Reputación siguen yendo al monolito mientras no se configure su URL.
- `X-Correlation-ID` en todas las peticiones, CORS centralizado sin cabeceras duplicadas y health checks de liveness y readiness.
- Pruebas de enrutamiento con WireMock, imagen sin privilegios y workflows de CI y publicación en Docker Hub.
