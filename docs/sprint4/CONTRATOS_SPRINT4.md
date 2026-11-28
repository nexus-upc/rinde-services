# Contratos de la API del Sprint 4

Documento de contratos del Sprint 4 de RINDE by NEXUS para los servicios del backend y los clientes web y móvil. La fuente de lo existente es el código de `Codigo/rinde-backend` en `develop` (`702d0a2`), leído el 25/11/2026.

Todas las rutas públicas se llaman a través del gateway en `http://localhost:8000` (o la URL pública desplegada). Las rutas `/api/internal/**` son exclusivas para comunicación entre servicios: no pasan por el gateway (devuelve 404) ni deben consumirse desde la web, el móvil ni Postman. Los ejemplos JSON son ilustrativos y usan identificadores de ejemplo, fechas en formato ISO y montos en PEN, sin datos personales reales ni credenciales.

## 1. Cómo levantar el entorno

1. Clonar `nexus-upc/rinde-services` y posicionarse en `develop` actualizado. Cada tarea parte de `develop` en una rama nueva.
2. Copiar `infra/.env.example` a `.env.compose` en la raíz del monorepo y completar los valores locales privados (no subir a Git). Copiar también el `.env.example` de cada módulo que se ejecute por separado (`gateway`, `services/core`, `services/iam`, `services/trip`, `services/fleet`, `services/expense`, `services/incident`, `services/subscription`).
3. Levantar el entorno con `docker compose --env-file .env.compose up -d --build`. Los puertos HTTP locales escuchan en `127.0.0.1`:
   - `gateway`: 8000
   - `core`: 8080 (Settlement, Notifications y Operations Dashboard; la copia residual de `subscription` en `core` no recibe tráfico del gateway y se retira en S4-03)
   - `iam`: 8081
   - `trip`: 8082
   - `fleet`: 8083
   - `expense`: 8084
   - `incident`: 8085
   - `subscription`: 8087
4. Swagger reunido en `http://localhost:8000/swagger-ui.html`.
5. Para compilar y verificar todo el backend desde la raíz: `mvnw.cmd clean verify`. Para un servicio concreto con su librería compartida: `mvnw.cmd -pl services/<servicio> -am clean verify`.

Settlement sigue en `core` (puerto 8080) hasta que se complete su separación en la tarea S4-02. Las rutas públicas de liquidación y adelantos no cambian al separarlo.

## 2. Convenciones comunes

### 2.1. Paginación

Las listas nuevas paginadas devuelven la estructura siguiente:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 57,
  "totalPages": 3
}
```

- `page` empieza en `0` (valor por defecto `0`). Si `page` es negativo, el endpoint responde `400` con `"detail": "El número de página no puede ser negativo."`.
- `size` tiene valor por defecto `20`, mínimo `1` y máximo `100`. Si `size` es menor a `1` o mayor a `100`, el endpoint responde `400` con `"detail": "El tamaño de página debe estar entre 1 y 100."`.
- Cada servicio define su propio record de página en su capa `interfaces/rest/resources`. No se agrega ni se modifica `libraries/shared-kernel` (publicada en versión `0.2.0`).
- Nota sobre consistencia existente: los endpoints previos de plataforma en IAM (`/api/v1/platform/companies` y `/api/v1/platform/demo-requests`) usan `PageResource` de `shared-kernel` con el campo `total` en lugar de `totalItems` y `totalPages`, y conservan ese formato sin cambios.

### 2.2. Respuestas parciales

Las consultas consolidadas que agregan datos de varios servicios responden aunque alguna fuente secundaria falle o exceda el tiempo de espera, incluyendo estos campos en el cuerpo de respuesta:

```json
{
  "partial": true,
  "unavailableSources": ["INCIDENT"]
}
```

- Los valores posibles de `unavailableSources` son: `TRIP`, `FLEET`, `EXPENSE`, `SETTLEMENT`, `INCIDENT` e `IAM`.
- Si todas las fuentes respondieron correctamente, `partial` es `false` y `unavailableSources` es una lista vacía `[]`.
- Patrón de referencia ya implementado: `GET /api/v1/dashboard/metrics` en `core` (`DashboardQueryServiceImpl.countOpenIncidents` y `OperationalMetricsResource`), que ante una excepción `RemoteServiceUnavailableException` al consultar `IncidentContextFacade` devuelve `incidents: { "openCount": null, "source": "UNAVAILABLE" }` y `"partial": true` sin fallar con 503.

### 2.3. Periodo (`from` y `to`)

- Los parámetros de consulta `from` y `to` son fechas en formato ISO `AAAA-MM-DD` (`LocalDate`).
- Si no se envían fechas en los endpoints de tablero (`C9`, igual que en `GET /api/v1/dashboard/metrics`), el periodo va desde el día 1 del mes actual hasta hoy (según el reloj UTC del servicio). En `C3` (historial de vehículo), la ventana por defecto sin fechas son los últimos 90 días.
- Si `from` es posterior a `to`, la respuesta es `400` con `"detail": "La fecha final no puede ser anterior a la inicial."`, manteniendo la misma redacción de `GET /api/v1/dashboard/metrics` (en `C2`, `C3`, `C9` y `metrics`).
- Para formato de fecha inválido, se valida explícitamente con `BusinessRuleException` respondiendo `400` con `"La fecha de inicio debe tener el formato AAAA-MM-DD."` o `"La fecha de fin debe tener el formato AAAA-MM-DD."`, igual que en `DashboardController`.

### 2.4. Errores, sesión y roles

La obtención de sesión (`POST /api/v1/auth/sign-in`), la cabecera `Authorization: Bearer <accessToken>`, los roles de empresa (`ADMINISTRATOR`, `OPERATIONS_MANAGER`, `DRIVER`), el rol de plataforma (`PLATFORM_OWNER`) y los códigos base de seguridad siguen lo definido en la sección 2 de `docs/sprint3/CONTRATOS_SPRINT3.md`.

Todos los servicios devuelven errores en formato RFC 7807 (`application/problem+json`):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "La solicitud tiene datos inválidos.",
  "errors": {
    "amount": "El monto debe ser mayor a cero."
  },
  "traceId": "4f8d2c10-9b3a-4e21-8c77-1a2b3c4d5e6f"
}
```

- `errors` solo aparece cuando hay errores de validación por campo (`MethodArgumentNotValidException` u homólogo).
- Sin token o con token inválido (`401`): `"detail": "Se requiere un token de acceso válido."` (o `"Token ausente o inválido."` cuando lo rechaza el gateway).
- Rol sin permiso (`403`): `"detail": "No tiene permisos para realizar esta operación."`. Empresa suspendida (`403`): `"detail": "El acceso de la empresa está suspendido."`.
- Error interno inesperado (`500`): `"detail": "Ocurrió un error inesperado."`.

### 2.5. Rutas del gateway

El archivo `gateway/src/main/resources/application.yml` evalúa las rutas por su campo `order` (menor primero) y `AnonymousRequestMatcher.java` define las peticiones permitidas sin JWT.

| Ruta pública | Entrada del gateway | `order` | Servicio destino | Requiere JWT | Contratos |
|---|---|---|---|---|---|
| `/api/v1/dashboard/**` | `monolith-service` (`/api/v1/**`) | 12 | `core` (`MONOLITH_URL`) | Sí (empresa) | C1, C9 |
| `/api/v1/notifications/**` | `monolith-service` (`/api/v1/**`) | 12 | `core` (`MONOLITH_URL`) | Sí (empresa) | C5, C6 |
| `/api/v1/expenses`, `/api/v1/expenses/**`, `/api/v1/trips/*/expenses` | `expense-service` | 6 | `expense` (`EXPENSE_SERVICE_URL`) | Sí (empresa) | C2, C4, C10 |
| `/api/v1/vehicles/**` | `fleet-service` | 10 | `fleet` (`FLEET_SERVICE_URL`) | Sí (empresa) | C3 |
| `/api/v1/incidents/**`, `/api/v1/trips/*/incidents` | `incident-service` | 6 | `incident` (`INCIDENT_SERVICE_URL`) | Sí (empresa) | C4, C10 |
| `/api/v1/tenants/**`, `/api/v1/users/**` | `iam-service` | 1 | `iam` (`IAM_SERVICE_URL`) | Sí en `/tenants/current` y `/users/**` | C7 |
| `/api/v1/trips/*/settlement/**`, `/api/v1/trips/*/advances` | `monolith-trip-children` | 5 | `core` (`MONOLITH_URL`) | Sí (empresa) | C10 |
| `/api/v1/trips/**` | `trip-service` | 8 | `trip` (`TRIP_SERVICE_URL`) | Sí (empresa) | C10, C11 |
| `/api/internal/**` | `internal-denied` | 4 | Ninguno (404 por `PathGuardGlobalFilter`) | No aplica | Todos |

Confirmación explícita de precedencia en el gateway: ninguna entrada con `order` de `0` a `11` captura `/api/v1/notifications/**` ni `/api/v1/dashboard/**`, por lo que ambas caen en `monolith-service` (`order: 12`) sin requerir cambios en el gateway mientras permanezcan en `core`. Asimismo, `AnonymousRequestMatcher` solo permite sin token `POST /api/v1/tenants` y `POST /api/v1/tenants/*/verification`, de modo que `GET /api/v1/tenants/current`, `PATCH /api/v1/tenants/current`, `PATCH /api/v1/users/me` y `POST /api/v1/users/me/password` quedan protegidos con JWT de empresa por el gateway.

### 2.6. Identificadores del conductor

- **Trip, Fleet y resumen del viaje (`C1`):** `driverId` corresponde al `id` de la ficha del conductor en Fleet (`DriverResource.id`).
- **Expense y bandeja de gastos (`C2`):** `ExpenseResource.driverId` corresponde al `userId` de IAM del conductor que registró el gasto (`TenantContext.userId()` en `ExpensesController`), no al `id` de la ficha de Fleet. Para mostrar el nombre del conductor en la bandeja, la web debe cruzar `ExpenseResource.driverId` con `DriverResource.userId` de `GET /api/v1/drivers`, nunca con `DriverResource.id`.

---

## 3. Contratos del Sprint 4

### C1. Resumen consolidado del viaje

- **Responsable y tarea:** Gabriel (`S4-11`).
- **Servicio y controlador:** `core` (`DashboardController`).
- **Ruta y método:** `GET /api/v1/dashboard/trips/{tripId}/summary`.
- **Entrada en gateway:** `monolith-service` (`order: 12`).
- **Roles (`@PreAuthorize`):** `hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')`.
- **Parámetros:** `tripId` (UUID, en la ruta).

#### Fuentes y clientes HTTP en `core`

`DashboardQueryServiceImpl` consulta las fuentes siguientes:

