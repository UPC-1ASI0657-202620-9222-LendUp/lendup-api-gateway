# LendUp API Gateway

Punto de entrada único de la aplicación web de LendUp (R14). Es un Spring Cloud Gateway (Java 21 · Spring Boot 4) que envía cada ruta `/api/v1/**` al servicio que la atiende en el Sprint 2: el monolito, `lendup-reservations-service`, `lendup-payments-service`, `lendup-notifications-service`, `lendup-catalog-service` o `lendup-reputation-service`.

## Qué hace y qué no

- **Enruta sin cambiar cuerpos ni rutas.** Pasa tal cual `Authorization`, las cabeceras de firma de Mercado Pago (`x-signature`, `x-request-id`) y los parámetros de la URL.
- **Correlación.** Agrega `X-Correlation-ID` si no viene o si trae caracteres no seguros, y lo devuelve en la respuesta.
- **CORS.** Lo resuelve en un solo lugar (`CORS_ORIGINS`) y elimina las cabeceras duplicadas cuando el servicio también las envía.
- **Servicio caído.** Si un servicio destino no responde, el gateway devuelve 503 `SERVICIO_NO_DISPONIBLE`, con el mismo formato de error que los servicios.
- **Salud.** `/actuator/health/liveness` y `/actuator/health/readiness`.
- **No valida el Firebase ID token.** Cada servicio lo valida (revocación y `email_verified`), igual que Reservas. Así un servicio no queda expuesto si alguien llega a él sin pasar por el gateway. El webhook de Mercado Pago es público y Pagos valida su firma.

Tabla de rutas y decisiones: [docs/routes.md](docs/routes.md). Despliegue: [docs/aws-handoff.md](docs/aws-handoff.md).

## Inicio local

Requisitos: JDK 21 (o Docker). Los servicios deben estar levantados en sus puertos: monolito 8080, Reservas 8081, Pagos 8082 y Notificaciones 8083.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Gateway en `http://localhost:8090`. En el frontend: `VITE_API_GATEWAY_URL=http://localhost:8090/api/v1`.

Con Docker:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

## Imagen

La publicación normal se hace al crear un tag `v*.*.*`, mediante GitHub Actions (`.github/workflows/docker-publish.yml`). Requiere la variable `DOCKERHUB_USERNAME` y el secreto `DOCKERHUB_TOKEN`.
