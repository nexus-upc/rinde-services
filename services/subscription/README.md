# rinde-subscription-service

Servicio de Subscriptions & Billing de RINDE by NEXUS. Separa de `services/core` los planes, las suscripciones de las empresas, el pago simulado y la administración de cobros de la plataforma. Usa `rinde-shared-kernel` 0.2.0.

## Ejecución

Requiere Java 21, PostgreSQL y RabbitMQ. Crear las bases `rinde_subscription` y `rinde_subscription_test`, copiar `.env.example` como `.env` y completar los valores. `RINDE_BILLING_SIMULATION_WEBHOOK_SECRET` necesita al menos 32 caracteres aunque la simulación esté apagada, y `RINDE_PLATFORM_JWT_SECRET` debe ser distinto de `JWT_SECRET`.

```
.\mvnw.cmd -pl services/subscription -am clean verify
mvnw.cmd spring-boot:run        (desde services/subscription)
```

Puerto 8087. Swagger: http://localhost:8087/swagger-ui.html. Flyway crea el esquema `subscription` con las migraciones V1 a V3, copiadas sin cambios de core.

## Endpoints

| Ruta | Método | Rol |
|---|---|---|
| `/api/v1/plans` | GET | Cualquier autenticado |
| `/api/v1/subscriptions` | POST | ADMINISTRATOR |
| `/api/v1/subscriptions/current` | GET | Cualquier autenticado |
| `/api/v1/subscriptions/{id}` | GET | Cualquier autenticado |
| `/api/v1/subscriptions/{id}/checkout` | POST | ADMINISTRATOR |
| `/api/v1/subscriptions/{subscriptionId}/payments/{paymentId}/simulation` | POST | ADMINISTRATOR |
| `/api/v1/billing/webhooks/payment` | POST | Sin JWT; firma `X-RINDE-Signature` |
| `/api/v1/platform/billing/overview`, `/plans`, `/plans/{id}/availability`, `/subscriptions`, `/payments`, `/audit/billing` | GET, POST, PATCH | PLATFORM_OWNER |

Los contratos están en `docs/sprint3/CONTRATOS_SPRINT3.md`, sección 4.4.2, y no cambian respecto de core.

## Integraciones

| Dirección | Contrato |
|---|---|
| HTTP a Fleet | `GET /api/internal/v1/fleet/tenants/{tenantId}/vehicles/count` (`count`). Si Fleet no responde, el servicio responde 503. |
| HTTP a IAM | Estado de la empresa para el filtro JWT y `GET /api/internal/v1/tenant-directory` para los nombres de empresa. |
| Publica en `rinde.events` | `subscription.activated` y `subscription.suspended` (IAM), `subscription.plan-changed` (Fleet), `subscription.expiring` y `subscription.payment-receipt` (Notifications). |
| Consume | Ninguna cola. Declara `iam.subscription-events` como lo hacía core; la cola `notification.subscription-events` la declara su consumidor. |

## Pruebas

`.feature` trasladados de core sin cambios: US38, US39, US40, US43, US44 y US45, con `SubscriptionSteps`, `PlatformBillingSteps` y `SubscriptionApi`. Fleet e IAM se simulan con WireMock. Las pruebas de integración y Cucumber usan la base `rinde_subscription_test` y un RabbitMQ local.

## Estado

La copia del paquete en core se mantiene hasta que se retire (decisión de Pedro). Mientras ambos existan, no deben levantarse con las mismas colas de consumo.
