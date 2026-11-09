# rinde-incident-service

Servicio de Incident Management de RINDE by NEXUS: incidencias que el conductor reporta durante un viaje en ruta y su atención hasta el cierre. Usa `rinde-shared-kernel` 0.2.0.

## Ejecución

Requiere Java 21, PostgreSQL y RabbitMQ. Crear las bases `rinde_incident` y `rinde_incident_test`, copiar `.env.example` como `.env` y completar los valores (`DB_*`, `JWT_SECRET`, `IAM_SERVICE_URL`, `TRIP_SERVICE_URL`, `FLEET_SERVICE_URL` y `RABBITMQ_*`).

```
mvnw.cmd spring-boot:run
mvnw.cmd clean verify
```

Puerto 8085. Swagger: http://localhost:8085/swagger-ui.html. Flyway crea el esquema `incident` con la tabla `incidents`.

## Endpoints públicos

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/api/v1/trips/{tripId}/incidents` | DRIVER | Reporte de una incidencia durante el viaje en ruta (US29). Con `idempotencyKey`, un reenvío devuelve la incidencia ya registrada. |
| GET | `/api/v1/incidents` | ADMINISTRATOR, OPERATIONS_MANAGER | Listado de la empresa, más reciente primero. Filtros `status`, `tripId` y `vehicleId` (US30). |
| GET | `/api/v1/incidents/{id}` | Cualquiera autenticado | Detalle. El conductor solo ve las que reportó; otra incidencia responde 404 (US30). |
| PATCH | `/api/v1/incidents/{id}/status` | ADMINISTRATOR, OPERATIONS_MANAGER | Pasa una incidencia abierta a `IN_PROGRESS` (US31). |
| POST | `/api/v1/incidents/{id}/close` | ADMINISTRATOR, OPERATIONS_MANAGER | Cierra la incidencia con la acción tomada, quién la atendió y la fecha (US31). |

Antes de registrar un reporte, el servicio consulta a Trip el viaje y a Fleet el conductor del usuario. El reporte solo se acepta si el viaje existe en la empresa, el usuario es el conductor asignado y el viaje está en `IN_ROUTE`.

## Endpoints internos

Sin JWT y fuera del gateway. Base `/api/internal/v1/incidents/tenants/{tenantId}`.

| Ruta | Lo consume |
|---|---|
| `/open-count` | Dashboard, cantidad de incidencias abiertas o en atención |
| `/open` | Dashboard, listado de incidencias abiertas o en atención |

## Eventos

Publica en el exchange `rinde.events` después del commit:

| Routing key | Evento | Cuándo |
|---|---|---|
| `incident.reported` | `IncidentReported` | Al registrarse una incidencia nueva. |
| `incident.closed` | `IncidentClosed` | Al cerrarse una incidencia. |

Sin outbox: si el broker no responde después del commit, el evento se pierde y el fallo queda en el log.

## Consumidores HTTP

| Servicio | Ruta | Uso |
|---|---|---|
| Trip | `/api/internal/v1/trips/tenants/{tenantId}/{tripId}` | Estado, vehículo y conductor del viaje |
| Fleet | `/api/internal/v1/fleet/tenants/{tenantId}/users/{userId}/driver` | Conductor vinculado al usuario |
| IAM | `/api/internal/v1/tenants/{tenantId}/status` | Estado de la empresa en cada petición |

## Límites conocidos

La evidencia es una URL opcional; el enlace firmado para subir la foto todavía no existe en este servicio. La hora de un reporte hecho sin conexión es la del servidor al recibirlo, no la del teléfono. Un reenvío simultáneo con la misma clave puede responder 500 por la restricción única en lugar de 200.
