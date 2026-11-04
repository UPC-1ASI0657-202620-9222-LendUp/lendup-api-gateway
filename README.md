# LendUp API Gateway

Punto de entrada único de la aplicación web de LendUp (R14). Es un Spring Cloud Gateway (Java 21 · Spring Boot 4) que envía cada ruta `/api/v1/**` al servicio que la atiende en el Sprint 2: el monolito, `lendup-reservations-service`, `lendup-payments-service`, `lendup-notifications-service`, `lendup-catalog-service`, `lendup-identity-service` o `lendup-reputation-service`.

## Qué hace y qué no

- **Enruta sin cambiar cuerpos ni rutas.** Pasa tal cual `Authorization`, las cabeceras de firma de Mercado Pago (`x-signature`, `x-request-id`) y los parámetros de la URL.
- **Correlación.** Agrega `X-Correlation-ID` si no viene o si trae caracteres no seguros, y lo devuelve en la respuesta.
- **CORS.** Lo resuelve en un solo lugar (`CORS_ORIGINS`) y elimina las cabeceras duplicadas cuando el servicio también las envía.
- **Servicio caído.** Si un servicio destino no responde, el gateway devuelve 503 `SERVICIO_NO_DISPONIBLE`, con el mismo formato de error que los servicios.
- **Salud.** `/actuator/health/liveness` y `/actuator/health/readiness`.
- **No valida el Firebase ID token.** Cada servicio lo valida (revocación y `email_verified`), igual que Reservas. Así un servicio no queda expuesto si alguien llega a él sin pasar por el gateway. El webhook de Mercado Pago es público y Pagos valida su firma.

Tabla de rutas y decisiones: [docs/routes.md](docs/routes.md). Despliegue: [docs/aws-handoff.md](docs/aws-handoff.md).

## Inicio local

Requisitos: JDK 21 (o Docker). Los servicios del monolito y de otros equipos pueden seguir levantados en 8080 (monolito), 8081 (Reservas), 8082 (Pagos) y 8083 (Notificaciones). Identidad y Catálogo pueden usar los puertos **de host** 8085 y 8084, respectivamente, para evitar conflictos.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Gateway en `http://localhost:8090`. En el frontend: `VITE_API_GATEWAY_URL=http://localhost:8090/api/v1`.

Con Docker:

```powershell
Copy-Item .env.example .env
# Activar en .env las URLs de los microservicios si ya están levantados:
# IDENTITY_SERVICE_URL=http://host.docker.internal:8085
# CATALOG_SERVICE_URL=http://host.docker.internal:8084
docker compose up --build
```

El Gateway mantiene el fallback al monolito para Identidad/Catálogo cuando sus URLs no se han configurado. Para enrutar a los nuevos servicios, establecer **ambas** variables en `.env` o en la task definition de AWS. La ruta de Reputación tiene prioridad sobre la de Identidad. `GET /api/v1/objetos` sigue en el monolito. Consulta [las rutas y los contratos](docs/routes.md).

## Imagen

La publicación normal se hace al crear un tag `v*.*.*`, mediante GitHub Actions (`.github/workflows/docker-publish.yml`). Requiere la variable `DOCKERHUB_USERNAME` y el secreto `DOCKERHUB_TOKEN`.
