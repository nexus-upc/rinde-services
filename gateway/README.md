# rinde-gateway

Puerta de enlace de RINDE by NEXUS. Recibe las peticiones de `rinde-web` en el puerto 8000, valida el token de acceso y las reenvía a IAM, Trip, Fleet, Expense o al monolito (core). Está hecha con Spring Cloud Gateway (WebFlux), Java 21 y Maven.

El gateway no tiene lógica de negocio. Los servicios vuelven a validar el token, así que el encabezado `Authorization` se reenvía sin cambios.

## Rutas

| Ruta | Destino | Variable |
| --- | --- | --- |
| `/api/v1/platform/auth/**`, `/api/v1/platform/me`, `/api/v1/platform/overview`, `/api/v1/platform/companies/**`, `/api/v1/platform/audit/identity`, `/api/v1/platform/demo-mailbox/**` | IAM | `IAM_SERVICE_URL` |
| `/api/v1/platform/billing/**`, `/api/v1/platform/plans/**`, `/api/v1/platform/subscriptions/**`, `/api/v1/platform/payments/**`, `/api/v1/platform/audit/billing` | Monolito (Subscriptions & Billing) | `MONOLITH_URL` |
| `/api/v1/auth/**`, `/api/v1/tenants/**`, `/api/v1/users/**`, `/api/v1/demo/features` | IAM (`/api/v1/...`) | `IAM_SERVICE_URL` |
| `/api/v1/trips/{id}/settlement/**` | Monolito (Settlement) | `MONOLITH_URL` |
| `/api/v1/trips/{id}/expenses/**`, `/api/v1/expenses/**` | Expense | `EXPENSE_SERVICE_URL` |
| `/api/v1/trips/{id}/incidents/**`, `/api/v1/incidents/**` | Incident | `INCIDENT_SERVICE_URL` |
| `/api/v1/trips`, `/api/v1/trips/**` (resto de viajes) | Trip (`/api/v1/trips/...`) | `TRIP_SERVICE_URL` |
| `/api/v1/vehicles/**`, `/api/v1/drivers/**`, `/api/v1/maintenances/**` | Fleet | `FLEET_SERVICE_URL` |
| `/api/v1/**` (resto de rutas) | Monolito | `MONOLITH_URL` |
| `/v3/api-docs/iam` | IAM (`/v3/api-docs`) | `IAM_SERVICE_URL` |
| `/v3/api-docs/trip` | Trip (`/v3/api-docs`) | `TRIP_SERVICE_URL` |
| `/v3/api-docs/fleet` | Fleet (`/v3/api-docs`) | `FLEET_SERVICE_URL` |
| `/v3/api-docs/expense` | Expense (`/v3/api-docs`) | `EXPENSE_SERVICE_URL` |
| `/v3/api-docs/core` | core (`/v3/api-docs`) | `MONOLITH_URL` |
| `/v3/api-docs/incident` | Incident (`/v3/api-docs`) | `INCIDENT_SERVICE_URL` |
| `/swagger-ui.html` | Swagger UI con los documentos de todos los servicios | |
| `/api/internal/**` | Ninguno. Responde 404 y nunca se reenvía | |
| `/actuator/health` | Estado del propio gateway | |

Las rutas se declaran en el orden del `order` de `application.yml`, y gana la primera que coincide. Las rutas de IAM, de liquidación y de gastos van antes que los comodines, para que esas subrutas de viajes no pasen a Trip.

## Rutas sin token

Estas peticiones pasan sin `Authorization`:

- `POST /api/v1/tenants` (registro de empresa).
- `POST /api/v1/tenants/{id}/verification`.
- `POST /api/v1/auth/sign-in`.
- `POST /api/v1/auth/password-reset`.
- `POST /api/v1/auth/password`.
- `POST /api/v1/platform/auth/sign-in` (inicio de sesión del dueño).
- `GET /api/v1/demo/features` (solo informa si el buzón de demostración está habilitado).
- Cualquier método sobre `/api/v1/billing/webhooks/**`. El monolito valida la firma del webhook.
- `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/webjars/**` y `/actuator/health`.
- Las peticiones preflight CORS (`OPTIONS`).

