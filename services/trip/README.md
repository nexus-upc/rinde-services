# rinde-trip-service

Servicio de Trip Management de RINDE by NEXUS: programación de viajes, asignación de vehículo y conductor, inicio y fin del viaje. Es el segundo servicio extraído del monolito `rinde-backend` (Sprint 2, tarea S2-05). Usa la librería `rinde-shared-kernel` (com.nexus:rinde-shared-kernel:0.2.0), consulta a Fleet y a IAM por HTTP y se comunica con los demás servicios por RabbitMQ.

## Requisitos

- Java 21.
- PostgreSQL 17 en local (sin Docker).
- RabbitMQ 4 en local. Por defecto usa el usuario `guest` en `localhost:5672`.
- La librería `rinde-shared-kernel` 0.2.0 instalada en el repositorio local de Maven, o disponible en GitHub Packages.

## Configuración

Los secretos y los datos de conexión se leen del archivo `.env` en la raíz del proyecto, que Git ignora. Para crearlo, copia `.env.example` y completa sus valores.

| Variable | Descripción |
| --- | --- |
| `DB_URL` | URL JDBC de la base del servicio, por ejemplo `jdbc:postgresql://localhost:5432/rinde_trip`. |
| `DB_USERNAME` | Usuario de PostgreSQL. |
| `DB_PASSWORD` | Contraseña de PostgreSQL. |
| `JWT_SECRET` | Secreto de firma de los tokens. Debe ser el mismo que usa IAM y tener al menos 32 caracteres. |
| `FLEET_SERVICE_URL` | Base de las consultas internas de Fleet. Por defecto `http://localhost:8083`. |
| `IAM_SERVICE_URL` | Base de IAM & Tenancy. Por defecto `http://localhost:8081`. |
| `RABBITMQ_HOST`, `RABBITMQ_PORT` | Broker de mensajería. Por defecto `localhost` y `5672`. |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | Credenciales del broker. Por defecto `guest` y `guest`. |

El puerto del servicio es el 8082 (`server.port` en `application.yml`).

## Base de datos

El servicio usa su propia base y su propio esquema `trip`. Crea las bases `rinde_trip` (servicio) y `rinde_trip_test` (pruebas) en PostgreSQL:

```sql
CREATE DATABASE rinde_trip;
CREATE DATABASE rinde_trip_test;
```

Al arrancar, Flyway crea el esquema `trip` y aplica las migraciones de `src/main/resources/db/migration/trip`: los viajes, el historial de estados, las ubicaciones en ruta y la tabla `processed_events` con los eventos ya procesados.

## Ejecución

```
mvnw.cmd spring-boot:run
```

Con el servicio arriba, la documentación está en `http://localhost:8082/swagger-ui.html` y el estado en `http://localhost:8082/actuator/health`.

## Pruebas

```
mvnw.cmd clean verify
```

Ejecuta las pruebas unitarias, las de integración contra `rinde_trip_test` y las de aceptación de `src/test/resources/features/trip`. Hay que tener PostgreSQL y RabbitMQ en marcha, porque las pruebas de mensajería usan el broker local. Fleet e IAM no hacen falta: las pruebas los simulan con WireMock.

El escenario "Ciclo completo del viaje contra Fleet real" no incluye las comprobaciones del estado de la unidad en Fleet (IN_TRIP y AVAILABLE). Ese estado lo mantiene Fleet, así que esas pruebas se harán en su servicio.

## Endpoints públicos

Todos requieren `Authorization: Bearer <token>`, salvo que se indique lo contrario.

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| POST | `/api/v1/trips` | ADMINISTRATOR, OPERATIONS_MANAGER | Programa un viaje con código correlativo por empresa (US14). |
| PUT | `/api/v1/trips/{id}/assignment` | ADMINISTRATOR, OPERATIONS_MANAGER | Asigna vehículo y conductor (US15). |
| POST | `/api/v1/trips/{id}/start` | DRIVER | Inicia un viaje asignado al conductor (US20). |
| POST | `/api/v1/trips/{id}/finish` | DRIVER | Finaliza un viaje en curso del conductor (US20). |
| GET | `/api/v1/trips` | ADMINISTRATOR, OPERATIONS_MANAGER | Tablero de viajes, con filtro `status` de varios estados separados por coma (US16). |
| GET | `/api/v1/trips/{id}` | ADMINISTRATOR, OPERATIONS_MANAGER, DRIVER | Detalle del viaje. Un conductor solo ve sus viajes (US17 y US19). |
| GET | `/api/v1/trips/assigned-to-me` | DRIVER | Viajes asignados al conductor, ordenados por fecha de salida (US18). |
| POST | `/api/v1/trips/{id}/locations` | DRIVER | Registra de 1 a 50 puntos de ubicación de un viaje `IN_ROUTE` de forma idempotente por `recordedAt` (S4-33). |
| GET | `/api/v1/trips/locations/latest` | ADMINISTRATOR, OPERATIONS_MANAGER | Última ubicación registrada de cada viaje `IN_ROUTE` de la empresa (S4-33). |

## Endpoints internos

Los llaman otros servicios de RINDE sin JWT. El gateway no los enruta. Están documentados en Swagger con la etiqueta `Internal`.

