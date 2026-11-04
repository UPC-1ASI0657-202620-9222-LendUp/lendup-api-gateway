# Rutas del gateway

Las rutas se evalúan **en orden** y gana la primera que coincide. La prioridad de Reputación frente a Identidad evita que `/estudiantes/{id}/reputacion` entre en el controlador genérico de estudiantes.

| # | Ruta (`Path`) | Destino | Variable |
| --- | --- | --- | --- |
| 1 | `/api/v1/prestamos/{id}/transacciones` | Pagos | `PAYMENTS_SERVICE_URL` |
| 2 | `/api/v1/medios-pago`, `/api/v1/pagos/**`, `/api/v1/garantias/**`, `/api/v1/webhooks/mercado-pago` | Pagos | `PAYMENTS_SERVICE_URL` |
| 3 | `/api/v1/notificaciones/**` | Notificaciones | `NOTIFICATIONS_SERVICE_URL` |
| 4 | `/api/v1/solicitudes/**`, `/api/v1/reservas/**` | Reservas | `RESERVATIONS_SERVICE_URL` |
| 5 | `/api/v1/catalogo`, `/api/v1/catalogo/**` | Catálogo (.NET 10) | `CATALOG_SERVICE_URL` (sin ella, monolito) |
| 6 | `/api/v1/prestamos/{id}/calificaciones`, `/api/v1/estudiantes/{id}/reputacion` | Reputación | `REPUTATION_SERVICE_URL` (sin ella, monolito) |
| 7 | `/api/v1/estudiantes`, `/api/v1/estudiantes/**`, `/api/v1/universidades`, `/api/v1/universidades/**`, `/api/v1/documentos/terminos` | Identidad (NestJS 11) | `IDENTITY_SERVICE_URL` (sin ella, monolito) |
| 8 | `/api/v1/**` (otros endpoints: Préstamos, Evidencias, `/objetos` heredado) | Monolito | `MONOLITH_URL` |

**IMPORTANTE:** las variables `IDENTITY_SERVICE_URL` y `CATALOG_SERVICE_URL` deben tener el **origen completo** (`http://host:puerto` o `https://servicio.interno`) **sin ruta `/api/v1`**. No pueden estar definidas como cadenas vacías. Al omitirlas, las solicitudes continúan en el monolito para permitir una migración progresiva. Configurarlas explícitamente en AWS activa el tráfico a los microservicios.

## Contratos conservados

- **Identidad:** `POST /api/v1/estudiantes`; `GET/PUT /api/v1/estudiantes/me`; `POST /api/v1/estudiantes/me/verificacion`; `POST /api/v1/estudiantes/me/aceptacion-terminos`; `GET /api/v1/estudiantes/{id}`; `GET /api/v1/universidades`; `GET /api/v1/documentos/terminos`.
- **Catálogo:** `GET /api/v1/catalogo/categorias`, `GET /api/v1/catalogo/publicaciones`, `GET /api/v1/catalogo/publicaciones/{id}`, y operaciones autenticadas en `/api/v1/catalogo/**`.
- El Gateway no usa `StripPrefix` ni modifica los cuerpos, parámetros, tokens Firebase o cabeceras de solicitudes. El microservicio de destino valida el `Authorization: Bearer <Firebase-ID-token>` por sí mismo. Las lecturas públicas no exigen token.
- `GET /api/v1/estudiantes/{id}/reputacion` y `POST /api/v1/prestamos/{id}/calificaciones` permanecen en Reputación (o monolito si está sin configurar).
- `GET /api/v1/objetos` sigue en el monolito: no debe enviarse a Catálogo .NET sin implementar antes compatibilidad de rutas.

## Decisiones técnicas

- **Spring Cloud Gateway (WebFlux)**, Java 21 · Spring Boot 4.
- **Firebase se verifica en cada microservicio, no en el Gateway**. El documento de arquitectura que exige validarlo también en Gateway debe actualizarse si el equipo confirma esta decisión.
- **Tiempos de espera:** `GATEWAY_CONNECT_TIMEOUT_MS=2000` y `GATEWAY_RESPONSE_TIMEOUT=20s`.
- Las pruebas con WireMock comprueban los destinos, la preservación de Authorization y los cuerpos multipart de Catálogo. Los tests no reemplazan un smoke test con Firebase, MySQL y servicios reales.

## Local y AWS

Para ejecutar en un mismo computador, evitar los puertos compartidos: Reservas `8081`, Pagos `8082`, Identidad en puerto **host 8085** y Catálogo en **host 8084**. Si el Gateway está en Docker, usar `host.docker.internal` para comunicarse con servicios que corren en el host. Si están todos en una red Docker común, usar los nombres de servicio con sus puertos internos.

En AWS/ECS, asignar `IDENTITY_SERVICE_URL` y `CATALOG_SERVICE_URL` a los DNS privados de sus respectivos servicios o ALB interno. **Distinto de esto**, Catálogo exige `IDENTITY_BASE_URL` **HTTPS con certificado válido** para comunicarse internamente con Identidad en producción. El Gateway no sustituye esa comunicación interna.
