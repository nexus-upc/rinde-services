# rinde-settlement-service

Servicio de Liquidaciones de RINDE by NEXUS. Separa de `services/core` los adelantos, las liquidaciones de viaje y su exportación. Usa `rinde-shared-kernel` 0.2.0.

## Ejecución

Requiere Java 21, PostgreSQL y RabbitMQ. Crear las bases `rinde_settlement` y `rinde_settlement_test`, copiar `.env.example` como `.env` y completar los valores.

```
.\mvnw.cmd -pl services/settlement -am clean verify
mvnw.cmd spring-boot:run        (desde services/settlement)
```

Puerto 8086. Swagger: http://localhost:8086/swagger-ui.html. Flyway crea el esquema `settlement` con las migraciones V1 a V3, copiadas sin cambios de core.

## Endpoints

| Ruta | Método | Rol |
|---|---|---|
| `/api/v1/trips/{tripId}/advances` | POST, GET | ADMINISTRATOR, OPERATIONS_MANAGER |
| `/api/v1/settlements/{id}` | GET | Cualquier autenticado |
| `/api/v1/trips/{tripId}/settlement` | GET | Cualquier autenticado |
| `/api/v1/settlements/{id}/recalculation` | POST | ADMINISTRATOR, OPERATIONS_MANAGER |
| `/api/v1/settlements/{id}/close` | POST | ADMINISTRATOR, OPERATIONS_MANAGER |
| `/api/v1/settlements/{id}/export` | GET | Cualquier autenticado |

Contratos en `docs/sprint3/CONTRATOS_SPRINT3.md`.

## Integraciones

| Dirección | Contrato |
|---|---|
| HTTP a IAM | Estado de la empresa para el filtro JWT. |
| HTTP a Trip | `GET /api/internal/v1/trips/tenants/{tenantId}/{tripId}/status` (estado del viaje). |
| HTTP a Expense | Gastos aprobados del viaje para recalcular y cerrar. Si Expense no responde, el cierre y el recálculo responden 503. |
| Publica en `rinde.events` | `settlement.closed` (Trip y Expense). |
| Consume | `settlement.trip-events` para crear el borrador cuando Trip publica `trip.finished`. |

## Pruebas

`.feature` trasladados de core sin cambios: US25, US25b, US26, US27, US28 y US35. Los steps y el soporte WireMock simulan Trip, Expense e IAM. Las pruebas usan la base `rinde_settlement_test` y un RabbitMQ local.

## Estado

El paquete de liquidaciones se retira de `services/core` en el mismo PR para no duplicarlo. El dashboard de core conserva solo el cliente HTTP (`DashboardSettlementClient`) para contar liquidaciones pendientes por empresa.