| Método | Ruta | Respuesta |
| --- | --- | --- |
| GET | `/api/internal/v1/trips/tenants/{tenantId}` | Lista de viajes de la empresa. |
| GET | `/api/internal/v1/trips/tenants/{tenantId}/{tripId}` | Resumen del viaje: `tripId`, `code`, `status`, `origin`, `destination`, `cargoDescription`, `cargoWeightKg`, `departureDate`, `vehicleId`, `driverId`, `startedAt` y `finishedAt`. |
| GET | `/api/internal/v1/trips/tenants/{tenantId}/{tripId}/status` | `{tripId, status}` con el estado actual del viaje. |

Si el viaje no existe en la empresa, responde 404. Estos endpoints reemplazan la fachada `TripContextFacade` del monolito. Los consumen Expense (estado del viaje) y Dashboard (resumen y listado).

## Fleet: contrato que consume

Trip consulta a Fleet con las rutas siguientes, sin JWT.

| Método | Ruta | Respuesta 200 | Respuesta 404 |
| --- | --- | --- | --- |
| GET | `/api/internal/v1/fleet/tenants/{tenantId}/vehicles/{vehicleId}/health` | `{"vehicleId", "maintenanceState", "availableForTrip"}`. El estado de mantenimiento es `UP_TO_DATE`, `DUE_SOON` u `OVERDUE`. Trip usa `maintenanceState`. | La unidad no existe en la empresa. |
| GET | `/api/internal/v1/fleet/tenants/{tenantId}/drivers/{driverId}/eligibility` | `{"driverId", "eligible", "reason"}`. Trip usa `eligible`. | El conductor no existe en la empresa. |
| GET | `/api/internal/v1/fleet/tenants/{tenantId}/users/{userId}/driver` | `{"driverId"}`, el conductor vinculado al usuario. | El usuario no tiene conductor registrado. |

Si Fleet no responde, responde con un error distinto de 404, o la conexión falla, Trip informa 503 con el mensaje "El servicio de flota no está disponible en este momento. Intente nuevamente más tarde.". El viaje no cambia en ese caso.

Trip también consulta a IAM el estado de la empresa en `GET /api/internal/v1/tenants/{tenantId}/status`. Si IAM no responde o la empresa ya no existe, se informa 503 y no se reutiliza el estado anterior del token.

## Eventos publicados

Trip publica en el exchange `rinde.events` (tipo topic, durable). Cada mensaje es JSON y su campo `eventId` sirve como clave de idempotencia para los consumidores.

| Evento | Routing key | Campos |
| --- | --- | --- |
| `TripAssigned` | `trip.assigned` | `eventId`, `occurredAt`, `tenantId`, `tripId`, `code`, `destination`, `departureDate`, `vehicleId`, `driverId` |
| `TripStarted` | `trip.started` | `eventId`, `occurredAt`, `tenantId`, `tripId`, `vehicleId`, `driverId` |
| `TripFinished` | `trip.finished` | `eventId`, `occurredAt`, `tenantId`, `tripId`, `vehicleId`, `driverId` |

Los eventos se publican después del commit del viaje. Si el broker no responde en ese momento, el viaje conserva su nuevo estado y el fallo queda en el log. Los mensajes llevan el header `__TypeId__` que agrega Spring AMQP; los consumidores de otros servicios deben ignorarlo.

## Eventos consumidos

Trip consume `settlement.closed` desde la cola `trip.settlement-closed`, enlazada al exchange `rinde.events`. El mensaje tiene estos campos:

```json
{
  "eventId": "5f0c2d1e-8a7b-4c3d-9e2f-1a2b3c4d5e6f",
  "occurredAt": "2026-10-25T18:30:00Z",
  "tenantId": "0b1c2d3e-4f5a-4b6c-8d7e-9f0a1b2c3d4e",
  "tripId": "7a8b9c0d-1e2f-4a3b-9c8d-7e6f5a4b3c2d",
  "closedBy": "3d4e5f6a-7b8c-4d9e-8f0a-1b2c3d4e5f6a"
}
```

Si el viaje está finalizado, pasa a `SETTLED`. Un `eventId` que ya se procesó se ignora gracias a la tabla `trip.processed_events`. Si el viaje no está finalizado, el mensaje se rechaza sin reencolarlo y el error queda en el log. Si el viaje no existe en la empresa, el evento se registra como procesado y no cambia nada.

## Estructura

El paquete `com.nexus.rinde.trip` se organiza en las capas `domain`, `application`, `infrastructure` e `interfaces`. El cliente de Fleet vive en `infrastructure/fleet` y se usa a través del puerto `FleetContextClient`. La mensajería está en `infrastructure/messaging`. El paquete raíz `com.nexus.rinde` permite que Spring cargue los beans de `rinde-shared-kernel`.

## Límites conocidos

- La publicación después del commit no usa una tabla de salida (outbox). Si el broker cae justo después de guardar el viaje, ese evento se pierde y solo queda en el log.
- Core conserva una copia antigua del módulo de viajes que ya no recibe tráfico. Su retiro requiere la autorización de Pedro.