El resto exige `Authorization: Bearer <jwt>`. Las rutas `/api/v1/platform/**` solo aceptan un token de plataforma, firmado con `RINDE_PLATFORM_JWT_SECRET`, con emisor `rinde-platform` y audiencia `rinde-platform-api`; las demás solo aceptan un token de empresa. Si el token falta, tiene firma distinta o está vencido, el gateway responde 401 con un cuerpo RFC 7807 y el mensaje "Token ausente o inválido.".

## Comportamiento general

- Si la ruta contiene segmentos "." o "..", después de decodificarla, el gateway responde 400 y no reenvía la petición.
- Si el servicio de destino rechaza la conexión o no responde a tiempo, el gateway responde 503 con un cuerpo RFC 7807 y el detalle "El servicio no está disponible. Intente nuevamente.". El tiempo de espera de respuesta se fija con `spring.cloud.gateway.server.webflux.httpclient.response-timeout`.
- Las respuestas de las rutas de negocio llevan `X-Correlation-Id`. Si el cliente envía un valor válido, se reutiliza y se propaga a los servicios. Si falta o no cumple el formato, el gateway genera uno nuevo.
- CORS se configura solo con `CORS_ALLOWED_ORIGINS`. Se aceptan los métodos GET, POST, PUT, PATCH, DELETE y OPTIONS, y los encabezados `Authorization`, `Content-Type` y `X-Correlation-Id`.

## Variables de entorno

Las variables se leen del archivo `.env` de la raíz, que no se sube a Git. Copia `.env.example` como `.env` y completa los valores.

| Variable | Obligatoria | Valor por defecto | Descripción |
| --- | --- | --- | --- |
| `JWT_SECRET` | Sí | Ninguno | Secreto HS256 de al menos 32 caracteres. Debe ser el mismo que usan IAM y el monolito. |
| `RINDE_PLATFORM_JWT_SECRET` | No | Ninguno | Secreto de los tokens de plataforma, distinto de `JWT_SECRET`. Sin él no se acepta ningún token de plataforma. |
| `IAM_SERVICE_URL` | No | `http://localhost:8081` | Dirección de `rinde-iam-service`. |
| `TRIP_SERVICE_URL` | No | `http://localhost:8082` | Dirección de `rinde-trip-service`. |
| `FLEET_SERVICE_URL` | No | `http://localhost:8083` | Dirección de `rinde-fleet-service`. |
| `EXPENSE_SERVICE_URL` | No | `http://localhost:8084` | Dirección de `rinde-expense-service`. |
| `INCIDENT_SERVICE_URL` | No | `http://localhost:8085` | Dirección de `rinde-incident-service`. |
| `MONOLITH_URL` | No | `http://localhost:8080` | Dirección del monolito `rinde-backend`. |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:4200` | Orígenes permitidos, separados por comas. |

## Puesta en marcha

Lo más simple es levantar todo con Docker Compose desde la raíz del monorepo (ver el README raíz). Para ejecutarlo solo, iniciar antes los servicios a los que enruta y luego `.\mvnw.cmd spring-boot:run` en esta carpeta. El estado está en `http://localhost:8000/actuator/health` y la documentación en `http://localhost:8000/swagger-ui.html`.

## Pruebas

```
.\mvnw.cmd clean verify
```

Las pruebas no necesitan los servicios en ejecución. Usan servidores WireMock en lugar de IAM, Trip, Fleet, Expense y el monolito, y un secreto JWT de prueba, distinto de cualquier entorno real. Cubren el enrutamiento a cada servicio, la validación del token, las rutas públicas, `/api/internal`, CORS y la propagación de `X-Correlation-Id`.
