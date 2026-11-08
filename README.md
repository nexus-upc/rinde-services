# RINDE Services

Backend de RINDE by NEXUS. Cada carpeta es un componente con su propio pom.xml, wrapper de Maven, `.env`, base de datos, Dockerfile y versión. Los clientes están en [nexus-upc/rinde-apps](https://github.com/nexus-upc/rinde-apps).

Se desarrolla en `develop`. `main` recibe solo las versiones cerradas de cada sprint.

## Componentes

| Carpeta | Artefacto | Versión | Puerto | Base |
|---|---|---|---|---|
| libraries/shared-kernel | com.nexus:rinde-shared-kernel | 0.2.0 | No aplica | No aplica |
| services/iam | rinde-iam-service | 0.3.0 | 8081 | rinde_iam |
| services/trip | rinde-trip-service | 0.2.0 | 8082 | rinde_trip |
| services/fleet | rinde-fleet-service | 0.1.0 | 8083 | rinde_fleet |
| services/expense | rinde-expense-service | 0.1.0 | 8084 | rinde_expense |
| services/core | rinde-backend | 0.3.0 | 8080 | rinde |
| gateway | rinde-gateway | 0.3.0 | 8000 | No aplica |

Core atiende los contextos que siguen en el monolito: Settlement, Subscriptions & Billing, Notifications y Operations Dashboard. Consulta a Trip, Fleet, Expense e IAM por HTTP. Core ya no contiene los controladores ni el dominio de esos contextos; solo conserva las fachadas y los clientes HTTP que usa.

El README de cada carpeta detalla su configuración, endpoints, contratos internos y eventos.

## Empezar

```powershell
git clone https://github.com/nexus-upc/rinde-services.git
cd rinde-services
git switch develop
git switch -c feature/<tarea>
```

## Compilar y probar

Requiere Java 21, PostgreSQL y RabbitMQ. Cada componente lee su propio `.env`: copiar el `.env.example` de su carpeta y completar los valores. Las pruebas usan `rinde_test`, `rinde_iam_test`, `rinde_trip_test`, `rinde_fleet_test` y `rinde_expense_test`.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -pl services/trip -am clean verify
```

El primer comando prueba todo el reactor; el segundo, un solo servicio con la librería.

## Docker Compose

```powershell
Copy-Item infra/.env.example .env.compose
docker compose --env-file .env.compose up -d --build
```

Levanta el gateway, IAM, Trip, Fleet, Expense, core, RabbitMQ y una instancia de PostgreSQL por servicio. Las bases y el broker no se publican al host y los puertos HTTP escuchan solo en 127.0.0.1. Swagger de todos los servicios: http://localhost:8000/swagger-ui.html.

`pwsh -File tests/deployment/verify.ps1 -Build` levanta un entorno temporal, comprueba el flujo entre procesos y lo elimina. Requiere PowerShell 7.2 o superior.

## Reglas

- Ramas `feature/<tarea>` desde `develop`, integradas con `merge --no-ff`. Conventional Commits en inglés, solo título.
- Un servicio no importa clases de otro contexto: usa HTTP con DTO propios o eventos de RabbitMQ, e ignora los eventos repetidos por `eventId`.
- Ningún servicio lee las tablas de otro.
- `mvnw.cmd clean verify` en verde antes de integrar.
- Tags por componente (`iam/vX.Y.Z`, `trip/vX.Y.Z`, `fleet/vX.Y.Z`). Los tags históricos `v0.1.0` y `v0.2.0` no se cambian.

## Límites conocidos

- Pagos simulados: no hay pasarela bancaria. Correo y push sin proveedor; IAM puede guardar los enlaces de acceso en un buzón de demostración (ver `services/iam`).
- Los eventos se publican después del commit, sin outbox: una caída entre ambos pasos puede perder un evento.
- Incident Management no está implementado.
