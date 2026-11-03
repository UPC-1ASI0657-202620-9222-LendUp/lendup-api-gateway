# Entrega a AWS

## Artefacto

Imagen: `DOCKERHUB_USERNAME/lendup-api-gateway:<semver|sha>`, para `linux/amd64`, en el puerto 8090. Health checks: liveness en `/actuator/health/liveness` y readiness en `/actuator/health/readiness`. Se ejecuta como UID 10001, sin credenciales y con el sistema de archivos de solo lectura (`/tmp` temporal).

## Configuración

- **Obligatorias:** `CORS_ORIGINS` (dominio del frontend en HTTPS), `MONOLITH_URL`, `RESERVATIONS_SERVICE_URL`, `PAYMENTS_SERVICE_URL` y `NOTIFICATIONS_SERVICE_URL`. Las URLs son internas de la VPC; por ejemplo, el DNS de Cloud Map o un ALB interno.
- **Opcionales:** `SERVER_PORT=8090`, `GATEWAY_CONNECT_TIMEOUT_MS=2000` y `GATEWAY_RESPONSE_TIMEOUT=20s`.

El gateway no maneja secretos.

## Red

- **Exposición pública.** Solo el gateway se expone a internet, detrás de un ALB con certificado TLS (R10). Los servicios, las bases de datos y RabbitMQ quedan en subredes privadas.
- **Webhook de Mercado Pago.** La URL pública es `https://<dominio-del-gateway>/api/v1/webhooks/mercado-pago`. Ese valor va en `MERCADOPAGO_NOTIFICATION_URL` de Pagos y en el panel de Mercado Pago.
- **Timeout del ALB.** Debe ser mayor que `GATEWAY_RESPONSE_TIMEOUT`.

## Despliegue y rollback

1. Desplegar primero los servicios destino y comprobar su readiness.
2. Desplegar el gateway y comprobar su readiness.
3. Hacer una llamada por ruta (ver `docs/routes.md`).
4. Cambiar `VITE_API_GATEWAY_URL` en el frontend.

**Rollback:** volver al digest anterior de la imagen. El gateway no guarda estado.
