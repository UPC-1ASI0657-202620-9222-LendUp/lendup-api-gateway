# Rutas del gateway

Las rutas se evalúan **en orden** y gana la primera que coincide. Por eso `/prestamos/{id}/transacciones` está antes de la ruta general del monolito.

| # | Ruta (`Path`) | Destino | Variable |
| --- | --- | --- | --- |
| 1 | `/api/v1/prestamos/{id}/transacciones` | Pagos | `PAYMENTS_SERVICE_URL` |
| 2 | `/api/v1/medios-pago`, `/api/v1/pagos/**`, `/api/v1/garantias/**`, `/api/v1/webhooks/mercado-pago` | Pagos | `PAYMENTS_SERVICE_URL` |
| 3 | `/api/v1/notificaciones/**` | Notificaciones | `NOTIFICATIONS_SERVICE_URL` |
| 4 | `/api/v1/solicitudes/**`, `/api/v1/reservas/**` | Reservas | `RESERVATIONS_SERVICE_URL` |
| 5 | `/api/v1/catalogo/**` | Catálogo (`lendup-catalog-service`) | `CATALOG_SERVICE_URL` (sin ella, el monolito) |
| 6 | `/api/v1/prestamos/{id}/calificaciones`, `/api/v1/estudiantes/{id}/reputacion` | Reputación (`lendup-reputation-service`) | `REPUTATION_SERVICE_URL` (sin ella, el monolito) |
| 7 | `/api/v1/**` (todo lo demás: Identidad, Préstamos, Evidencias y las rutas de Catálogo del Sprint 1, como `/objetos`) | Monolito | `MONOLITH_URL` |

Catálogo y Reputación usan por defecto la URL del monolito, para no romper nada mientras sus servicios no estén desplegados. Si la variable existe pero está vacía, Spring no usa ese valor por defecto: o se define con una URL, o no se define. Cuando otro equipo extraiga un servicio (por ejemplo Identidad), se agrega su ruta antes de la número 7 y su caso en `GatewayRoutingTest`.

## Decisiones

- **Spring Cloud Gateway (WebFlux)**, la tecnología que el informe asigna al API Gateway. Es Java 21 · Spring Boot 4, igual que Reservas y Pagos.
- **El token se valida en cada servicio, no en el gateway.** El informe (4.1.3, CD-04) dice que el gateway valida el token. En la práctica cada servicio ya lo valida y además necesita el UID para resolver el `usuario_id`. Validarlo dos veces suma latencia y una dependencia más con Firebase. Esta diferencia está anotada en `PENDIENTES.md` para corregir el informe.
- **Sin reescritura de rutas.** Los servicios conservan las rutas del monolito para no romper el frontend.
- **Tiempos de espera.** `GATEWAY_CONNECT_TIMEOUT_MS` (2 s) y `GATEWAY_RESPONSE_TIMEOUT` (20 s), mayores que los de los servicios hacia Mercado Pago e Identidad, para que el error lo decida el servicio y no el gateway.