1. **Trip:** `HttpTripContextFacade` (`tripRestClient`), llama a `GET /api/internal/v1/trips/tenants/{tenantId}/{tripId}`. Es la fuente principal: si el viaje no existe en la empresa devuelve 404; si Trip no responde devuelve 503.
2. **Fleet:** `HttpFleetContextFacade` (`fleetRestClient`), hoy expone `findVehiclesByTenantId` (`GET /api/internal/v1/fleet/tenants/{tenantId}/vehicles`) y `findDriversByTenantId` (`GET /api/internal/v1/fleet/tenants/{tenantId}/drivers`) para obtener `vehiclePlate` y `driverName` cuando `vehicleId` o `driverId` no son nulos.
3. **Expense:** `HttpExpenseContextFacade` (`expenseRestClient`), llama a `GET /api/internal/v1/expenses/tenants/{tenantId}/trips/{tripId}` para obtener la lista de `ExpenseSummary` (`id`, `category`, `amount`, `currency`, `expenseDate`, `status`).
4. **Settlement:** `SettlementContextFacade` (`SettlementContextFacadeImpl`, fachada local en `core`). Hoy `SettlementSummary` solo expone `status`, `advanceAmount` y `advanceBalance`. En `S4-B`, esta fachada local se amplía para exponer `settlementId`, `settlementClosedAt` y `advanceDeliveryMethod` leyendo `TripAdvance` para obtener el medio del adelanto registrado más recientemente (aunque todavía no exista liquidación). Cuando Gabriel separe Settlement en `S4-02`, convierte `SettlementContextFacade` en cliente HTTP hacia `services/settlement` y agrega `"SETTLEMENT"` a `unavailableSources` si no responde.
5. **Incident:** `HttpIncidentContextFacade` (`incidentRestClient`). Hoy solo implementa `countOpenByTenantId` (`/api/internal/v1/incidents/tenants/{tenantId}/open-count`). `InternalIncidentsController` en `services/incident` ya expone `GET /api/internal/v1/incidents/tenants/{tenantId}/open`, que devuelve `InternalIncidentResource` (`id`, `tripId`, `vehicleId`, `type`, `status`, `reportedAt`). `HttpIncidentContextFacade` se amplía para consumir `/open` y filtrar por `tripId` hasta un máximo de 5 incidencias.

Si Fleet, Expense, Settlement (una vez separado en `S4-02`) o Incident fallan con `RemoteServiceUnavailableException`, la respuesta no falla: marca `partial: true` y agrega el código correspondiente en `unavailableSources` (`FLEET`, `EXPENSE`, `SETTLEMENT`, `INCIDENT`).

#### Respuesta (`200 OK`)

`TripSummaryResource` conserva los 18 campos actuales usados por la web sin renombrar ni quitar ninguno, y agrega los campos nuevos:

| Campo | Tipo | Nullable | Origen / Regla |
|---|---|---|---|
| `tripId` | UUID | No | Existente (Trip) |
| `code` | String | No | Existente (Trip) |
| `status` | String | No | Existente (`TripStatus`: `SCHEDULED`, `ASSIGNED`, `IN_ROUTE`, `FINISHED`, `SETTLED`) |
| `origin` | String | No | Existente (Trip) |
| `destination` | String | No | Existente (Trip) |
| `cargoDescription` | String | No | Existente (Trip) |
| `cargoWeightKg` | Decimal | Sí | Existente (Trip) |
| `departureDate` | LocalDate | No | Existente (Trip) |
| `vehicleId` | UUID | Sí | Existente (Trip) |
| `driverId` | UUID | Sí | Existente (Trip) |
| `startedAt` | Instant | Sí | Existente (Trip) |
| `finishedAt` | Instant | Sí | Existente (Trip) |
| `expenseCount` | Integer | No | Existente (total de gastos del viaje) |
| `expenseTotal` | Decimal | No | Existente (suma de gastos `APPROVED`, `0` si no hay) |
| `expenseCurrency` | String | No | Existente (moneda del primer gasto aprobado o `"PEN"`) |
| `settlementStatus` | String | Sí | Existente (`OPEN`, `CLOSED` o null) |
| `advanceAmount` | Decimal | Sí | Existente (Settlement) |
| `advanceBalance` | Decimal | Sí | Existente (Settlement) |
| `vehiclePlate` | String | Sí | Nuevo (Fleet; null si no hay vehículo o Fleet no respondió) |
| `driverName` | String | Sí | Nuevo (Fleet; null si no hay conductor o Fleet no respondió) |
| `expenseCountsByStatus` | Object | No | Nuevo; claves fijas de `ExpenseStatus`: `REGISTERED`, `PENDING_SUPPORT`, `APPROVED`, `OBSERVED` con conteo entero (`0` si no hay) |
| `approvedTotal` | Decimal | No | Nuevo; suma de montos de gastos en estado `APPROVED` |
| `pendingReviewCount` | Integer | No | Nuevo; cantidad de gastos en `REGISTERED` o `PENDING_SUPPORT` |
| `advanceDeliveryMethod` | String | Sí | Nuevo (`CASH`, `TRANSFER` o null, de `AdvanceDeliveryMethod`); corresponde al adelanto registrado más recientemente y se devuelve aunque aún no exista liquidación |
| `settlementId` | UUID | Sí | Nuevo (identificador de la liquidación o null si aún no existe) |
| `settlementClosedAt` | Instant | Sí | Nuevo (fecha de cierre de la liquidación o null si está abierta o no existe) |
| `openIncidents` | Array | No | Nuevo; hasta 5 objetos `{ id, type, status, reportedAt }` con `type` de `IncidentType` (`BREAKDOWN`, `DELAY`, `OTHER`) y `status` de `IncidentStatus` (`OPEN`, `IN_PROGRESS`) |
| `partial` | Boolean | No | Nuevo; `true` si alguna fuente secundaria falló |
| `unavailableSources` | Array de String | No | Nuevo; vacío `[]` si todo respondió, o códigos entre `FLEET`, `EXPENSE`, `SETTLEMENT`, `INCIDENT` |

Ejemplo JSON (`200 OK`):

```json
{
  "tripId": "7b0e4f52-1c9a-4f3e-8d21-5a6c9e0b3d44",
  "code": "TRP-000014",
  "status": "FINISHED",
  "origin": "Lima",
  "destination": "Trujillo",
  "cargoDescription": "Materiales de construcción",
  "cargoWeightKg": 4200.00,
  "departureDate": "2026-11-20",
  "vehicleId": "deac3d8e-12d4-4a72-98f2-1e2fc4d0d230",
  "driverId": "1355b6b8-7cd5-4f4f-9f66-7b883c7e8819",
  "startedAt": "2026-11-20T07:30:00Z",
  "finishedAt": "2026-11-20T18:10:00Z",
  "expenseCount": 4,
  "expenseTotal": 310.00,
  "expenseCurrency": "PEN",
  "settlementStatus": "OPEN",
  "advanceAmount": 500.00,
  "advanceBalance": 190.00,
  "vehiclePlate": "ABC-123",
  "driverName": "Luis Quispe Mamani",
  "expenseCountsByStatus": {
    "REGISTERED": 1,
    "PENDING_SUPPORT": 0,
    "APPROVED": 2,
    "OBSERVED": 1
  },
  "approvedTotal": 310.00,
  "pendingReviewCount": 1,
  "advanceDeliveryMethod": "TRANSFER",
  "settlementId": "91a4c2d0-8e33-4a1f-9c22-44b1e0a9f101",
  "settlementClosedAt": null,
  "openIncidents": [
    {
      "id": "3c589391-d520-4d01-838a-1a574c7d2bb6",
      "type": "DELAY",
      "status": "OPEN",
      "reportedAt": "2026-11-20T13:20:00Z"
    }
  ],
  "partial": false,
  "unavailableSources": []
}
```

#### Errores de negocio

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 404 | El viaje no existe. | El `tripId` no existe o pertenece a otra empresa. |
| 503 | El servicio de viajes no está disponible. Intente nuevamente. | `HttpTripContextFacade` no pudo consultar Trip (`RemoteServiceUnavailableException` con `"viajes"`). |

#### Eventos y reparto S4-B vs responsable

- **Eventos:** ninguno.
- **S4-B:** define el record ampliado `TripSummaryResource` en `core`, el método de consulta de incidencias abiertas en `IncidentContextFacade` / `HttpIncidentContextFacade` (consumiendo `/open`), la ampliación de la fachada local `SettlementContextFacade` / `SettlementContextFacadeImpl` (`settlementId`, `settlementClosedAt` y `advanceDeliveryMethod` desde `TripAdvance`), y la firma actualizada en `DashboardQueryServiceImpl`.
- **Gabriel (S4-11):** implementa la lógica completa de agregación, conteos, manejo de fallas parciales por cada fuente secundaria, pruebas unitarias y de integración en `core` y la pantalla consolidada en web. Al ejecutar `S4-02` (separación de Settlement), convierte `SettlementContextFacade` en cliente HTTP hacia `services/settlement` y agrega `"SETTLEMENT"` a `unavailableSources` cuando no responda.

---

### C2. Bandeja de gastos

- **Responsable y tarea:** Farid (`S4-12`).
- **Servicio y controlador:** `expense` (`ExpensesController`).
- **Ruta y método:** `GET /api/v1/expenses`.
- **Entrada en gateway:** `expense-service` (`order: 6`, `Path=/api/v1/trips/*/expenses,...,/api/v1/expenses,/api/v1/expenses/**`).
- **Roles (`@PreAuthorize`):** `hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')`.
- **Confirmación de colisión de rutas:** `ExpensesController` expone actualmente `/api/v1/trips/{tripId}/expenses`, `/api/v1/expenses/{id}/status`, `/api/v1/expenses/{id}`, `/api/v1/expenses/{expenseId}/evidences`, `/api/v1/expenses/{expenseId}/evidences/{evidenceId}/content` y `/api/v1/expenses/sync`. No existe ningún `@GetMapping("/api/v1/expenses")`, por lo que no hay colisión ni ambigüedad con `/api/v1/expenses/{id}` (`{id}` es `UUID`).

#### Parámetros de consulta (todos opcionales)

| Parámetro | Tipo | Regla y valor por defecto |
|---|---|---|
| `status` | String | Uno o varios valores de `ExpenseStatus` (`REGISTERED`, `PENDING_SUPPORT`, `APPROVED`, `OBSERVED`) separados por coma. |
| `tripId` | UUID | Filtra por viaje. |
| `driverId` | UUID | Filtra por conductor: en `expense`, corresponde al `userId` de IAM del conductor que registró el gasto (`TenantContext.userId()` en `ExpensesController`), no al `id` de la ficha en Fleet. |
| `from` | String (`AAAA-MM-DD`) | Fecha mínima inclusiva de `expenseDate`. |
| `to` | String (`AAAA-MM-DD`) | Fecha máxima inclusiva de `expenseDate`. |
| `page` | Integer | Número de página base `0`. Por defecto `0`. |
| `size` | Integer | Tamaño de página. Por defecto `20`, mínimo `1`, máximo `100`. |

- **Orden obligatorio:** `expenseDate` descendente y luego `createdAt` descendente.
- **Regla de independencia e identificador de conductor:** Expense no llama a Trip ni a Fleet para enriquecer la bandeja. Como `ExpenseResource.driverId` almacena el `userId` de IAM del conductor, la web obtiene el código de viaje con `GET /api/v1/trips` y el nombre del conductor cruzando `ExpenseResource.driverId` con `DriverResource.userId` de `GET /api/v1/drivers`, nunca con `DriverResource.id`.

#### Respuesta (`200 OK`)

Objeto paginado con las convenciones de la sección 2.1, cuyos elementos son `ExpenseResource` tal como existe hoy en `services/expense`:

