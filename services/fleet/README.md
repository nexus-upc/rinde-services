# rinde-fleet-service

Servicio de Fleet & Maintenance de RINDE by NEXUS: unidades, conductores, mantenimientos y límite de unidades del plan. Usa `rinde-shared-kernel` 0.2.0.

## Ejecución

Requiere Java 21, PostgreSQL y RabbitMQ. Crear las bases `rinde_fleet` y `rinde_fleet_test`, copiar `.env.example` como `.env` y completar los valores (`DB_*`, `JWT_SECRET`, `IAM_SERVICE_URL` y `RABBITMQ_*`).

```
mvnw.cmd spring-boot:run
mvnw.cmd clean verify
```

Puerto 8083. Swagger: http://localhost:8083/swagger-ui.html. Flyway crea el esquema `fleet`: unidades, conductores, mantenimientos, `tenant_plan_limits` y `processed_trip_events`.

## Endpoints públicos

Requieren token de empresa. Las altas exigen ADMINISTRATOR u OPERATIONS_MANAGER.

| Método | Ruta | Descripción |
|---|---|---|
| GET, POST | `/api/v1/vehicles` | Listado y alta de unidades (US12). Respeta el límite del plan. |
| GET | `/api/v1/vehicles/{id}` | Ficha de la unidad (US32). |
| GET | `/api/v1/vehicles/{id}/health-status` | Estado de mantenimiento de la unidad (US15 y US34). |
| POST | `/api/v1/vehicles/{id}/maintenances` | Registro de mantenimiento (US33). |
| GET, POST | `/api/v1/drivers` | Listado y alta de conductores (US13). Si se indica `userId`, IAM debe confirmar que la cuenta es DRIVER de la empresa, que está `ACTIVE` y que no está vinculada a otro conductor (404, 400 o 409). |
| PUT | `/api/v1/drivers/{id}/account` | Cambia la cuenta vinculada con `{"userId": "..."}` o la retira con `{"userId": null}` (US11 y US13). Aplica las mismas validaciones del alta. Volver a vincular la misma cuenta responde 200 sin cambios. Sin la clave `userId` responde 400. |
| GET | `/api/v1/drivers/me` | Conductor vinculado a la cuenta del token (rol DRIVER). Responde 404 si la cuenta no tiene vínculo. |
| GET | `/api/v1/drivers/{id}` y `/{id}/eligibility` | Detalle y habilitación del conductor. |
| GET | `/api/v1/maintenances/alerts` | Mantenimientos próximos o vencidos (US34). |

## Endpoints internos

Sin JWT y fuera del gateway. Base `/api/internal/v1/fleet/tenants/{tenantId}`.

| Ruta | Lo consume |
|---|---|
| `/vehicles/{vehicleId}/health` | Trip, al asignar |
| `/drivers/{driverId}/eligibility` | Trip, al asignar |
| `/users/{userId}/driver` | Trip, para saber qué conductor es el usuario |
| `/vehicles/count` | Subscriptions, al elegir un plan |
| `/vehicles` y `/drivers` | Dashboard |

## Eventos

| Dirección | Routing key | Efecto |
|---|---|---|
| Consume | `trip.started`, `trip.finished` | La unidad pasa a IN_TRIP o vuelve a AVAILABLE. |
| Consume | `subscription.plan-changed` | Guarda el límite de unidades de la empresa. |
| Publica | `fleet.maintenance-due` | Aviso de mantenimiento próximo o vencido para Notifications. |

Los consumidores registran el `eventId` e ignoran las entregas repetidas. Los mensajes que fallan quedan en la cola `.dead` de cada consumidor.
