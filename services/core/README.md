# RINDE Backend (core)

Monolito modular original de RINDE by NEXUS, dentro del monorepo. Atiende Settlement, Subscriptions & Billing, Notifications y Operations Dashboard; Incident tiene solo su estructura. Lo común viene de `rinde-shared-kernel` 0.2.0.

De Trip, IAM, Fleet y Expense solo quedan las fachadas (`interfaces/acl`) y los clientes HTTP (`infrastructure/remote`) que usan Dashboard, Settlement, Subscriptions y Notifications. Sus controladores, servicios, dominio y repositorios se retiraron de core: esos contextos viven en sus servicios separados. Las migraciones antiguas de `db/migration/iam`, `trip`, `fleet` y `expense` ya se aplicaron y no se editan; core no las ejecuta.

## Ejecución

Requiere JDK 21, PostgreSQL y RabbitMQ. Crear las bases `rinde` y `rinde_test`, copiar `.env.example` como `.env` y completar los valores.

```
mvnw.cmd spring-boot:run
mvnw.cmd clean verify
```

Puerto 8080. Swagger: http://localhost:8080/swagger-ui.html.

## Comunicación con los servicios separados

Core consulta los otros contextos a través de sus fachadas. Con la propiedad remota activa, que es el valor por defecto, cada fachada llama por HTTP al endpoint interno del servicio:

| Contexto | Activación | Dirección | Uso |
|---|---|---|---|
| IAM | `RINDE_IAM_REMOTE_ENABLED` | `IAM_SERVICE_URL`, 8081 | Usuarios, estado de la empresa y correo del administrador. |
| Trip | `RINDE_TRIP_REMOTE_ENABLED` | `TRIP_SERVICE_URL`, 8082 | Viajes para Dashboard. |
| Fleet | `RINDE_FLEET_REMOTE_ENABLED` | `FLEET_SERVICE_URL`, 8083 | Conteo de unidades para Subscriptions y listados para Dashboard. |
| Expense | `RINDE_EXPENSE_REMOTE_ENABLED` | `EXPENSE_SERVICE_URL`, 8084 | Gastos aprobados para Settlement y gastos para Dashboard. |
| Incident | `RINDE_INCIDENT_REMOTE_ENABLED` | `INCIDENT_SERVICE_URL`, 8085 | Incidencias abiertas para Dashboard (`GET /api/internal/v1/incidents/tenants/{tenantId}/open-count`). |

Si un servicio no responde, la API informa 503. La excepción es Incident: el Dashboard entrega el resto de las métricas con `incidents.source` en `UNAVAILABLE` y `partial` en `true`. Las pruebas ponen estas propiedades en `false` y usan dobles en memoria (`support/ContextDoubles`, `support/ExpenseDouble` e `support/IncidentDouble`), de modo que no necesitan los otros servicios.

## Adelantos y liquidación

El responsable registra adelantos con `POST /api/v1/trips/{tripId}/advances` mientras el viaje esté programado o asignado. Cada adelanto guarda monto, moneda y medio de entrega (`CASH` o `TRANSFER`) en `settlement.trip_advances`. Al recibir `trip.finished`, la liquidación suma los adelantos del viaje. La liquidación no se cierra mientras haya gastos sin aprobar ni observar.

## Eventos

Todo pasa por el exchange durable `rinde.events`, en JSON y con `eventId` como clave de idempotencia.

| Dirección | Routing key | Contexto | Efecto |
|---|---|---|---|
| Consume | `trip.finished` | Settlement | Crea la liquidación del viaje. |
| Publica | `settlement.closed` | Settlement | Trip marca el viaje como liquidado y Expense bloquea sus gastos. |
| Consume | `trip.assigned` | Notifications | Prepara el aviso al conductor. |
| Consume | `fleet.maintenance-due` | Notifications | Prepara el correo al administrador. |
| Publica | `subscription.activated`, `subscription.suspended` | Subscriptions | IAM actualiza el estado comercial de la empresa. |
| Publica | `subscription.plan-changed` | Subscriptions | Fleet guarda el límite de unidades. |

Los consumidores registran el `eventId` y no repiten el efecto. Cada cola tiene su cola `.dead` para los mensajes que fallan. La publicación pide confirmación al broker; si no llega, el error queda en el log con el `eventId` para conciliarlo.

Notifications no tiene proveedor push: deja el aviso pendiente y reintenta a los 1, 2, 4, 8 y 16 minutos. No se presenta como un envío real.

## Pago simulado

El webhook de pago usa `RINDE_BILLING_SIMULATION_WEBHOOK_SECRET`. Con `RINDE_BILLING_SIMULATION_ENABLED=true` se habilita `POST /api/v1/subscriptions/{id}/payments/{paymentId}/simulation`, que confirma o rechaza un pago de prueba desde la web sin exponer la clave del webhook. Está desactivado por defecto y solo se usa en pruebas.

`GET /api/v1/subscriptions/current` devuelve la suscripción de la empresa autenticada, o 204 si aún no tiene una.