```json
{
  "items": [
    {
      "id": "3c589391-d520-4d01-838a-1a574c7d2bb6",
      "tenantId": "d011f0a2-2b6f-4d32-9cb4-fa6d7cb81698",
      "tripId": "87460614-42ea-4cd5-9de1-8f38d9496434",
      "driverId": "764a4968-403f-40d7-9172-ba637d10fb16",
      "category": "FUEL",
      "amount": 180.50,
      "currency": "PEN",
      "expenseDate": "2026-11-20",
      "status": "REGISTERED",
      "idempotencyKey": "EXP-9831-UUID-01",
      "observationReason": null,
      "isLocked": false,
      "evidence": {
        "id": "7d1c2f0e-3b8a-4d55-9a61-2e0f4c1b9a10",
        "fileName": "recibo-combustible.jpg",
        "contentType": "image/jpeg",
        "fileSizeBytes": 204800,
        "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        "contentUrl": "/api/v1/expenses/3c589391-d520-4d01-838a-1a574c7d2bb6/evidences/7d1c2f0e-3b8a-4d55-9a61-2e0f4c1b9a10/content",
        "uploadedAt": "2026-11-20T14:05:00Z"
      },
      "createdAt": "2026-11-20T14:00:00Z",
      "updatedAt": "2026-11-20T14:05:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

#### Errores de negocio

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | El número de página no puede ser negativo. | `page < 0` |
| 400 | El tamaño de página debe estar entre 1 y 100. | `size < 1` o `size > 100` |
| 400 | La fecha de inicio debe tener el formato AAAA-MM-DD. | `from` con formato inválido |
| 400 | La fecha de fin debe tener el formato AAAA-MM-DD. | `to` con formato inválido |
| 400 | La fecha final no puede ser anterior a la inicial. | `from` posterior a `to` |
| 400 | El parámetro status contiene un estado de gasto inválido. | Algún valor en `status` no pertenece a `ExpenseStatus` |

#### Eventos y reparto S4-B vs responsable

- **Eventos:** ninguno.
- **S4-B:** define el record de página propio en `services/expense` (`ExpensePageResource` o equivalente), el record de query con filtros y la firma del método `GET /api/v1/expenses` en `ExpensesController`.
- **Farid (S4-12):** implementa la consulta filtrada y paginada en `ExpenseRepository` (especificación o consulta JPQL), `ExpenseQueryServiceImpl`, pruebas unitarias, integración y `.feature`, más la bandeja en la web.

---

### C3. Historial del vehículo

- **Responsable y tarea:** Andree (`S4-13`).
- **Servicio y controlador:** `fleet` (`VehiclesController`).
- **Ruta y método:** `GET /api/v1/vehicles/{id}/history`.
- **Entrada en gateway:** `fleet-service` (`order: 10`, `Path=/api/v1/vehicles/**`).
- **Roles (`@PreAuthorize`):** `hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')`.
- **Parámetros:**
  - `id` (UUID del vehículo, en la ruta).
  - `from` (`AAAA-MM-DD`, opcional).
  - `to` (`AAAA-MM-DD`, opcional). Si no se envían fechas, el periodo abarca los últimos 90 días (hasta hoy).

#### Fuentes y clientes HTTP

1. **Mantenimientos (local en Fleet):** consulta `MaintenanceRepository` filtrando por `tenantId`, `vehicleId` y rango de `executionDate`. El campo `nextMaintenanceDate` de la respuesta proviene del último mantenimiento del vehículo, y es `null` cuando el vehículo no tiene mantenimientos registrados.
2. **Viajes (Trip):** cliente HTTP en Fleet hacia `GET /api/internal/v1/trips/tenants/{tenantId}` (`InternalTripsController.list`), filtrando por `vehicleId` y por `departureDate` (o fecha del viaje) dentro de `[from, to]`. Si Trip no responde, se agrega `"TRIP"` a `unavailableSources` y `partial` pasa a `true`.
3. **Incidencias (Incident):** cliente HTTP en Fleet hacia el endpoint interno nuevo `GET /api/internal/v1/incidents/tenants/{tenantId}?vehicleId={id}` en `InternalIncidentsController`, con parámetro obligatorio `vehicleId`. Devuelve todas las incidencias del vehículo en cualquier estado (`OPEN`, `IN_PROGRESS`, `CLOSED`), usando `InternalIncidentResource` (`id`, `tripId`, `vehicleId`, `type`, `description`, `status`, `reportedAt`), donde `S4-B` agrega `description` de forma aditiva (afectando también a `/open` sin quitar ningún campo existente). Si Incident no responde, se agrega `"INCIDENT"` a `unavailableSources` y `partial` pasa a `true`.

#### Respuesta (`200 OK`)

| Campo | Tipo | Nullable | Descripción |
|---|---|---|---|
| `vehicleId` | UUID | No | Identificador del vehículo consultado |
| `nextMaintenanceDate` | LocalDate | Sí | Fecha del próximo mantenimiento programado según el último mantenimiento registrado, o `null` si no tiene mantenimientos |
| `items` | Array | No | Lista unificada de eventos de historial ordenada por `date` descendente |
| `items[].type` | String | No | Valores posibles: `TRIP`, `MAINTENANCE`, `INCIDENT` |
| `items[].id` | UUID | No | Identificador del viaje, mantenimiento o incidencia |
| `items[].date` | Instant | No | Fecha del acontecimiento para orden descendente (en mantenimiento/viaje de fecha `LocalDate`, instante ISO en UTC; en incidencia, `reportedAt`) |
| `items[].title` | String | No | Título resumido (por ejemplo, código y ruta del viaje, tipo de mantenimiento o tipo de incidencia) |
| `items[].status` | String | Sí | Estado del elemento: `TripStatus` para viajes, `IncidentStatus` para incidencias y `null` para mantenimientos (`Maintenance` no tiene estado) |
| `items[].detail` | String | Sí | Detalle breve: notas del mantenimiento, carga del viaje o `description` de la incidencia |
| `partial` | Boolean | No | `true` si Trip o Incident no respondieron |
| `unavailableSources` | Array de String | No | Subconjunto de `["TRIP", "INCIDENT"]` |

Ejemplo JSON (`200 OK`):

```json
{
  "vehicleId": "deac3d8e-12d4-4a72-98f2-1e2fc4d0d230",
  "nextMaintenanceDate": "2027-02-09",
  "items": [
    {
      "type": "INCIDENT",
      "id": "3c589391-d520-4d01-838a-1a574c7d2bb6",
      "date": "2026-11-20T13:20:00Z",
      "title": "Incidencia BREAKDOWN",
      "status": "CLOSED",
      "detail": "Falla en el freno neumático"
    },
    {
      "type": "TRIP",
      "id": "7b0e4f52-1c9a-4f3e-8d21-5a6c9e0b3d44",
      "date": "2026-11-20T07:30:00Z",
      "title": "TRP-000014 Lima a Trujillo",
      "status": "FINISHED",
      "detail": "Materiales de construcción"
    },
    {
      "type": "MAINTENANCE",
      "id": "5a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d",
      "date": "2026-11-09T00:00:00Z",
      "title": "Mantenimiento PREVENTIVE",
      "status": null,
      "detail": "Cambio de aceite y filtros"
    }
  ],
  "partial": false,
  "unavailableSources": []
}
```

#### Errores de negocio

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La fecha de inicio debe tener el formato AAAA-MM-DD. | `from` con formato inválido |
| 400 | La fecha de fin debe tener el formato AAAA-MM-DD. | `to` con formato inválido |
| 400 | La fecha final no puede ser anterior a la inicial. | `from` posterior a `to` |
| 404 | El vehículo no existe. | El vehículo no existe en la empresa del token. |

#### Eventos y reparto S4-B vs responsable

- **Eventos:** ninguno.
- **S4-B:** crea el endpoint interno nuevo de incidencias por vehículo en `services/incident`, configura las propiedades `TRIP_SERVICE_URL` e `INCIDENT_SERVICE_URL` y sus `RestClient` en `services/fleet`, deja implementados los clientes HTTP de Fleet hacia Trip e Incident con sus DTO propios, y define los records de respuesta y la firma del endpoint en `VehiclesController`.
- **Andree (S4-13):** implementa la consulta y combinación de mantenimientos, viajes e incidencias en `VehicleQueryServiceImpl`, manejo de `partial`, pruebas unitarias, integración y `.feature`, más la vista de historial en web.

---

### C4. Archivos en Azure Blob Storage

- **Responsable y tarea:** Farid (`S4-14`).
- **Servicios:** `expense` (sin rutas nuevas) e `incident` (dos rutas nuevas).

#### 4.1. Almacenamiento en Expense (sin rutas nuevas)

- Puerto existente en `services/expense`: `EvidenceStorage` (`save(String storageKey, byte[] content)`, `load(String storageKey)`, `delete(String storageKey)`).
- Adaptador existente: `LocalFileSystemEvidenceStorage`, seleccionado cuando `rinde.evidence.storage.type` vale `local` (valor por defecto).
- Adaptador nuevo: implementará `EvidenceStorage` sobre Azure Blob Storage y se activará con `rinde.evidence.storage.type=azure-blob`.
- Propiedades y variables de entorno en `services/expense`:
  - `rinde.evidence.storage.type` (`RINDE_EVIDENCE_STORAGE_TYPE`, por defecto `local`; valores: `local`, `azure-blob`).
  - `rinde.evidence.storage.local-path` (`RINDE_EVIDENCE_STORAGE_PATH`, por defecto `./data/evidences`).
  - `RINDE_EVIDENCE_BLOB_CONNECTION_STRING` (cadena de conexión de Azure Storage).
  - `RINDE_EVIDENCE_BLOB_CONTAINER` (nombre del contenedor privado).
  - `rinde.evidence.max-size-bytes` (`RINDE_EVIDENCE_MAX_SIZE_BYTES`, por defecto `5242880` = 5 MB; límite multipart del contenedor `10MB`).
- Seguridad del contenedor: el contenedor es privado y no expone URL públicas de Blob Storage. La lectura siempre pasa por `GET /api/v1/expenses/{expenseId}/evidences/{evidenceId}/content` con validación de JWT, empresa y propietario (`ADMINISTRATOR`, `OPERATIONS_MANAGER` o el `DRIVER` que registró el gasto).
- Claves de almacenamiento: `{tenantId}/{expenseId}/{uuid}`, idéntico formato al actual. Formatos permitidos detectados por firma binaria (`EvidenceFormat`): JPEG (`image/jpeg`), PNG (`image/png`), WEBP (`image/webp`) y PDF (`application/pdf`).

#### 4.2. Evidencia de incidencias en Incident (rutas nuevas)

- **Entrada en gateway:** `incident-service` (`order: 6`, `Path=/api/v1/trips/*/incidents,...,/api/v1/incidents,/api/v1/incidents/**`). Ambas rutas ya quedan cubiertas por `/api/v1/incidents/**`.
- **Aislamiento entre servicios:** `services/incident` define su propio puerto de almacenamiento (por ejemplo `IncidentEvidenceStorage`) y sus propios adaptadores `local` y `azure-blob`; no importa clases de `services/expense`.
- **Propiedades y variables de entorno en `services/incident`:** mismas propiedades y variables que Expense (`rinde.evidence.storage.type` con valores `local` por defecto y `azure-blob`, `RINDE_EVIDENCE_STORAGE_PATH`, `RINDE_EVIDENCE_BLOB_CONNECTION_STRING`, `RINDE_EVIDENCE_BLOB_CONTAINER`, `RINDE_EVIDENCE_MAX_SIZE_BYTES` con defecto `5242880` y límite multipart `10MB`).

##### Subida de foto de incidencia

- **Ruta y método:** `POST /api/v1/incidents/{id}/evidences` (`consumes = MediaType.MULTIPART_FORM_DATA_VALUE`).
- **Roles (`@PreAuthorize`):** `hasRole('DRIVER')`. Además, solo el conductor que reportó la incidencia puede adjuntar su evidencia (`incident.isReportedBy(userId)`).
- **Cuerpo de petición:** formulario multipart con el campo `file`. Mismos formatos (`JPEG`, `PNG`, `WEBP`, `PDF`) y tamaño máximo (5 MB por defecto) que Expense. Si ya existía una evidencia adjunta, la nueva la reemplaza y la anterior se elimina tras confirmar la transacción.
- **Respuesta (`201 Created`):** cabecera `Location: /api/v1/incidents/{id}/evidences/{evidenceId}/content` y cuerpo `IncidentResource` actualizado. `IncidentResource` agrega el campo `evidence` con la misma forma que `EvidenceResource` de Expense, manteniendo `evidenceUrl` como campo obsoleto y nullable:

```json
{
  "id": "3c589391-d520-4d01-838a-1a574c7d2bb6",
  "tripId": "87460614-42ea-4cd5-9de1-8f38d9496434",
  "vehicleId": "d6b9a0e4-1c2f-4f3a-9a77-3e1d2b5c8f10",
  "reportedBy": "764a4968-403f-40d7-9172-ba637d10fb16",
  "type": "BREAKDOWN",
  "description": "Falla en el freno neumático",
  "evidenceUrl": null,
  "evidence": {
    "id": "7d1c2f0e-3b8a-4d55-9a61-2e0f4c1b9a10",
    "fileName": "foto-freno.jpg",
    "contentType": "image/jpeg",
    "fileSizeBytes": 204800,
    "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "contentUrl": "/api/v1/incidents/3c589391-d520-4d01-838a-1a574c7d2bb6/evidences/7d1c2f0e-3b8a-4d55-9a61-2e0f4c1b9a10/content",
    "uploadedAt": "2026-11-20T13:25:00Z"
  },
  "status": "OPEN",
  "reportedAt": "2026-11-20T13:20:00Z",
  "resolution": null,
  "updatedAt": "2026-11-20T13:25:00Z"
}
```

Errores de subida (`POST /api/v1/incidents/{id}/evidences`):

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | Adjunte el comprobante en el campo file. | Falta el campo `file`. |
| 400 | El archivo del comprobante está vacío. | Archivo con 0 bytes. |
| 400 | Formato no permitido. Adjunte una foto en JPEG, PNG o WEBP, o un PDF. | Firma binaria distinta de JPEG, PNG, WEBP o PDF. |
| 403 | Solo el conductor que reportó la incidencia puede adjuntar su evidencia. | Otro conductor de la misma empresa. |
| 403 | No tiene permisos para realizar esta operación. | Rol distinto de `DRIVER`. |
| 404 | La incidencia no existe. | Incidencia inexistente o de otra empresa. |
| 413 | El comprobante supera el tamaño máximo permitido de 5 MB. | Archivo mayor a `RINDE_EVIDENCE_MAX_SIZE_BYTES`. |
| 413 | El comprobante supera el tamaño máximo permitido. | Solicitud mayor al límite del contenedor (`10 MB`). |

##### Lectura de foto de incidencia

- **Ruta y método:** `GET /api/v1/incidents/{id}/evidences/{evidenceId}/content` (`produces = MediaType.ALL_VALUE`).
- **Roles (`@PreAuthorize`):** `isAuthenticated()`; en el servicio se valida `ADMINISTRATOR`, `OPERATIONS_MANAGER` o el `DRIVER` que reportó la incidencia.
- **Respuesta (`200 OK`):** bytes del archivo con `Content-Type` real, `Content-Disposition: inline; filename="..."` y `Cache-Control: no-store`. Valida huella SHA-256 antes de responder.

Errores de lectura (`GET /api/v1/incidents/{id}/evidences/{evidenceId}/content`):

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 403 | No tiene permiso para ver esta evidencia. | Conductor que no reportó la incidencia. |
| 404 | La incidencia no existe. | Incidencia inexistente o de otra empresa. |
| 404 | La evidencia no existe. | La incidencia no tiene evidencia o el `evidenceId` no coincide. |
| 404 | El archivo del comprobante no está disponible en el almacenamiento. | Archivo faltante en disco o en Blob Storage. |
| 500 | Ocurrió un error inesperado. | La huella SHA-256 no coincide. |

#### Eventos y reparto S4-B vs responsable

- **Eventos:** ninguno.
- **S4-B:** agrega la dependencia de Azure Blob Storage al `pom.xml` de `expense` e `incident` si corresponde, configura las propiedades en `application.yml` y `.env.example`, crea la migración Flyway de evidencia en `services/incident`, actualiza `IncidentResource` con `evidence`, define el puerto propio de almacenamiento en `incident` y declara las firmas de los dos endpoints en `IncidentsController`.
- **Farid (S4-14):** implementa el adaptador Azure Blob Storage en `expense` y en `incident`, el adaptador local en `incident`, la lógica de subida y lectura en `IncidentCommandServiceImpl` / `IncidentQueryServiceImpl`, y las pruebas unitarias, integración y `.feature`.

---

### C5. Dispositivos del conductor y avisos push

- **Responsable y tarea:** Pedro (`S4-24`).
- **Servicio y módulo:** `core`, módulo `notification`.
- **Entrada en gateway:** `monolith-service` (`order: 12`, `/api/v1/**`).

#### Endpoints

1. **Registrar o actualizar dispositivo:**
   - **Ruta y método:** `POST /api/v1/notifications/devices`.
   - **Roles (`@PreAuthorize`):** `hasRole('DRIVER')`.
   - **Cuerpo de petición:**

```json
{
  "token": "fcm-device-token-ejemplo-01",
  "platform": "ANDROID"
}
```

   - **Comportamiento:** idempotente por `token` (asocia el token al `tenantId` y `userId` del JWT; si el token ya existía, actualiza su propietario y marca temporal de uso sin duplicar fila).
   - **Respuesta:** `204 No Content`.
   - **Errores:** `400` si `token` está vacío o supera el tamaño permitido, o si `platform` es nulo o no válido (`ANDROID`).

2. **Eliminar dispositivo:**
   - **Ruta y método:** `DELETE /api/v1/notifications/devices/{token}`.
   - **Roles (`@PreAuthorize`):** `hasRole('DRIVER')`.
   - **Comportamiento:** elimina el registro del `token` perteneciente al usuario autenticado (operación idempotente: responde `204` incluso si ya fue eliminado).
   - **Respuesta:** `204 No Content`.

#### Adaptador push (`PushAdapter`)

- Puerto existente en `core`: `com.nexus.rinde.notification.domain.services.PushAdapter` con método `void send(TripAssignmentNotice notice)`.
- Implementación existente: `UnavailablePushAdapter` (lanza `PushAdapterUnavailableException`).
- Implementación nueva: adaptador de Firebase Cloud Messaging (FCM).
- Selección por propiedad:
  - `rinde.push.provider` (`RINDE_PUSH_PROVIDER`): valores `unavailable` (por defecto) y `fcm`.
  - `rinde.push.fcm.credentials-json` (`RINDE_FCM_CREDENTIALS_JSON`): JSON de credenciales de cuenta de servicio de Firebase (variable privada, nunca en el repositorio).

#### Evento consumido

- **Routing key:** `trip.assigned` (exchange `rinde.events`, cola `notification.trip-assigned`, cola de errores `notification.trip-assigned.dead`).
- **Payload real (`TripAssigned` en `services/trip` / `TripAssignedNotification` en `core`):**

```json
{
  "eventId": "5f0c2d1e-8a7b-4c3d-9e2f-1a2b3c4d5e6f",
  "occurredAt": "2026-11-25T10:00:00Z",
  "tenantId": "d011f0a2-2b6f-4d32-9cb4-fa6d7cb81698",
  "tripId": "7b0e4f52-1c9a-4f3e-8d21-5a6c9e0b3d44",
  "code": "TRP-000014",
  "destination": "Trujillo",
  "departureDate": "2026-11-26",
  "vehicleId": "deac3d8e-12d4-4a72-98f2-1e2fc4d0d230",
  "driverId": "1355b6b8-7cd5-4f4f-9f66-7b883c7e8819"
}
```

- Idempotencia y reintentos: `TripAssignmentNotificationService` verifica `retryStoreRepository.existsById(event.eventId())` antes de preparar el aviso. Si la entrega falla, el viaje ya fue asignado y no se revierte; el aviso queda programado con `RetryPolicy` (`[1, 2, 4, 8, 16]` minutos). Para resolver el `userId` destinatario a partir del `driverId` del evento, `S4-B` agrega `userId` de forma aditiva al recurso interno de conductores de Fleet (`GET /api/internal/v1/fleet/tenants/{tenantId}/drivers`) y a `FleetDriverSummary` en `core`.

#### Reparto S4-B vs responsable

- **S4-B:** migración Flyway para la tabla de dispositivos del módulo `notification`, agregado aditivo de `userId` en el recurso interno de conductores de Fleet y en `FleetDriverSummary` de `core`, configuración condicional por `rinde.push.provider` (`unavailable` / `fcm`) en `NotificationConfiguration` y `application.yml`, y firma del controlador de dispositivos.
- **Pedro (S4-24):** implementa el registro y baja de tokens, el adaptador FCM con Firebase Admin SDK, la resolución del destinatario usando `userId` de `FleetDriverSummary` y las pruebas correspondientes.

---

### C6. Centro de notificaciones

- **Responsable y tarea:** Flor (`S4-25`).
- **Servicio y módulo:** `core`, módulo `notification`.
- **Entrada en gateway:** `monolith-service` (`order: 12`, `/api/v1/**`).
- **Roles (`@PreAuthorize`):** `hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')` en los cuatro endpoints.

#### Endpoints

1. **Listar notificaciones:**
   - `GET /api/v1/notifications?unreadOnly=false&page=0&size=20`
   - Parámetros opcionales: `unreadOnly` (boolean, por defecto `false`), `type` (uno o varios valores separados por coma entre `EXPENSE_PENDING_REVIEW`, `MAINTENANCE_DUE`, `INCIDENT_REPORTED` y `SETTLEMENT_READY`; sin `type` lista todos), `page` (int, por defecto `0`), `size` (int, por defecto `20`, máximo `100`).
   - Combinación de filtros: `type` se combina con `unreadOnly` (por ejemplo, `?type=INCIDENT_REPORTED&unreadOnly=true` lista solo incidencias no leídas). `GET /api/v1/notifications/unread-count` no cambia: cuenta todos los tipos para la campana.
   - Etiquetas en web por tipo: `EXPENSE_PENDING_REVIEW` `"Gastos por revisar"`, `MAINTENANCE_DUE` `"Mantenimiento"`, `INCIDENT_REPORTED` `"Incidencias"`, `SETTLEMENT_READY` `"Viajes por liquidar"`.
   - Orden: `createdAt` descendente.
   - Modelo de propiedad: la notificación pertenece a la empresa (`tenantId`) y el estado de lectura (`readAt`) se guarda por usuario (`userId`), de modo que marcar como leída afecta solo al usuario autenticado.
   - Respuesta `200 OK` (paginada según sección 2.1):

```json
{
  "items": [
    {
      "id": "a1b2c3d4-1111-4222-8333-444455556666",
      "type": "EXPENSE_PENDING_REVIEW",
      "title": "Gasto pendiente de revisión",
      "message": "Se registró un gasto por PEN 180.50 para revisar.",
      "link": "/expenses?tripId=87460614-42ea-4cd5-9de1-8f38d9496434",
      "createdAt": "2026-11-25T14:00:00Z",
      "readAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

2. **Contar notificaciones no leídas:**
   - `GET /api/v1/notifications/unread-count`
   - Respuesta `200 OK`:

```json
{
  "count": 3
}
```

3. **Marcar una notificación como leída:**
   - `PATCH /api/v1/notifications/{id}/read` (sin cuerpo).
   - Respuesta: `204 No Content`. Si la notificación no existe en la empresa del usuario: `404` (`"La notificación no existe."`). Si ya estaba leída por ese usuario, conserva su `readAt` original y responde `204`.

4. **Marcar todas las notificaciones como leídas:**
   - `POST /api/v1/notifications/read-all` (sin cuerpo).
   - Respuesta: `204 No Content`. Marca como leídas todas las notificaciones de la empresa que el usuario autenticado aún no ha leído.

#### Errores de negocio

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | El número de página no puede ser negativo. | `page < 0` en el listado |
| 400 | El tamaño de página debe estar entre 1 y 100. | `size < 1` o `size > 100` en el listado |
| 400 | El parámetro type contiene un tipo de aviso inválido. | Algún valor en `type` no pertenece a `EXPENSE_PENDING_REVIEW`, `MAINTENANCE_DUE`, `INCIDENT_REPORTED` o `SETTLEMENT_READY` |
| 404 | La notificación no existe. | El `id` no existe en la empresa al marcar como leída |

#### Tipos de notificación, eventos de RabbitMQ y enlaces

| `type` | Routing key en `rinde.events` | Estado del evento | `link` en web |
|---|---|---|---|
| `EXPENSE_PENDING_REVIEW` | `expense.registered` | Nuevo (lo publica `expense` tras commit) | `/expenses?tripId={tripId}` |
| `MAINTENANCE_DUE` | `fleet.maintenance-due` | Existe en `fleet` (`MaintenanceDue`) | `/fleet?vehicleId={vehicleId}` |
| `INCIDENT_REPORTED` | `incident.reported` | Existe en `incident` (`IncidentReported`) | `/incidents/{incidentId}` |
| `SETTLEMENT_READY` | `trip.finished` | Existe en `trip` (`TripFinished`) | `/trips/{tripId}` |

Campos reales de cada evento en `rinde.events`:

1. **`expense.registered` (nuevo, publicado por `expense` en `ExpenseCommandServiceImpl` después del commit):**
   - `eventId` (`UUID`), `tenantId` (`UUID`), `expenseId` (`UUID`), `tripId` (`UUID`), `driverId` (`UUID`), `amount` (`BigDecimal`), `currency` (`String`), `occurredAt` (`Instant`).
2. **`fleet.maintenance-due` (`MaintenanceDue` en `services/fleet`):**
   - `eventId` (`UUID`), `occurredAt` (`Instant`), `tenantId` (`UUID`), `vehicleId` (`UUID`), `plateNumber` (`String`), `maintenanceState` (`String`), `nextMaintenanceDate` (`LocalDate`).
3. **`incident.reported` (`IncidentReported` en `services/incident`):**
   - `eventId` (`UUID`), `occurredAt` (`Instant`), `tenantId` (`UUID`), `incidentId` (`UUID`), `tripId` (`UUID`), `vehicleId` (`UUID`), `reportedBy` (`UUID`), `incidentType` (`String`).
4. **`trip.finished` (`TripFinished` en `services/trip`):**
   - `eventId` (`UUID`), `occurredAt` (`Instant`), `tenantId` (`UUID`), `tripId` (`UUID`), `vehicleId` (`UUID`), `driverId` (`UUID`).

Cada consumidor de `notification` guarda el `eventId` recibido e ignora cualquier entrega repetida.

#### Reparto S4-B vs responsable

- **S4-B:** define el evento `ExpenseRegistered` y su publicación post-commit en `services/expense`, las migraciones Flyway de notificaciones de empresa y lecturas por usuario en `core`, y las firmas del controlador y records en `core`.
- **Flor (S4-25):** declara en `RabbitMessagingConfiguration` las colas `notification.expense-registered`, `notification.incident-reported` y `notification.trip-finished`, enlazadas a `rinde.events` con su routing key y cada una con su cola `.dead` en el exchange `rinde.notification.dead`, siguiendo el patrón de `notification.trip-assigned` (sin cambiar la cola de `fleet.maintenance-due`); implementa los listeners idempotentes por `eventId`, los servicios de comando y consulta del centro de notificaciones, pruebas unitarias, integración y `.feature`, y la campana en la web.

---

### C7. Perfil y configuración

- **Responsable y tarea:** Farid (`S4-28`).
- **Servicio:** `iam` (`services/iam`).
- **Entrada en gateway:** `iam-service` (`order: 1`, `/api/v1/tenants/**,/api/v1/users/**`).
- **Verificación de colisión de rutas:**
  - En `UsersController` (`@RequestMapping("/api/v1/users")`), existe `PATCH /{id}` donde `{id}` es `UUID`. En `CurrentUserController` (`@RequestMapping("/api/v1/users/me")`), existe `GET /api/v1/users/me`. Agregar `PATCH /api/v1/users/me` y `POST /api/v1/users/me/password` en `CurrentUserController` no choca con `PATCH /api/v1/users/{id}`, ya que Spring MVC da prioridad a la coincidencia literal `/me` y además `{id}` exige formato `UUID`.
  - En `TenantsController` (`@RequestMapping("/api/v1/tenants")`), existen `POST` (`/api/v1/tenants`) y `POST /{id}/verification`. Agregar `GET /api/v1/tenants/current` y `PATCH /api/v1/tenants/current` no choca con esas rutas. `TenantsController` tiene `@SecurityRequirements` a nivel de clase (público en Swagger), por lo que `/api/v1/tenants/current` va en un controlador propio `CurrentTenantController` con seguridad JWT en Swagger, y utiliza un recurso nuevo `CompanyProfileResource` (`{ id, tradeName, ruc, status, phone, address }`), dejando `TenantResource` sin cambios.

#### Endpoints

1. **Consultar empresa actual:**
   - `GET /api/v1/tenants/current`
   - **Roles (`@PreAuthorize`):** `isAuthenticated()` (cualquier rol de empresa: `ADMINISTRATOR`, `OPERATIONS_MANAGER`, `DRIVER`).
   - **Cambios de modelo:** `iam.tenants` agrega las columnas nullable `phone` (`VARCHAR(30)`) y `address` (`VARCHAR(200)`). `TenantResource` no cambia.
   - **Respuesta `200 OK` (`CompanyProfileResource`):**

```json
{
  "id": "3f6c1b9e-8a2d-4c57-9a43-0d2f6b1e7a10",
  "tradeName": "Transportes Andes SAC",
  "ruc": "20123456789",
  "status": "ACTIVE",
  "phone": "+51 1 445 8899",
  "address": "Av. Argentina 2450, Lima"
}
```

2. **Actualizar empresa actual:**
   - `PATCH /api/v1/tenants/current`
   - **Roles (`@PreAuthorize`):** `hasRole('ADMINISTRATOR')`.
   - **Cuerpo de petición:** todos los campos son opcionales; el `ruc` no se puede editar (si se envía en JSON se ignora o se rechaza según validación estándar, no se modifica).

```json
{
  "tradeName": "Transportes Andes del Perú SAC",
  "phone": "+51 999 888 777",
  "address": "Av. Nicolás Arriola 1200, La Victoria, Lima"
}
```

   - **Reglas de validación:** `tradeName`, si se envía, no puede estar en blanco y admite hasta 120 caracteres (igual que en `Tenant.java`); `phone` admite hasta 30 caracteres; `address` admite hasta 200 caracteres.
   - **Respuesta `200 OK` (`CompanyProfileResource`):** mismo formato de `GET /api/v1/tenants/current` con los valores actualizados.

3. **Actualizar perfil del usuario actual:**
   - `PATCH /api/v1/users/me`
   - **Roles (`@PreAuthorize`):** `isAuthenticated()` (cualquier rol de empresa).
   - **Cuerpo de petición:**

```json
{
  "fullName": "Ana Torres Vega"
}
```

   - **Reglas de validación:** `fullName` obligatorio (`@NotBlank`), máximo 150 caracteres (`@Size(max = 150)`, igual que en `User.java`).
   - **Respuesta `200 OK` (`CurrentUserResource`):** mismo formato que `GET /api/v1/users/me` (`id`, `fullName`, `email`, `role`, `status` y `tenant` con `TenantResource` sin `phone` ni `address`).

4. **Cambiar contraseña del usuario actual:**
   - `POST /api/v1/users/me/password`
   - **Roles (`@PreAuthorize`):** `isAuthenticated()` (cualquier rol de empresa).
   - **Cuerpo de petición:**

```json
{
  "currentPassword": "Clave-Actual-2026",
  "newPassword": "Clave-Nueva-2026"
}
```

   - **Política de contraseñas (citada desde `DefinePasswordResource.java` y `RegisterTenantResource.java`):** `@NotBlank @Size(min = 8, max = 72)` (entre 8 y 72 caracteres, donde 72 es el límite de BCrypt).
   - **Respuesta:** `204 No Content`.
   - **Errores:**
     - `400`: `"detail": "La contraseña actual no es correcta."` cuando `currentPassword` no coincide con `passwordHash` del usuario.
     - `400`: `"detail": "La nueva contraseña debe ser distinta de la actual."` cuando `newPassword` coincide con la contraseña vigente.
     - `400`: validación estándar (`"detail": "La solicitud tiene datos inválidos."`) si `newPassword` no cumple entre 8 y 72 caracteres o falta algún campo.

#### Eventos y reparto S4-B vs responsable

- **Eventos:** ninguno.
- **S4-B:** crea la migración Flyway de `iam.tenants` agregando `phone` y `address`, actualiza la entidad `Tenant` (manteniendo `TenantResource` intacto), crea `CompanyProfileResource` y `CurrentTenantController` con seguridad JWT en Swagger, y declara los endpoints nuevos en `CurrentUserController`.
- **Farid (S4-28):** implementa los comandos, validaciones (verificación BCrypt de `currentPassword` y rechazo si `newPassword` es igual a la actual), pruebas unitarias, integración y `.feature`, y conecta el panel `layout/account-settings` en la web.

---

### C8. Correo real

- **Responsable y tarea:** Flor (`S4-27`).
- **Servicio:** `iam` (`services/iam`), sin rutas nuevas.

#### Diseño del puerto y adaptadores

- **Estado actual:** cuando `rinde.demo.mailbox.enabled=true`, `DemoMailCapture` escucha por `@EventListener` (con `Propagation.MANDATORY`) los eventos `TenantRegistered`, `UserInvited` y `PasswordResetRequested` y los guarda cifrados con `DemoMailCipher` en `iam.demo_mail_messages`. Cuando el buzón está deshabilitado, `AccessTokenLoggingEventHandler` registra los enlaces en el log. `DemoMailCipher` prohíbe iniciar el servicio si los perfiles activos incluyen `prod` o `production` (`"El buzón de demostración no puede habilitarse con perfiles de producción."`).
- **Contrato nuevo:** se define un puerto de envío de correo en `iam` con dos adaptadores seleccionados por la propiedad `rinde.mail.provider`:
  - `demo` (valor por defecto): conserva exactamente el comportamiento actual; con `rinde.demo.mailbox.enabled=true` guarda los mensajes cifrados en `iam.demo_mail_messages` (y mantiene la prohibición en perfiles `prod` y `production`), y si está en `false` registra el enlace en el log mediante `AccessTokenLoggingEventHandler`.
  - `azure`: envía correos reales mediante Azure Communication Services Email. No instancia los componentes del buzón de demostración ni el registro de enlaces en el log.
  - **Validación en producción:** con los perfiles `prod` o `production`, `rinde.mail.provider` debe ser `azure`; si no lo es, el arranque del servicio falla con `"En producción el correo debe usar el proveedor azure."`, evitando que cualquier enlace de acceso quede en los logs de producción.
- **Variables de entorno nuevas:**
  - `rinde.mail.provider` (`RINDE_MAIL_PROVIDER`, por defecto `demo`; valores: `demo`, `azure`).
  - `RINDE_ACS_CONNECTION_STRING` (cadena de conexión de Azure Communication Services, requerida cuando el proveedor es `azure`).
  - `RINDE_MAIL_SENDER` (dirección de correo remitente verificada en Azure Communication Services).

#### Correos enviados, asuntos y enlaces

Los tres correos corresponden a los eventos publicados por `Tenant` y `User`, con los asuntos tomados de `DemoMailboxService.java` y los enlaces construidos con `WEB_BASE_URL` (`rinde.web.base-url`, por defecto `http://localhost:4200`) tal como en `AccessTokenLoggingEventHandler.java`:

| Evento de dominio | Campos del evento | Asunto en español | Enlace incluido y vigencia |
|---|---|---|---|
| `TenantRegistered` | `eventId`, `occurredAt`, `tenantId`, `tradeName`, `administratorEmail`, `verificationToken`, `tokenExpiresAt` | `Verifica tu empresa en Rinde` | `{WEB_BASE_URL}/verify-email?tenant={tenantId}&token={token}` (48 horas) |
| `UserInvited` | `eventId`, `occurredAt`, `tenantId`, `userId`, `fullName`, `email`, `role`, `invitationToken`, `tokenExpiresAt` | `Tu invitación a {companyName}` | `{WEB_BASE_URL}/accept-invitation?token={token}` (48 horas) |
| `PasswordResetRequested` | `eventId`, `occurredAt`, `tenantId`, `userId`, `email`, `resetToken`, `tokenExpiresAt` | `Recupera tu acceso a Rinde` | `{WEB_BASE_URL}/reset-password?token={token}` (1 hora) |

#### Reparto S4-B vs responsable

- **S4-B:** define el puerto de correo en `services/iam`, agrega las propiedades `rinde.mail.provider`, `RINDE_ACS_CONNECTION_STRING` y `RINDE_MAIL_SENDER` en `application.yml` y `.env.example`, y la dependencia del SDK de Azure Communication Services Email en `pom.xml`.
- **Flor (S4-27):** implementa el adaptador de Azure Communication Services Email, integra el adaptador `demo` con las reglas de habilitación y validación en `prod`/`production`, y agrega las pruebas unitarias e integración con dobles.

---

### C9. Gráficos del tablero

- **Responsable y tarea:** Gabriel (`S4-23`).
- **Servicio y controlador:** `core` (`DashboardController`).
- **Ruta y método:** `GET /api/v1/dashboard/charts`.
- **Entrada en gateway:** `monolith-service` (`order: 12`).
- **Roles (`@PreAuthorize`):** `hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')`.
- **Parámetros:**
  - `from` (`AAAA-MM-DD`, opcional).
  - `to` (`AAAA-MM-DD`, opcional). Sin fechas, del día 1 del mes actual a hoy.

#### Fuentes y reglas de cálculo

1. **Fuentes consultadas:** `HttpExpenseContextFacade` (`findByTenantId`), `HttpTripContextFacade` (`findByTenantId`) y `HttpFleetContextFacade` (`findVehiclesByTenantId`). Si alguna fuente falla con `RemoteServiceUnavailableException`, la respuesta marca `partial: true` y agrega el origen a `unavailableSources` (`EXPENSE`, `TRIP`, `FLEET`).
2. **Solo gastos aprobados en `PEN` del periodo:** los agregados de gastos (`expensesByCategory` y `expensesByVehicle`) consideran únicamente gastos con `status == "APPROVED"`, `currency == "PEN"` y `expenseDate` dentro de `[from, to]`. Los gastos aprobados en otra moneda dentro del periodo se excluyen de las sumas y sus códigos de moneda se informan sin repetir en `excludedCurrencies` (lista vacía `[]` si todos están en `PEN`).
3. **`expensesByCategory`:** siempre devuelve las 6 categorías de `ExpenseCategory` (`FUEL`, `TOLL`, `FOOD`, `LODGING`, `MAINTENANCE`, `OTHER`), con total `0` cuando no existan gastos aprobados en `PEN` en esa categoría.
4. **`tripsByStatus`:** siempre devuelve los 5 valores de `TripStatus` (`SCHEDULED`, `ASSIGNED`, `IN_ROUTE`, `FINISHED`, `SETTLED`), con `count: 0` si no hay viajes en ese estado.
5. **`expensesByVehicle`:** agrupa los gastos aprobados en `PEN` del periodo por el `vehicleId` del viaje asociado, incluye la placa (`plateNumber`) desde Fleet, ordena de mayor a menor `total` y limita la lista a un máximo de 10 vehículos. `S4-B` agrega `tripId` de forma aditiva a `InternalExpensesController.InternalExpenseResource` en `expense` y a `ExpenseSummary` en `core` (junto con sus dobles de prueba) para permitir este cruce.

#### Respuesta (`200 OK`)

```json
{
  "from": "2026-11-01",
  "to": "2026-11-30",
  "currency": "PEN",
  "excludedCurrencies": [],
  "expensesByCategory": [
    { "category": "FUEL", "total": 2450.00 },
    { "category": "TOLL", "total": 420.50 },
    { "category": "FOOD", "total": 310.00 },
    { "category": "LODGING", "total": 180.00 },
    { "category": "MAINTENANCE", "total": 0 },
    { "category": "OTHER", "total": 95.00 }
  ],
  "tripsByStatus": [
    { "status": "SCHEDULED", "count": 2 },
    { "status": "ASSIGNED", "count": 3 },
    { "status": "IN_ROUTE", "count": 2 },
    { "status": "FINISHED", "count": 4 },
    { "status": "SETTLED", "count": 5 }
  ],
  "expensesByVehicle": [
    {
      "vehicleId": "deac3d8e-12d4-4a72-98f2-1e2fc4d0d230",
      "plateNumber": "ABC-123",
      "total": 1890.50
    }
  ],
  "partial": false,
  "unavailableSources": []
}
```

#### Errores de negocio

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La fecha final no puede ser anterior a la inicial. | `from` posterior a `to` |
| 400 | La fecha de inicio debe tener el formato AAAA-MM-DD. | Formato inválido en `from` |
| 400 | La fecha de fin debe tener el formato AAAA-MM-DD. | Formato inválido en `to` |

#### Eventos y reparto S4-B vs responsable

- **Eventos:** ninguno.
- **S4-B:** agrega `tripId` de forma aditiva a `InternalExpenseResource` en `expense` y a `ExpenseSummary` en `core` (con sus dobles de prueba `ExpenseDouble`), define los records de respuesta de gráficos (incluyendo `excludedCurrencies`) y la firma del método en `DashboardController`.
- **Gabriel (S4-23):** implementa el servicio de consulta en `DashboardQueryServiceImpl` con cálculo de las tres series, exclusión de monedas distintas de `PEN`, degradación parcial, pruebas en `core` y los gráficos en la web.

---

### C10. Lo que usa el móvil

- **Responsable y tareas:** Pedro (`S4-07`, `S4-08`, `S4-09`, `S4-10`, más `S4-24` en C5).
- **Rutas nuevas del móvil:** únicamente subida de foto de incidencia en `C4` (`POST /api/v1/incidents/{id}/evidences`) y registro/baja de token push en `C5` (`POST /api/v1/notifications/devices` y `DELETE /api/v1/notifications/devices/{token}`). El resto son rutas ya existentes en `develop`.

#### Resumen de endpoints que consume el móvil (comportamiento vigente)

| Endpoint | Servicio | Entrada gateway | `@PreAuthorize` vigente | Validación de alcance para `DRIVER` |
|---|---|---|---|---|
| `GET /api/v1/trips/assigned-to-me` | `trip` | `trip-service` (8) | `hasRole('DRIVER')` | Resuelve `driverId` por `userId` en Fleet y lista solo sus viajes. |
| `GET /api/v1/trips/{id}` | `trip` | `trip-service` (8) | `hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER','DRIVER')` | Para `DRIVER`, valida `trip.isAssignedTo(driverId)`; si no coincide, responde `404`. |
| `POST /api/v1/trips/{tripId}/expenses` | `expense` | `expense-service` (6) | `hasRole('DRIVER')` | Exige rol `DRIVER`, resuelve su `driverId` en Fleet y valida asignación al viaje en Trip; si no coincide, responde `403` con `"Solo el conductor asignado al viaje puede registrar gastos."`. |
| `POST /api/v1/expenses/{expenseId}/evidences` | `expense` | `expense-service` (6) | `hasRole('DRIVER')` | Verifica `expense.isByDriver(userId)`; si es otro conductor responde `403`. |
| `POST /api/v1/expenses/sync` | `expense` | `expense-service` (6) | `hasRole('DRIVER')` | Exige rol `DRIVER` y consulta Fleet una sola vez por petición; si algún ítem corresponde a un viaje no asignado al conductor, se rechaza el ítem con código `TRIP_NOT_ASSIGNED` sin fallar el lote. |
| `GET /api/v1/trips/{tripId}/settlement` | `core` | `monolith-trip-children` (5) | `isAuthenticated()` | Un `DRIVER` solo consulta viajes asignados a él; si no, responde `404` con `"El viaje no existe."`, igual que `GET /api/v1/trips/{id}`. |
| `GET /api/v1/settlements/{id}` | `core` | `monolith-service` (12) | `isAuthenticated()` | Un `DRIVER` solo consulta liquidaciones de viajes asignados a él; si no, responde `404` con `"La liquidación no existe."`. |
| `GET /api/v1/settlements/{id}/export` | `core` | `monolith-service` (12) | `hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')` | Rol `DRIVER` recibe `403`. |
| `GET /api/v1/trips/{tripId}/advances` | `core` | `monolith-trip-children` (5) | `isAuthenticated()` | Un `DRIVER` solo consulta viajes asignados a él; si no, responde `404` con `"El viaje no existe."`, igual que `GET /api/v1/trips/{id}`. |
| `POST /api/v1/trips/{tripId}/incidents` | `incident` | `incident-service` (6) | `hasRole('DRIVER')` | Consulta Trip y Fleet; exige viaje `IN_ROUTE` y conductor asignado (`403` si no es el asignado). |

#### Detalle de petición, respuesta y errores por endpoint

1. **`GET /api/v1/trips/assigned-to-me`:**
   - Respuesta `200 OK`: lista de `TripSummaryResource` (vacía `[]` si no tiene viajes o si la cuenta no está vinculada a un conductor):

```json
[
  {
    "id": "7b0e4f52-1c9a-4f3e-8d21-5a6c9e0b3d44",
    "code": "TRP-000014",
    "origin": "Lima",
    "destination": "Trujillo",
    "departureDate": "2026-11-26",
    "vehicleId": "deac3d8e-12d4-4a72-98f2-1e2fc4d0d230",
    "driverId": "1355b6b8-7cd5-4f4f-9f66-7b883c7e8819",
    "status": "ASSIGNED"
  }
]
```

2. **`GET /api/v1/trips/{id}`:**
   - Respuesta `200 OK`: `TripDetailResource` con `statusChanges`:

```json
{
  "id": "7b0e4f52-1c9a-4f3e-8d21-5a6c9e0b3d44",
  "code": "TRP-000014",
  "origin": "Lima",
  "destination": "Trujillo",
  "cargoDescription": "Materiales de construcción",
  "cargoWeightKg": 4200.00,
  "departureDate": "2026-11-26",
  "status": "IN_ROUTE",
  "vehicleId": "deac3d8e-12d4-4a72-98f2-1e2fc4d0d230",
  "driverId": "1355b6b8-7cd5-4f4f-9f66-7b883c7e8819",
  "assignedAt": "2026-11-25T10:00:00Z",
  "overdueMaintenanceConfirmedBy": null,
  "startedAt": "2026-11-26T08:00:00Z",
  "finishedAt": null,
  "statusChanges": [
    {
      "status": "SCHEDULED",
      "changedAt": "2026-11-25T09:00:00Z",
      "changedBy": "9db235a4-3e8a-4c1b-8c65-a687c73b6a18"
    }
  ]
}
```

   - Error: `404` `"El viaje no existe."` (también cuando pertenece a otro conductor).

3. **`POST /api/v1/trips/{tripId}/expenses`:**
   - Petición: `{ "category": "FUEL", "amount": 150.00, "currency": "PEN", "expenseDate": "2026-11-26", "idempotencyKey": "EXP-MOBILE-0001" }`.
   - Respuesta `201 Created` (con `Location: /api/v1/expenses/{id}`): `ExpenseResource` en estado `PENDING_SUPPORT` y `evidence: null`.
   - Errores: `400` validación (`"El monto debe ser mayor a cero."`), `403` `"Solo el conductor asignado al viaje puede registrar gastos."` (o `"No tiene permisos para realizar esta operación."` si el rol no es `DRIVER`), `404` `"El viaje no existe."`, `409` `"El viaje todavía no ha iniciado."`, `"El viaje ya fue liquidado."` o `"La clave de idempotencia ya fue utilizada."`, `503` `"No se pudo consultar el estado del viaje en Trip."` o `"No se pudo consultar el conductor en Fleet."`.

4. **`POST /api/v1/expenses/{expenseId}/evidences`:**
   - Petición: `multipart/form-data`, campo `file` (JPEG, PNG, WEBP o PDF; máximo 5 MB por defecto).
   - Respuesta `201 Created` (con `Location: /api/v1/expenses/{expenseId}/evidences/{evidenceId}/content`): `ExpenseResource` con `status: "REGISTERED"` y `evidence` poblado (`contentUrl` incluido).
   - Errores: `400` (`"Adjunte el comprobante en el campo file."`, `"El archivo del comprobante está vacío."`, `"Formato no permitido. Adjunte una foto en JPEG, PNG o WEBP, o un PDF."`), `403` `"Solo el conductor que registró el gasto puede adjuntar su comprobante."`, `404` `"El gasto no existe."`, `409` `"El gasto se encuentra bloqueado por liquidación cerrada."`, `413`.

5. **`POST /api/v1/expenses/sync`:**
   - Petición: lista JSON de `SyncExpenseItemResource`: `[{ "tripId": "...", "category": "TOLL", "amount": 12.50, "currency": "PEN", "expenseDate": "2026-11-26", "idempotencyKey": "OFFLINE-EXP-001" }]`.
   - Respuesta `200 OK`: `SyncExpensesResponseResource` (`totalReceived`, `synchronizedCount`, `items` de `ExpenseResource`, `rejectedCount`, `rejected` con `{ idempotencyKey, reason, code }` donde `code` es `TRIP_NOT_FOUND`, `TRIP_NOT_ASSIGNED` (`"Solo el conductor asignado al viaje puede registrar gastos."`), `TRIP_NOT_STARTED`, `TRIP_SETTLED`, `INVALID_EXPENSE`, `DUPLICATE_KEY`).
   - Error de rol y disponibilidad: `403` `"No tiene permisos para realizar esta operación."` si el rol no es `DRIVER`; `503` si Trip o Fleet no responden para que el cliente mantenga el lote pendiente de reintento.

6. **`GET /api/v1/trips/{tripId}/settlement` y `GET /api/v1/settlements/{id}`:**
   - Respuesta `200 OK`: `SettlementResource` (`id`, `tenantId`, `tripId`, `driverId`, `status`, `advanceAmount`, `advanceCurrency`, `expenseTotalAmount`, `expenseTotalCurrency`, `advanceBalance`, `createdAt`, `updatedAt`).
   - Errores: en `/trips/{tripId}/settlement`, `404` `"No existe liquidación para este viaje."`, y para un `DRIVER` sobre un viaje que no le está asignado `404` `"El viaje no existe."`; en `/settlements/{id}`, `404` `"La liquidación no existe."` (incluyendo cuando pertenece a un viaje ajeno para `DRIVER`). `GET /api/v1/settlements/{id}/export` exige `hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')`.

7. **`GET /api/v1/trips/{tripId}/advances`:**
   - Respuesta `200 OK`: lista de `TripAdvanceResource` (`id`, `tripId`, `amount`, `currency`, `deliveryMethod`, `registeredBy`, `registeredAt`) ordenada por creación ascendente, o `[]` si el viaje no tiene adelantos.
   - Error: `404` `"El viaje no existe."` (también para un `DRIVER` sobre un viaje ajeno).

8. **`POST /api/v1/trips/{tripId}/incidents`:**
   - Petición: `{ "type": "BREAKDOWN", "description": "Falla en el freno neumático", "evidenceUrl": null, "idempotencyKey": "INC-OFFLINE-0001" }`.
   - Respuesta `201 Created` (con `Location: /api/v1/incidents/{id}`) la primera vez, o `200 OK` si el mismo usuario reenvía la misma `idempotencyKey`: devuelve `IncidentResource` (que en C4 agrega `evidence`).
   - Errores: `400` validación (`type` obligatorio, `description` obligatorio de hasta 500 caracteres), `403` `"Solo el conductor asignado al viaje puede reportar incidencias."`, `404` `"El viaje no existe."`, `409` `"Solo se puede reportar una incidencia cuando el viaje está en ruta."` o `"La clave de idempotencia ya fue utilizada."`, `503` `"No se pudo consultar el viaje en Trip."`.

#### Reparto S4-B vs responsable

- **S4-B:** deja sin cambios los endpoints actuales; las restricciones de rol y asignación del conductor en `C10` se implementan en `S4-04`.
- **Pedro (S4-04 y S4-07 a S4-10):** ajusta los permisos del conductor (`S4-04`) e implementa las pantallas, cámara/galería, compresión y cola sin conexión en `apps/mobile` consumiendo estos contratos.

---

### C11. Ubicación del camión en ruta

- **Responsable y tarea:** Pedro (`S4-33`).
- **Servicio y controladores:** `trip` (`TripsController` para escritura y `TripQueriesController` para consulta).
- **Entrada en gateway:** `trip-service` (`order: 8`, `Path=/api/v1/trips/**`).
- **Confirmación de rutas y no colisión:** tanto `POST /api/v1/trips/{id}/locations` como `GET /api/v1/trips/locations/latest` quedan cubiertos por `/api/v1/trips/**` sin modificar `gateway/src/main/resources/application.yml`. Ninguna entrada con `order` menor (`monolith-trip-children` en `order: 5`, `expense-service` en `order: 6` e `incident-service` en `order: 6`) captura `/locations` ni `/locations/latest`. En `TripQueriesController`, `GET /api/v1/trips/locations/latest` no colisiona con `GET /api/v1/trips/{id}` porque Spring MVC prioriza la coincidencia literal sobre la variable de ruta y `{id}` exige formato `UUID`.

#### 1. Registrar ubicaciones del viaje en ruta

- **Ruta y método:** `POST /api/v1/trips/{id}/locations`.
- **Roles (`@PreAuthorize`):** `hasRole('DRIVER')`.
- **Cuerpo de petición (`RecordTripLocationsResource`):** lista de 1 a 50 puntos enviados desde la cola del conductor.

```json
{
  "points": [
    {
      "latitude": -12.046374,
      "longitude": -77.042793,
      "accuracyMeters": 12.50,
      "recordedAt": "2026-11-28T14:30:00Z"
    }
  ]
}
```

- **Reglas de validación e idempotencia:**
  - `points`: obligatorio, de 1 a 50 elementos no nulos.
  - `latitude`: obligatorio, decimal entre `-90` y `90`. Se persiste redondeado a 6 decimales.
  - `longitude`: obligatorio, decimal entre `-180` y `180`. Se persiste redondeado a 6 decimales.
  - `accuracyMeters`: opcional (`nullable`), decimal mayor o igual a `0` cuando se envía. Se persiste redondeado a 2 decimales.
  - `recordedAt`: obligatorio, instante ISO-8601 UTC de captura en el dispositivo (`Instant`).
  - Solo se aceptan puntos si el viaje está en estado `IN_ROUTE` y pertenece al conductor autenticado (verificado mediante `FleetAvailabilityService.findDriverIdByUser`).
  - Un punto con el mismo `recordedAt` (precisión de microsegundos) para el mismo viaje no se guarda dos veces, tanto entre reenvíos de la cola como dentro del mismo lote.
- **Respuesta (`202 Accepted`, `RecordTripLocationsResponseResource`):**

```json
{
  "savedCount": 1,
  "duplicateCount": 0
}
```

- **Errores de negocio:**

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La solicitud tiene datos inválidos. | `points` vacío, mayor a 50, con elementos nulos o campos fuera de rango (`errors` detalla cada campo inválido) |
| 403 | No tiene permisos para realizar esta operación. | El rol del token no es `DRIVER` |
| 404 | El viaje no existe. | El viaje no existe en la empresa del token o no está asignado al conductor autenticado |
| 409 | Solo se registra la ubicación de un viaje en ruta. | El viaje asignado al conductor está en estado `ASSIGNED`, `FINISHED` o `SETTLED` |
| 503 | El servicio de flota no está disponible en este momento. Intente nuevamente más tarde. | Fleet no responde al resolver el conductor del token |

#### 2. Consultar la última ubicación de los viajes en ruta

- **Ruta y método:** `GET /api/v1/trips/locations/latest`.
- **Roles (`@PreAuthorize`):** `hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')`.
- **Comportamiento:** devuelve la última ubicación registrada (mayor `recordedAt`) para cada viaje en estado `IN_ROUTE` de la empresa autenticada, ordenado por código de viaje (`code`) ascendente. Excluye viajes en estados `SCHEDULED`, `ASSIGNED`, `FINISHED` y `SETTLED`. Como `trip` almacena `vehicleId` y no guarda la placa del vehículo ni consulta a Fleet en esta lectura, la respuesta expone `vehicleId`. Un viaje `IN_ROUTE` sin puntos registrados aparece con `latitude`, `longitude`, `accuracyMeters` y `recordedAt` en `null`, para que la web muestre "Ubicación no compartida". No se calculan porcentaje de avance ni llegada estimada.
- **Respuesta (`200 OK`, lista de `LatestTripLocationResource`):**

```json
[
  {
    "tripId": "7b0e4f52-1c9a-4f3e-8d21-5a6c9e0b3d44",
    "code": "TRP-000014",
    "vehicleId": "deac3d8e-12d4-4a72-98f2-1e2fc4d0d230",
    "latitude": -12.046374,
    "longitude": -77.042793,
    "accuracyMeters": 12.50,
    "recordedAt": "2026-11-28T14:30:00Z"
  },
  {
    "tripId": "9a1f2c3d-4e5b-4a6c-8d7e-9f0a1b2c3d4e",
    "code": "TRP-000015",
    "vehicleId": "ab12cd34-56ef-4789-90ab-cdef12345678",
    "latitude": null,
    "longitude": null,
    "accuracyMeters": null,
    "recordedAt": null
  }
]
```

- **Errores de negocio:** `403` `"No tiene permisos para realizar esta operación."` cuando el rol no es `ADMINISTRATOR` ni `OPERATIONS_MANAGER`.
- **Eventos:** ninguno.

---

## 4. Qué deja listo la tarea de bases (S4-B)

| Contrato / Tarea | Qué deja listo la tarea de bases (S4-B) | Qué implementa el responsable |
|---|---|---|
| C1. Resumen del viaje (Gabriel, S4-11) | Record ampliado `TripSummaryResource` en `core`; método `findOpenByTenantId` en `IncidentContextFacade` y `HttpIncidentContextFacade` sobre `/open`; ampliación de la fachada local `SettlementContextFacade` (`settlementId`, `settlementClosedAt` y `advanceDeliveryMethod` leyendo `TripAdvance`). | Lógica de consolidación en `DashboardQueryServiceImpl`, manejo de `partial` y `unavailableSources`, pruebas en `core` y pantalla consolidada en web. Al separar Settlement (`S4-02`), convierte `SettlementContextFacade` en cliente HTTP y agrega `SETTLEMENT` a `unavailableSources`. |
| C2. Bandeja de gastos (Farid, S4-12) | Record de página en `services/expense`; query con filtros (`status`, `tripId`, `driverId`, `from`, `to`, `page`, `size`); firma de `GET /api/v1/expenses` en `ExpensesController`. | Consulta filtrada y ordenada en `ExpenseRepository` y `ExpenseQueryServiceImpl`, validaciones explícitas con `BusinessRuleException`, pruebas y pantalla de bandeja en web. |
| C3. Historial del vehículo (Andree, S4-13) | Endpoint interno `GET /api/internal/v1/incidents/tenants/{tenantId}?vehicleId={id}` en `services/incident` con `description` aditivo en `InternalIncidentResource` (afecta también a `/open`); propiedades `TRIP_SERVICE_URL` e `INCIDENT_SERVICE_URL` y clientes HTTP hacia Trip e Incident en `services/fleet`; records (`items[].status` nullable) y firma de `GET /api/v1/vehicles/{id}/history` en `VehiclesController`. | Consulta de mantenimientos, cruce con viajes e incidencias, degradación parcial (`partial` / `unavailableSources`), pruebas en `fleet` y vista de ficha e historial en web. |
| C4. Archivos en Azure Blob Storage (Farid, S4-14) | Dependencia de Azure Storage Blob; propiedades `rinde.evidence.storage.type`, `RINDE_EVIDENCE_BLOB_CONNECTION_STRING` y `RINDE_EVIDENCE_BLOB_CONTAINER` en `expense` e `incident`; puerto propio de almacenamiento en `incident`; migración Flyway de evidencia en `incident`; `IncidentResource` con `evidence` y firmas de ambos endpoints en `IncidentsController`. | Adaptador `azure-blob` en `expense`; adaptadores `local` y `azure-blob` en `incident`; lógica de subida, reemplazo, borrado post-commit y lectura autorizada en `incident`; pruebas en ambos servicios. |
| C5. Dispositivos y avisos push (Pedro, S4-24) | Migración Flyway de dispositivos en `core` (`notification`); campo `userId` aditivo en el recurso interno de conductores de Fleet y en `FleetDriverSummary` en `core`; configuración condicional por `rinde.push.provider` (`unavailable` / `fcm`) y variable `RINDE_FCM_CREDENTIALS_JSON`; firmas de `POST /api/v1/notifications/devices` y `DELETE /api/v1/notifications/devices/{token}`. | Gestión de dispositivos, adaptador FCM, resolución del destinatario usando `userId` de `FleetDriverSummary`, pruebas y consumo en el cliente Android. |
| C6. Centro de notificaciones (Flor, S4-25) | Evento `ExpenseRegistered` y publicación post-commit en `expense`; migraciones Flyway de notificaciones y lecturas en `core`; records de página y notificación y firmas de los 4 endpoints en `core`. | Colas `notification.expense-registered`, `notification.incident-reported` y `notification.trip-finished` con sus colas `.dead`; consumidores idempotentes por `eventId`, creación de notificaciones, consultas y marcado como leído por usuario, pruebas en `core` y `expense`, y campana en web. |
| C7. Perfil y configuración (Farid, S4-28) | Migración Flyway en `iam.tenants` (`phone`, `address`); actualización de `Tenant` manteniendo `TenantResource` sin cambios; nuevo recurso `CompanyProfileResource` y controlador `CurrentTenantController` con seguridad JWT en Swagger; firmas y DTO de `GET/PATCH /api/v1/tenants/current`, `PATCH /api/v1/users/me` (`CurrentUserResource`) y `POST /api/v1/users/me/password`. | Casos de uso de actualización de empresa, nombre de usuario y cambio de contraseña (rechazando contraseña igual a la actual), validaciones y errores, pruebas en `iam` y pantalla en web (`account-settings`). |
| C8. Correo real (Flor, S4-27) | Puerto de envío de correo en `iam`; propiedades `rinde.mail.provider` (`demo` / `azure`), `RINDE_ACS_CONNECTION_STRING` y `RINDE_MAIL_SENDER`; dependencia de Azure Communication Services Email en `pom.xml`. | Adaptador de Azure Communication Services Email, integración del adaptador `demo` sin instanciarse con `azure` y validación obligatoria de `azure` en `prod`/`production`, más pruebas con dobles en `iam`. |
| C9. Gráficos del tablero (Gabriel, S4-23) | Campo `tripId` aditivo en `InternalExpenseResource` (`expense`), `ExpenseSummary` (`core`) y sus dobles de prueba; records de respuesta (incluyendo `excludedCurrencies`) y firma de `GET /api/v1/dashboard/charts` en `DashboardController`. | Agregación por categoría, estado y top 10 de vehículos solo en `PEN`, listado de `excludedCurrencies`, manejo de `partial`, pruebas en `core` y gráficos en web. |
| C10. Lo que usa el móvil (Pedro, S4-07 a S4-10) | Sin rutas nuevas (aplica C4 y C5). | En `S4-04`, restricción de consultas de liquidación/adelantos al conductor asignado (`404` en `/trips/{tripId}/settlement`, `/trips/{tripId}/advances` y `/settlements/{id}`), cierre de `/settlements/{id}/export` para `ADMINISTRATOR` y `OPERATIONS_MANAGER` (`403` para `DRIVER`), y exigencia de `DRIVER` asignado en registro y sincronización de gastos (`403` / `TRIP_NOT_ASSIGNED`). En `S4-07` a `S4-10`, pantallas, foto, cola offline, saldo y liquidación en `apps/mobile`. |

---

## 5. Decisiones tomadas

1. **Mensaje de periodo invertido (Convenciones 2.3, C2, C3, C9):** se conserva en todos los endpoints el texto existente `"La fecha final no puede ser anterior a la inicial."`, manteniendo `GET /api/v1/dashboard/metrics` sin cambios.
2. **Ruta interna de incidencias por vehículo (C3):** sigue la convención de `InternalIncidentsController` como `GET /api/internal/v1/incidents/tenants/{tenantId}?vehicleId={id}`, con `vehicleId` obligatorio.
3. **Liquidación y adelantos en `C1`:** `advanceDeliveryMethod` corresponde al medio del adelanto registrado más recientemente y se devuelve aunque aún no exista liquidación; `S4-B` amplía `SettlementContextFacade` para exponer `settlementId`, `settlementClosedAt` y ese medio leyendo `TripAdvance`.
4. **Campo `tripId` en gasto interno (C9):** `S4-B` agrega `tripId` de forma aditiva a `InternalExpenseResource` en `expense` y a `ExpenseSummary` en `core`, junto con sus dobles de prueba.
5. **Monedas en `C9`:** solo se suman gastos aprobados en `PEN`; los de otra moneda se excluyen y sus códigos se devuelven en el campo `excludedCurrencies`.
6. **Detalle de incidencias y mantenimiento en `C3`:** `S4-B` agrega `description` de forma aditiva a `InternalIncidentResource` (también en `/open`); `items[].detail` de una incidencia usa `description`, `items[].status` es `null` en mantenimientos y `nextMaintenanceDate` es `null` si el vehículo no tiene mantenimientos.
7. **Recursos y validaciones en `C7`:** `TenantResource` no cambia; `GET/PATCH /api/v1/tenants/current` usan `CompanyProfileResource` en `CurrentTenantController` con seguridad JWT en Swagger; `PATCH /api/v1/users/me` devuelve `CurrentUserResource`; `POST /api/v1/users/me/password` rechaza repetir la contraseña actual con `400` y `"La nueva contraseña debe ser distinta de la actual."`.
8. **Paginación de plataforma (Convenciones 2.1):** los endpoints previos de plataforma conservan `PageResource` de `shared-kernel` sin cambios.
9. **Permisos del conductor en `C10`:** se corrigen en `S4-04` (`404` `"El viaje no existe."` en liquidación por viaje y adelantos de viajes ajenos, `404` `"La liquidación no existe."` en `GET /api/v1/settlements/{id}` para viajes ajenos, `hasAnyRole('ADMINISTRATOR','OPERATIONS_MANAGER')` en `GET /api/v1/settlements/{id}/export`, y `hasRole('DRIVER')` con asignación al viaje en gastos con `403` `"Solo el conductor asignado al viaje puede registrar gastos."` o rechazo `TRIP_NOT_ASSIGNED` en `sync`).
10. **Mapeo de `driverId` a `userId` para push (C5):** `S4-B` agrega `userId` de forma aditiva al recurso interno de conductores de Fleet y a `FleetDriverSummary` en `core` para resolver el destinatario.
11. **Mensajes `400` de parámetros inválidos (Convenciones 2.1, 2.3, C2, C3, C6, C9):** se usa `BusinessRuleException` con mensajes explícitos para fechas (`"La fecha de inicio debe tener el formato AAAA-MM-DD."`, `"La fecha de fin debe tener el formato AAAA-MM-DD."`), estado en `C2` (`"El parámetro status contiene un estado de gasto inválido."`) y paginación (`"El número de página no puede ser negativo."`, `"El tamaño de página debe estar entre 1 y 100."`).
12. **Convivencia de `C1` con la separación de Settlement (`S4-02`):** `S4-B` prepara solo la fachada local actual; al separar Settlement en `S4-02`, Gabriel convierte `SettlementContextFacade` en cliente HTTP y agrega `SETTLEMENT` a `unavailableSources` si no responde.
13. **Proveedor de correo (`C8`):** `demo` conserva el comportamiento actual (buzón si `rinde.demo.mailbox.enabled=true` o log si está en `false`); `azure` no instancia buzón ni log de enlaces; en perfiles `prod` o `production` se exige `rinde.mail.provider=azure` o el arranque falla con `"En producción el correo debe usar el proveedor azure."`.
14. **Filtro por tipo de aviso en el centro de notificaciones (`C6`):** `GET /api/v1/notifications` admite el parámetro opcional `type` (uno o varios valores CSV entre `EXPENSE_PENDING_REVIEW`, `MAINTENANCE_DUE`, `INCIDENT_REPORTED` y `SETTLEMENT_READY`), combinable con `unreadOnly` y con rechazo `400` `"El parámetro type contiene un tipo de aviso inválido."`; `/unread-count` no cambia.
15. **Ubicación del camión en ruta (`C11`):** `POST /api/v1/trips/{id}/locations` recibe un objeto `{ "points": [...] }` (1 a 50 puntos) para conservar el formato RFC 7807 con mapa `errors` por campo en `GlobalExceptionHandler`; deduplica por `(trip_id, recorded_at)` en microsegundos; `GET /api/v1/trips/locations/latest` devuelve `vehicleId` (ya que `trip` no almacena placas ni consulta Fleet en esta lectura) y campos de ubicación `null` para viajes `IN_ROUTE` sin puntos registrados.
