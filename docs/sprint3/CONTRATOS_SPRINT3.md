# Contratos de la API del Sprint 3

Documento de trabajo del equipo para las tareas de Andree, Farid, Gabriel y Flor (`TAREAS_AVANCE4.md`). La fuente es el código de `Codigo/rinde-backend` en develop 2cc8806, leído el 09/11/2026. Los reportes de `Sprint3_Avance4/Borradores/Fase0/` se contrastaron con el código; cuando difieren, manda el código y la diferencia queda en la sección 7.

Las rutas se llaman por el gateway en `http://localhost:8000`. Las rutas `/api/internal/**` son entre servicios: no pasan por el gateway y no se usan desde la web ni desde Postman. Los ejemplos JSON son ilustrativos: los nombres de campos, roles, rutas y mensajes de error son los que devuelve el código. Los identificadores son de ejemplo.

## 1. Cómo levantar el entorno

1. Clonar `nexus-upc/rinde-services` y cambiar a develop. Cada tarea sale de develop en su propia rama, como indica `TAREAS_AVANCE4.md`.
2. Preparar `.env.compose` en la raíz del monorepo con valores propios, como indica la sección 3. Ese archivo contiene secretos y no se sube a Git.
3. Levantar el entorno con `docker compose --env-file .env.compose up -d --build`. El compose crea un PostgreSQL por servicio (core, iam, trip, fleet, expense e incident), RabbitMQ y los servicios. Los puertos HTTP escuchan solo en 127.0.0.1: gateway 8000, core 8080, IAM 8081, Trip 8082, Fleet 8083, Expense 8084 e Incident 8085.
4. Swagger reunido del gateway: `http://localhost:8000/swagger-ui.html`.
5. Comprobación rápida: `GET http://localhost:8000/api/v1/users/me` sin token responde 401 con "Se requiere un token de acceso válido.".
6. `tests/deployment/verify.ps1` crea un entorno temporal y lo elimina. No sustituye al entorno de trabajo.

Settlement y Subscriptions siguen dentro de core (puerto 8080) hasta que se separen en S3-10a y S3-10b.

## 2. Sesión, roles y errores comunes

Inicio de sesión: `POST /api/v1/auth/sign-in` devuelve el token. Cada petición protegida lleva la cabecera `Authorization: Bearer <accessToken>`. La respuesta indica la vigencia del token en `expiresInSeconds`.

Roles de empresa: `ADMINISTRATOR`, `OPERATIONS_MANAGER` (responsable de operaciones) y `DRIVER`. El dueño de la plataforma (`PLATFORM_OWNER`) tiene cuenta y token propios (sección 4.4.3).

Formato de error, igual en todos los servicios (RFC 7807):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "El vehículo no existe.",
  "traceId": "<uuid>"
}
```

- Validación de datos (400): `detail` es "La solicitud tiene datos inválidos." y `errors` trae un mensaje por campo.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "La solicitud tiene datos inválidos.",
  "errors": { "plateNumber": "La placa es obligatoria." },
  "traceId": "<uuid>"
}
```

- Sin token o con token no válido (401): "Se requiere un token de acceso válido.".
- Rol sin permiso (403): "No tiene permisos para realizar esta operación.". Si la empresa está suspendida, el filtro responde 403 con "El acceso de la empresa está suspendido.".
- Error no esperado (500): "Ocurrió un error inesperado.", con el detalle solo en el log.

Las tablas de cada contrato listan solo los errores de negocio. Los de rol y de token se resumen en esta sección.

## 3. Datos de prueba de tu entorno

Cada integrante levanta su propio entorno y crea sus propios datos de prueba. No hay cuentas compartidas: las contraseñas, las claves y los archivos `.env.compose` y `.env.demo-company` son de cada uno y no se suben a Git ni se pegan en la colección, en el chat o en las capturas.

1. Copiar `infra/.env.example` a `.env.compose` en la raíz de rinde-services y completarlo con valores propios: contraseñas locales de PostgreSQL y RabbitMQ (`POSTGRES_PASSWORD` y `RABBITMQ_PASSWORD`); `JWT_SECRET` y `RINDE_PLATFORM_JWT_SECRET`, distintos entre sí y de al menos 32 caracteres; `RINDE_BILLING_SIMULATION_WEBHOOK_SECRET`, de al menos 32 caracteres, que debe ser la misma clave que la colección usa como `webhookSecret`; `RINDE_PLATFORM_BOOTSTRAP_ENABLED=true`, con un correo y una contraseña propios en `RINDE_PLATFORM_BOOTSTRAP_EMAIL` y `RINDE_PLATFORM_BOOTSTRAP_PASSWORD` para el dueño de la plataforma; `RINDE_DEMO_MAILBOX_ENABLED=true`, y una clave propia en `RINDE_DEMO_MAILBOX_ENCRYPTION_KEY`. Esa clave tiene 64 caracteres hexadecimales (32 bytes), por ejemplo generada con `openssl rand -hex 32` desde Git Bash. La regla de formato está en services/iam/README.md. No compartan ni suban ese archivo.
2. Levantar los servicios desde la raíz de rinde-services con `docker compose --env-file .env.compose up -d --build` y esperar hasta que `docker compose ps` muestre todos los servicios en ejecución y los que tienen comprobación de salud como healthy.
3. En rinde-apps, desde la raíz de ese repositorio (la carpeta que contiene apps/), ejecutar con Node 18 o superior:

```
$env:RINDE_TEST_ENV="..\rinde-backend\.env.compose"
node apps/web/tests/demo-company/seed.cjs
```

La ruta de `RINDE_TEST_ENV` se resuelve desde la carpeta en la que se ejecuta el comando. La ruta del archivo de cuentas, en cambio, se calcula desde la ubicación del script: se escribe en `rinde-backend/.env.demo-company`, junto a rinde-apps. Por eso las dos carpetas deben estar al mismo nivel y la de rinde-services debe llamarse rinde-backend. Si no es así, cambien la ruta de `RINDE_TEST_ENV` o definan `RINDE_DEMO_ACCOUNT` con la ruta del archivo de cuentas.

El script usa la API del gateway en http://localhost:8000 y hace lo siguiente:

- Entra como dueño de la plataforma con los valores `RINDE_PLATFORM_BOOTSTRAP_*` de `.env.compose` y lee el buzón de demostración para obtener los enlaces de verificación e invitación.
- Crea la empresa Transportes Andes Demo con un sufijo aleatorio, un RUC aleatorio que empieza por 20 y un administrador con el correo `demo-<sufijo>@example.test`, y la verifica.
- Crea cuatro vehículos (ABC-123, BXT-604, CMR-218 y DEF-456).
- Crea cuatro conductores (Luis Quispe, Jorge Salas, Rosa Medina y Marco Ríos) con correos `conductorN-<sufijo>@example.test`, acepta sus invitaciones y crea su ficha vinculada a la cuenta.
- Crea siete viajes: dos en ruta, uno finalizado, dos asignados y dos programados sin unidad.

Al terminar, escribe `.env.demo-company` con el correo y la contraseña del administrador (`RINDE_DEMO_ADMIN_EMAIL` y `RINDE_DEMO_ADMIN_PASSWORD`) y el correo del primer conductor (`RINDE_DEMO_DRIVER_EMAIL`). Los conductores usan la misma contraseña que el administrador. La consola solo muestra el correo, el nombre de la empresa y la ruta del archivo.

4. Pasar esos valores a las variables de la colección en el entorno local de Postman: `adminEmail` y `adminPassword` con `RINDE_DEMO_ADMIN_EMAIL` y `RINDE_DEMO_ADMIN_PASSWORD`; `driverEmail` con `RINDE_DEMO_DRIVER_EMAIL`, y `driverPassword` con la misma contraseña del administrador. La colección no crea viajes ni vincula cuentas, así que depende de estos datos.

Las fechas de los ejemplos son de noviembre de 2026. Ajustarlas si la prueba se hace en otra fecha.

## 4. Contratos por tarea

### 4.1. Andree: flota web (S3-01, S3-02 y S3-07)

| Ruta | Método | Rol | Uso |
|---|---|---|---|
| `/api/v1/vehicles` | GET | Cualquier autenticado | Listado de vehículos (S3-01) |
| `/api/v1/vehicles` | POST | ADMINISTRATOR, OPERATIONS_MANAGER | Alta de vehículo (S3-01) |
| `/api/v1/vehicles/{id}` | GET | Cualquier autenticado | Ficha del vehículo (S3-01) |
| `/api/v1/vehicles/{id}/maintenance-status` | GET | Cualquier autenticado | Estado de mantenimiento en la ficha (S3-07) |
| `/api/v1/vehicles/{id}/health-status` | GET | Cualquier autenticado | Mismo resultado que maintenance-status |
| `/api/v1/vehicles/{id}/maintenances` | POST | ADMINISTRATOR, OPERATIONS_MANAGER | Registro de mantenimiento desde la ficha (S3-07) |
| `/api/v1/maintenances` | POST | ADMINISTRATOR, OPERATIONS_MANAGER | Registro de mantenimiento con vehicleId en el cuerpo (S3-07) |
| `/api/v1/maintenances/alerts` | GET | Cualquier autenticado | Alertas de mantenimiento (S3-07) |
| `/api/v1/drivers` | GET | Cualquier autenticado | Listado de conductores (S3-02) |
| `/api/v1/drivers` | POST | ADMINISTRATOR, OPERATIONS_MANAGER | Alta de conductor (S3-02) |
| `/api/v1/drivers/{id}` | GET | Cualquier autenticado | Detalle del conductor (S3-02) |
| `/api/v1/drivers/{id}/eligibility` | GET | Cualquier autenticado | Habilitación del conductor (S3-02) |
| `/api/v1/users` | GET | ADMINISTRATOR | Cuentas de la empresa para el selector (S3-02) |

#### 4.1.1. Vehículos

`GET /api/v1/vehicles` devuelve una lista de `VehicleResource`:

```json
[
  {
    "id": "<uuid>",
    "plateNumber": "PX123",
    "brand": "Volvo",
    "model": "FH 500",
    "modelYear": 2021,
    "payloadCapacityKg": 18000.50,
    "status": "AVAILABLE",
    "createdAt": "2026-11-09T14:00:00Z"
  }
]
```

`POST /api/v1/vehicles`. Request:

```json
{
  "plateNumber": "PX123",
  "brand": "Volvo",
  "model": "FH 500",
  "modelYear": 2021,
  "payloadCapacityKg": 18000.50
}
```

Respuesta 201, con cabecera `Location: /api/v1/vehicles/{id}`: el mismo `VehicleResource` con `status` en `AVAILABLE`. Los estados posibles son `AVAILABLE`, `IN_TRIP` e `IN_MAINTENANCE`.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La solicitud tiene datos inválidos. | Faltan campos. `errors` trae "La placa es obligatoria.", "La marca es obligatoria.", "El modelo es obligatorio.", "El año del modelo es obligatorio.", "La capacidad de carga es obligatoria." o "La capacidad de carga debe ser positiva." |
| 400 | La placa de rodaje no es válida. | Placa con menos de tres caracteres |
| 403 | La suscripción está vencida. Regularice el pago para programar viajes o registrar unidades. | La empresa está restringida (RESTRICTED) |
| 409 | La placa ya se encuentra registrada en la empresa. | Placa repetida. Es la prueba de placa repetida de S3-01 |
| 409 | El plan alcanzó su límite de N unidades. Seleccione un plan con mayor capacidad desde /api/v1/plans. | La empresa llegó al límite de su plan |

`GET /api/v1/vehicles/{id}` devuelve el `VehicleResource`. Si no existe en la empresa: 404 "El vehículo no existe.".

`GET /api/v1/vehicles/{id}/maintenance-status` (alias `health-status`):

```json
{
  "vehicleId": "<uuid>",
  "plateNumber": "PX123",
  "vehicleStatus": "AVAILABLE",
  "maintenanceState": "DUE_SOON",
  "nextMaintenanceDate": "2026-11-15",
  "availableForTrip": true
}
```

`maintenanceState` vale `UP_TO_DATE`, `DUE_SOON` u `OVERDUE`. `availableForTrip` es falso si el vehículo no está disponible o si el mantenimiento está vencido. Error: 404 "El vehículo no existe.".

#### 4.1.2. Mantenimiento y alertas

`POST /api/v1/vehicles/{id}/maintenances` y `POST /api/v1/maintenances` aceptan el mismo cuerpo. En la ruta por vehículo, el `vehicleId` del cuerpo no se usa; en `/api/v1/maintenances` debe enviarse.

```json
{
  "vehicleId": "<uuid>",
  "maintenanceType": "PREVENTIVE",
  "executionDate": "2026-11-09",
  "mileage": 125000,
  "cost": 350.00,
  "nextMaintenanceDate": "2027-02-09",
  "notes": "Cambio de aceite y filtros"
}
```

Respuesta 201, con `Location: /api/v1/maintenances/{id}`:

```json
{
  "id": "<uuid>",
  "vehicleId": "<uuid>",
  "maintenanceType": "PREVENTIVE",
  "executionDate": "2026-11-09",
  "mileage": 125000,
  "cost": 350.00,
  "nextMaintenanceDate": "2027-02-09",
  "notes": "Cambio de aceite y filtros",
  "createdAt": "2026-11-09T14:10:00Z"
}
```

El tipo de mantenimiento se guarda en mayúsculas.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La solicitud tiene datos inválidos. | `errors` trae "El tipo de mantenimiento es obligatorio.", "La fecha de ejecución es obligatoria.", "El costo es obligatorio.", "El costo no puede ser negativo." o "La fecha del próximo mantenimiento es obligatoria." |
| 400 | La fecha del próximo mantenimiento no puede ser anterior a la fecha de ejecución. | Fechas en orden inverso |
| 404 | El vehículo no existe. | Vehículo de otra empresa o inexistente |

`GET /api/v1/maintenances/alerts` devuelve solo las unidades en `DUE_SOON` u `OVERDUE`:

```json
[
  {
    "vehicleId": "<uuid>",
    "plateNumber": "PX123",
    "state": "DUE_SOON",
    "nextMaintenanceDate": "2026-11-15",
    "message": "La unidad tiene un mantenimiento próximo."
  }
]
```

Si el estado es `OVERDUE`, el mensaje es "La unidad tiene el mantenimiento vencido.". No hay endpoint de historial del vehículo (duda 5).

#### 4.1.3. Conductores

`GET /api/v1/drivers` devuelve una lista de `DriverResource`. `GET /api/v1/drivers/{id}` devuelve un `DriverResource`; si no existe, 404 "El conductor no existe.".

`POST /api/v1/drivers`. Request:

```json
{
  "userId": null,
  "fullName": "Luis Quispe Mamani",
  "documentType": "DNI",
  "documentNumber": "71234567",
  "licenseNumber": "Q12345678",
  "licenseCategory": "A-IIb",
  "licenseExpirationDate": "2030-12-31"
}
```

`userId` es opcional. Hoy el servidor no comprueba que la cuenta tenga rol `DRIVER` ni que no esté vinculada a otro conductor.

Respuesta 201, con `Location: /api/v1/drivers/{id}`:

```json
{
  "id": "<uuid>",
  "userId": null,
  "fullName": "Luis Quispe Mamani",
  "documentType": "DNI",
  "documentNumber": "71234567",
  "licenseNumber": "Q12345678",
  "licenseCategory": "A-IIb",
  "licenseExpirationDate": "2030-12-31",
  "status": "ENABLED",
  "createdAt": "2026-11-09T14:12:00Z"
}
```

Si la licencia ya venció al registrarlo, el conductor queda `DISABLED`.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La solicitud tiene datos inválidos. | `errors` trae "Los nombres del conductor son obligatorios.", "El tipo de documento es obligatorio.", "El número de documento es obligatorio.", "El número de licencia es obligatorio.", "La categoría de licencia es obligatoria." o "La fecha de vencimiento es obligatoria." |
| 409 | El documento ya se encuentra registrado en la empresa. | Documento repetido |
| 409 | La licencia ya se encuentra registrada en la empresa. | Licencia repetida |

`GET /api/v1/drivers/{id}/eligibility`:

```json
{
  "driverId": "<uuid>",
  "eligible": true,
  "driverStatus": "ENABLED",
  "licenseStatus": "VALID",
  "reason": "Conductor habilitado."
}
```

Si la licencia está vencida o el conductor está `DISABLED`, `eligible` es falso y `reason` es "El conductor no está habilitado para ser asignado.". `licenseStatus` vale `VALID` o `EXPIRED`.

#### 4.1.4. Cuentas para el selector de conductor

`GET /api/v1/users` (solo ADMINISTRATOR) devuelve la lista de usuarios de la empresa:

```json
[
  {
    "id": "<uuid>",
    "fullName": "Luis Quispe Mamani",
    "email": "conductor@empresa-demo.pe",
    "role": "DRIVER",
    "status": "ACTIVE"
  }
]
```

Con token de OPERATIONS_MANAGER responde 403 con el mensaje genérico. Por eso el selector de S3-02 no puede funcionar para el responsable hasta que F0-1 publique un contrato propio.

### 4.2. Farid: gastos, liquidación e incidencias (S3-06, S3-06b y S3-08b)

#### 4.2.1. Gastos y comprobantes

| Ruta | Método | Rol | Uso |
|---|---|---|---|
| `/api/v1/trips/{tripId}/expenses` | GET | Cualquier autenticado (duda 2) | Listado y resumen por estado (S3-06b) |
| `/api/v1/expenses/{id}` | GET | Cualquier autenticado | Detalle y enlaces (S3-06b) |
| `/api/v1/expenses/{id}/status` | PATCH | ADMINISTRATOR, OPERATIONS_MANAGER | Aprobar u observar (S3-06) |
| `/api/v1/expenses/{expenseId}/evidences/{evidenceId}/content` | GET | Responsable o administrador de la empresa, y el conductor dueño | Lectura de la foto (S3-06b y S3-08b) |
| `/api/v1/trips/{tripId}/expenses` | POST | Cualquier autenticado (la app usa DRIVER) | Registro del gasto (Android, S3-05) |
| `/api/v1/expenses/{expenseId}/evidences` | POST | DRIVER dueño del gasto | Subida del comprobante (S3-03, Pedro) |
| `/api/v1/expenses/sync` | POST | Cualquier autenticado | Lote offline (S3-05, Android) |

`GET /api/v1/trips/{tripId}/expenses` devuelve una lista de `ExpenseResource`. Los totales por estado de S3-06b se calculan en la web con esta lista; el backend no tiene un endpoint de resumen de gastos por viaje (duda 4).

```json
[
  {
    "id": "<uuid>",
    "tenantId": "<uuid>",
    "tripId": "<uuid>",
    "driverId": "<uuid>",
    "category": "FUEL",
    "amount": 180.50,
    "currency": "PEN",
    "expenseDate": "2026-11-09",
    "status": "APPROVED",
    "idempotencyKey": "EXP-S3-0001",
    "observationReason": null,
    "isLocked": false,
    "evidence": null,
    "createdAt": "2026-11-09T06:58:12Z",
    "updatedAt": "2026-11-09T07:00:00Z"
  }
]
```

Los estados son `REGISTERED`, `PENDING_SUPPORT`, `APPROVED` y `OBSERVED`. Las categorías son `FUEL`, `TOLL`, `FOOD`, `LODGING`, `MAINTENANCE` y `OTHER`.

`GET /api/v1/expenses/{id}` devuelve el mismo recurso con `evidence` cuando hay comprobante. El enlace a la foto es `contentUrl`, una ruta relativa que exige JWT:

```json
"evidence": {
  "id": "<uuid>",
  "fileName": "recibo-combustible.jpg",
  "contentType": "image/jpeg",
  "fileSizeBytes": 204800,
  "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "contentUrl": "/api/v1/expenses/<uuid>/evidences/<uuid>/content",
  "uploadedAt": "2026-11-09T07:00:00Z"
}
```

Error de lectura del gasto: 404 "El gasto no existe.".

`PATCH /api/v1/expenses/{id}/status`. Request para aprobar:

```json
{ "status": "APPROVED" }
```

Request para observar (el motivo es obligatorio):

```json
{ "status": "OBSERVED", "reason": "Comprobante ilegible o borroso" }
```

Respuesta 200 con el `ExpenseResource`.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | El motivo de observación es obligatorio. | Observar sin motivo |
| 404 | El gasto no existe. | Gasto de otra empresa o inexistente |
| 409 | El gasto se encuentra bloqueado por liquidación cerrada. | Liquidación ya cerrada |

`POST /api/v1/trips/{tripId}/expenses`. Request:

```json
{
  "category": "FUEL",
  "amount": 150.0,
  "currency": "PEN",
  "expenseDate": "2026-11-09",
  "idempotencyKey": "EXP-S3-0001"
}
```

Respuesta 201, con `Location: /api/v1/expenses/{id}`. El gasto nace en `PENDING_SUPPORT` con `evidence` en null. Si el cliente envía un campo `evidence`, se ignora.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La solicitud tiene datos inválidos. | Faltan categoría, fecha o clave. `errors.amount` puede traer "El monto debe ser mayor a cero." |
| 404 | El viaje no existe. | Viaje de otra empresa o inexistente |
| 409 | El viaje todavía no ha iniciado. | Viaje SCHEDULED o ASSIGNED |
| 409 | El viaje ya fue liquidado. | Viaje SETTLED |
| 409 | La clave de idempotencia ya fue utilizada. | Clave repetida |

`POST /api/v1/expenses/{expenseId}/evidences` (multipart/form-data, campo `file`). Solo el conductor que registró el gasto puede subir su comprobante. Un comprobante nuevo reemplaza al anterior. Respuesta 201, con `Location: /api/v1/expenses/{expenseId}/evidences/{evidenceId}/content`, y el `ExpenseResource` con `status` en `REGISTERED` y `evidence` con `contentUrl`.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | Adjunte el comprobante en el campo file. | Falta el campo |
| 400 | El archivo del comprobante está vacío. | Archivo vacío |
| 400 | Formato no permitido. Adjunte una foto en JPEG, PNG o WEBP, o un PDF. | El contenido no es JPEG, PNG, WEBP ni PDF |
| 403 | Solo el conductor que registró el gasto puede adjuntar su comprobante. | Otro conductor de la misma empresa |
| 403 | No tiene permisos para realizar esta operación. | Rol distinto de DRIVER |
| 404 | El gasto no existe. | Gasto de otra empresa o inexistente |
| 409 | El gasto se encuentra bloqueado por liquidación cerrada. | Liquidación ya cerrada |
| 413 | El comprobante supera el tamaño máximo permitido de 5 MB. | Archivo mayor que `RINDE_EVIDENCE_MAX_SIZE_BYTES` (5 MB por defecto) |
| 413 | El comprobante supera el tamaño máximo permitido. | Solicitud mayor que el límite del contenedor (10 MB) |

`GET /api/v1/expenses/{expenseId}/evidences/{evidenceId}/content` devuelve el archivo con su `Content-Type`, `Content-Disposition: inline; filename="..."` y `Cache-Control: no-store`. Lo leen el administrador y el responsable de la empresa, y el conductor dueño del gasto. Como el enlace exige el token, la web debe pedir el archivo con la cabecera `Authorization` y mostrarlo como blob; una etiqueta `<img>` no envía el token (duda 6). Conviene enviar `Accept: */*` o `image/*`; con otro `Accept` la API puede responder 406.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 403 | No tiene permiso para ver este comprobante. | Conductor que no registró el gasto |
| 404 | El gasto no existe. | Gasto de otra empresa o inexistente |
| 404 | El comprobante no existe. | `evidenceId` que no pertenece al gasto |
| 404 | El comprobante no tiene archivo almacenado. | Comprobante anterior a la migración V3 |
| 404 | El archivo del comprobante no está disponible en el almacenamiento. | Archivo borrado del disco |
| 500 | Ocurrió un error inesperado. | La huella SHA-256 no coincide; el detalle queda en el log |

`POST /api/v1/expenses/sync` es el lote offline de Android. Cada gasto guardado nace sin comprobante; la foto se sube después con `POST /api/v1/expenses/{id}/evidences`. Request:

```json
[
  {
    "tripId": "<uuid>",
    "category": "TOLL",
    "amount": 12.50,
    "currency": "PEN",
    "expenseDate": "2026-11-09",
    "idempotencyKey": "OFFLINE-EXP-001"
  }
]
```

Respuesta 200. Los rechazos de un ítem no impiden guardar los demás:

```json
{
  "totalReceived": 1,
  "synchronizedCount": 1,
  "items": [
    {
      "id": "<uuid>",
      "status": "PENDING_SUPPORT",
      "evidence": null
    }
  ],
  "rejectedCount": 0,
  "rejected": []
}
```

Cada rechazo trae `idempotencyKey`, `reason` y `code`. Los códigos son `TRIP_NOT_FOUND` ("El viaje no existe."), `TRIP_NOT_STARTED` ("El viaje todavía no ha iniciado."), `TRIP_SETTLED` ("El viaje ya fue liquidado."), `INVALID_EXPENSE` (el mensaje de la regla) y `DUPLICATE_KEY` ("La clave de idempotencia ya fue utilizada.").

#### 4.2.2. Liquidación

| Ruta | Método | Rol | Uso |
|---|---|---|---|
| `/api/v1/trips/{tripId}/settlement` | GET | Cualquier autenticado | Liquidación del viaje |
| `/api/v1/settlements/{id}` | GET | Cualquier autenticado | Detalle de la liquidación |
| `/api/v1/trips/{tripId}/advances` | GET | Cualquier autenticado | Adelantos del viaje |
| `/api/v1/trips/{tripId}/advances` | POST | ADMINISTRATOR, OPERATIONS_MANAGER | Adelanto antes de iniciar (S3-06) |
| `/api/v1/settlements/{id}/recalculation` | POST | ADMINISTRATOR, OPERATIONS_MANAGER | Recalcular y contar pendientes (S3-06) |
| `/api/v1/settlements/{id}/close` | POST | ADMINISTRATOR, OPERATIONS_MANAGER | Cerrar (S3-06) |
| `/api/v1/settlements/{id}/export` | GET | Cualquier autenticado | Sustento en CSV (S3-06) |

La ruta de los adelantos es `/api/v1/trips/{tripId}/advances`, la que usan el código y su prueba de documentación (`ApiDocumentationIntegrationTest`).

`GET /api/v1/trips/{tripId}/settlement` y `GET /api/v1/settlements/{id}` devuelven el `SettlementResource`:

```json
{
  "id": "<uuid>",
  "tenantId": "<uuid>",
  "tripId": "<uuid>",
  "driverId": "<uuid>",
  "status": "OPEN",
  "advanceAmount": 500.00,
  "advanceCurrency": "PEN",
  "expenseTotalAmount": 180.50,
  "expenseTotalCurrency": "PEN",
  "advanceBalance": 319.50,
  "createdAt": "2026-11-09T12:00:00Z",
  "updatedAt": "2026-11-09T12:30:00Z"
}
```

`status` vale `OPEN` o `CLOSED`. La liquidación se abre cuando llega `trip.finished`; hasta entonces el viaje no tiene liquidación.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 404 | No existe liquidación para este viaje. | El viaje no ha terminado o el evento aún no se procesó |
| 404 | La liquidación no existe. | Id inexistente o de otra empresa |

`POST /api/v1/trips/{tripId}/advances`. Request:

```json
{ "amount": 500.00, "currency": "PEN", "deliveryMethod": "CASH" }
```

`currency` es opcional (PEN por defecto). `deliveryMethod` vale `CASH` o `TRANSFER`. Respuesta 201:

```json
{
  "id": "<uuid>",
  "tripId": "<uuid>",
  "amount": 500.00,
  "currency": "PEN",
  "deliveryMethod": "CASH",
  "registeredBy": "<uuid>",
  "registeredAt": "2026-11-09T11:55:00Z"
}
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La solicitud tiene datos inválidos. | `errors` trae "El monto debe ser mayor a cero.", "Indique el monto del adelanto.", "Indique el medio de entrega del adelanto.", "El medio de entrega debe ser CASH o TRANSFER." o "La moneda debe tener tres letras, por ejemplo PEN." |
| 404 | El viaje no existe. | Viaje de otra empresa o inexistente |
| 409 | Solo se puede registrar el adelanto de un viaje programado o asignado. | Viaje IN_ROUTE o FINISHED |
| 409 | La liquidación del viaje ya fue abierta; no se pueden registrar más adelantos. | Ya existe liquidación |
| 409 | Los adelantos de un viaje deben registrarse en una sola moneda. | Moneda distinta de los adelantos previos |

`GET /api/v1/trips/{tripId}/advances` devuelve una lista de `TripAdvanceResource` en orden de registro, o una lista vacía.

`POST /api/v1/settlements/{id}/recalculation` (sin cuerpo) vuelve a sumar los gastos aprobados. La respuesta es el `SettlementResource` con `pendingExpenseCount`, que cuenta los gastos que no están aprobados ni observados (`REGISTERED` y `PENDING_SUPPORT`):

```json
{
  "id": "<uuid>",
  "status": "OPEN",
  "advanceAmount": 500.00,
  "advanceCurrency": "PEN",
  "expenseTotalAmount": 180.50,
  "expenseTotalCurrency": "PEN",
  "advanceBalance": 319.50,
  "pendingExpenseCount": 1
}
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 404 | La liquidación no existe. | Id inexistente o de otra empresa |
| 409 | La liquidación ya fue cerrada. | Liquidación CLOSED |

`POST /api/v1/settlements/{id}/close` (sin cuerpo) cierra la liquidación, publica `settlement.closed` y devuelve el `SettlementResource` con `status` en `CLOSED`. La respuesta no incluye `closedAt` ni `closedBy`; esos datos salen en el CSV (duda 8).

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 409 | La liquidación ya fue cerrada. | Segundo cierre |
| 409 | No se puede cerrar la liquidación mientras haya gastos sin revisar. Apruébelos u obsérvelos primero. | Hay gastos `REGISTERED` o `PENDING_SUPPORT` |
| 404 | La liquidación no existe. | Id inexistente o de otra empresa |

`GET /api/v1/settlements/{id}/export` devuelve `text/csv` con `Content-Disposition: attachment; filename=settlement-{id}.csv`:

```
campo,valor
settlementId,<uuid>
tripId,<uuid>
driverId,<uuid>
status,CLOSED
advance,500.00 PEN
expenseTotal,180.50 PEN
advanceBalance,319.50
closedAt,2026-11-09T13:00:00Z
closedBy,<uuid>

expenseId,category,amount,currency,date,status
<uuid>,FUEL,180.50,PEN,2026-11-09,APPROVED
```

Error: 404 "La liquidación no existe.".

#### 4.2.3. Incidencias

| Ruta | Método | Rol | Uso |
|---|---|---|---|
| `/api/v1/incidents` | GET | ADMINISTRATOR, OPERATIONS_MANAGER | Listado con filtros (S3-08b) |
| `/api/v1/incidents/{id}` | GET | Cualquier autenticado; el conductor solo ve las suyas | Detalle (S3-08b) |
| `/api/v1/incidents/{id}/status` | PATCH | ADMINISTRATOR, OPERATIONS_MANAGER | Pasar a en atención (S3-08b) |
| `/api/v1/incidents/{id}/close` | POST | ADMINISTRATOR, OPERATIONS_MANAGER | Cerrar con la acción tomada (S3-08b) |
| `/api/v1/trips/{tripId}/incidents` | POST | DRIVER asignado al viaje | Reporte desde Android (S3-08c) |

Los estados son `OPEN`, `IN_PROGRESS` y `CLOSED`. Los tipos son `BREAKDOWN`, `DELAY` y `OTHER`.

`GET /api/v1/incidents?status=OPEN&tripId={id}&vehicleId={id}` (los filtros son opcionales) devuelve una lista de `IncidentResource`, ordenada por `reportedAt` descendente:

```json
[
  {
    "id": "<uuid>",
    "tripId": "<uuid>",
    "vehicleId": "<uuid>",
    "reportedBy": "<uuid>",
    "type": "BREAKDOWN",
    "description": "Falla en el freno neumático",
    "evidenceUrl": "https://storage.rinde.pe/evidences/incidencia-1.jpg",
    "status": "OPEN",
    "reportedAt": "2026-11-09T13:20:00Z",
    "resolution": null,
    "updatedAt": "2026-11-09T13:20:00Z"
  }
]
```

Si `status` no es un estado válido, la API responde 400 con el detalle que genera Spring, que no está traducido. `evidenceUrl` es una URL libre; la subida de la foto de la incidencia no existe todavía.

`POST /api/v1/trips/{tripId}/incidents`. Request:

```json
{
  "type": "BREAKDOWN",
  "description": "Falla en el freno neumático",
  "evidenceUrl": null,
  "idempotencyKey": "INC-OFFLINE-0001"
}
```

Respuesta 201, con `Location: /api/v1/incidents/{id}`. Si se reenvía el mismo reporte con la misma clave, responde 200 con la incidencia ya registrada y no publica otro evento.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La solicitud tiene datos inválidos. | Falta `type` o `description`, o superan 500 caracteres. El mensaje por campo lo genera la validación por defecto |
| 403 | Solo el conductor asignado al viaje puede reportar incidencias. | El conductor no es el asignado |
| 403 | No tiene permisos para realizar esta operación. | Rol distinto de DRIVER |
| 404 | El viaje no existe. | Viaje de otra empresa o inexistente |
| 409 | Solo se puede reportar una incidencia cuando el viaje está en ruta. | Viaje distinto de IN_ROUTE |
| 409 | La clave de idempotencia ya fue utilizada. | La clave la usó otro usuario |
| 503 | No se pudo consultar el viaje en Trip. | Trip no responde |

`GET /api/v1/incidents/{id}`: 404 "La incidencia no existe." si no existe en la empresa o si el conductor no la reportó.

`PATCH /api/v1/incidents/{id}/status`. Request:

```json
{ "status": "IN_PROGRESS" }
```

Respuesta 200 con el `IncidentResource` en `IN_PROGRESS`.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | Este cambio solo pasa la incidencia a en atención. Para cerrarla use la acción de cierre. | Estado solicitado distinto de IN_PROGRESS |
| 404 | La incidencia no existe. | Id inexistente o de otra empresa |
| 409 | Solo una incidencia abierta puede pasar a atención. | La incidencia no está en OPEN |

`POST /api/v1/incidents/{id}/close`. Request:

```json
{ "action": "Se cambió la llanta de repuesto y el viaje continuó" }
```

Respuesta 200. La incidencia pasa a `CLOSED` y queda la resolución:

```json
{
  "id": "<uuid>",
  "tripId": "<uuid>",
  "vehicleId": "<uuid>",
  "reportedBy": "<uuid>",
  "type": "BREAKDOWN",
  "description": "Falla en el freno neumático",
  "evidenceUrl": "https://storage.rinde.pe/evidences/incidencia-1.jpg",
  "status": "CLOSED",
  "reportedAt": "2026-11-09T13:20:00Z",
  "resolution": {
    "action": "Se cambió la llanta de repuesto y el viaje continuó",
    "closedBy": "<uuid>",
    "closedAt": "2026-11-09T15:05:00Z"
  },
  "updatedAt": "2026-11-09T15:05:00Z"
}
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La solicitud tiene datos inválidos. | `action` vacía o con más de 500 caracteres |
| 404 | La incidencia no existe. | Id inexistente o de otra empresa |
| 409 | La incidencia ya está cerrada. | Segundo cierre |

Las incidencias también publican `incident.reported` e `incident.closed` en `rinde.events`. Fleet todavía no consume `incident.closed`.

### 4.3. Gabriel: Dashboard y Settlement (S3-09a y S3-10a)

| Ruta | Método | Rol | Uso |
|---|---|---|---|
| `/api/v1/dashboard/metrics` | GET | ADMINISTRATOR, OPERATIONS_MANAGER | Métricas con periodo (S3-09a) |
| `/api/v1/dashboard/fleet-status` | GET | ADMINISTRATOR, OPERATIONS_MANAGER | Estado de la flota (Inicio) |
| `/api/v1/dashboard/trips/{tripId}/summary` | GET | ADMINISTRATOR, OPERATIONS_MANAGER | Resumen del viaje y enlaces |

`GET /api/v1/dashboard/metrics?from=2026-11-01&to=2026-11-30`. Sin fechas, la API toma desde el día 1 del mes hasta hoy. Respuesta 200:

```json
{
  "period": { "from": "2026-11-01", "to": "2026-11-30" },
  "totalTrips": 12,
  "scheduledTrips": 2,
  "assignedTrips": 1,
  "inRouteTrips": 3,
  "finishedTrips": 4,
  "settledTrips": 2,
  "totalExpenses": 30,
  "approvedExpenses": 18,
  "pendingExpenses": 9,
  "observedExpenses": 3,
  "approvedExpenseTotals": { "PEN": 4210.50 },
  "pendingSettlements": 2,
  "incidents": { "openCount": 1, "source": "AVAILABLE" },
  "partial": false
}
```

`pendingExpenses` cuenta `PENDING_SUPPORT` y `REGISTERED`. `approvedExpenseTotals` suma solo los gastos aprobados cuya fecha está en el periodo, por moneda. Si Incident no responde, `incidents.source` vale `UNAVAILABLE`, `openCount` va en null y `partial` es verdadero.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La fecha de inicio debe tener el formato AAAA-MM-DD. | `from` con otro formato |
| 400 | La fecha de fin debe tener el formato AAAA-MM-DD. | `to` con otro formato |
| 400 | La fecha final no puede ser anterior a la inicial. | `to` anterior a `from` |

`GET /api/v1/dashboard/fleet-status`:

```json
{
  "totalVehicles": 5,
  "availableVehicles": 3,
  "inTripVehicles": 1,
  "inMaintenanceVehicles": 1,
  "totalDrivers": 4,
  "enabledDrivers": 3,
  "disabledDrivers": 1,
  "vehicles": [
    { "id": "<uuid>", "plateNumber": "PX123", "brand": "Volvo", "model": "FH 500", "status": "AVAILABLE" }
  ],
  "drivers": [
    { "id": "<uuid>", "fullName": "Luis Quispe Mamani", "licenseNumber": "Q12345678", "licenseCategory": "A-IIb", "status": "ENABLED" }
  ]
}
```

`GET /api/v1/dashboard/trips/{tripId}/summary`:

```json
{
  "tripId": "<uuid>",
  "code": "TRP-000001",
  "status": "FINISHED",
  "origin": "Lima",
  "destination": "Arequipa",
  "cargoDescription": "Repuestos para maquinaria",
  "cargoWeightKg": 1250.50,
  "departureDate": "2026-11-09",
  "vehicleId": "<uuid>",
  "driverId": "<uuid>",
  "startedAt": "2026-11-09T08:00:00Z",
  "finishedAt": "2026-11-09T17:30:00Z",
  "expenseCount": 2,
  "expenseTotal": 180.50,
  "expenseCurrency": "PEN",
  "settlementStatus": "OPEN",
  "advanceAmount": 500.00,
  "advanceBalance": 319.50
}
```

Error de los tres endpoints para un conductor: 403 con el mensaje genérico. Error del resumen: 404 "El viaje no existe.".

Regresión de S3-10a: las rutas públicas de liquidación (sección 4.2.2) no cambian al separar Settlement. Los endpoints internos de liquidaciones que propone Gabriel (`/api/internal/v1/settlements/...`) todavía no existen; el Dashboard usa hoy la fachada local de core (duda 11).

### 4.4. Flor: suscripciones (S3-10b) y landing (S3-17)

#### 4.4.1. Landing (S3-17)

La landing no consume API en US01 a US05 ni en US07. El formulario de demostración de US06 envía sus datos a `POST /api/v1/public/demo-requests`, descrito en la sección Solicitud de demostración (US06) de este documento.

#### 4.4.2. Suscripciones de la empresa

| Ruta | Método | Rol | Uso |
|---|---|---|---|
| `/api/v1/plans` | GET | Cualquier autenticado | Catálogo de planes activos |
| `/api/v1/subscriptions` | POST | ADMINISTRATOR | Elegir plan (queda PENDING_PAYMENT) |
| `/api/v1/subscriptions/current` | GET | Cualquier autenticado | Suscripción vigente o pendiente; 204 si no tiene |
| `/api/v1/subscriptions/{id}` | GET | Cualquier autenticado | Detalle de la suscripción |
| `/api/v1/subscriptions/{id}/checkout` | POST | ADMINISTRATOR | Pago simulado |
| `/api/v1/subscriptions/{subscriptionId}/payments/{paymentId}/simulation` | POST | ADMINISTRATOR | Confirmar o rechazar el pago simulado |
| `/api/v1/billing/webhooks/payment` | POST | Sin JWT; firma `X-RINDE-Signature` | Resultado que informa la pasarela |

`GET /api/v1/plans`:

```json
[
  { "planId": "<uuid>", "name": "Plan inicial", "monthlyPrice": 149.00, "unitLimit": 10 }
]
```

`POST /api/v1/subscriptions`. Request y respuesta 201, con `Location`:

```json
{ "planId": "<uuid>" }
```

```json
{
  "subscriptionId": "<uuid>",
  "plan": { "planId": "<uuid>", "name": "Plan inicial", "monthlyPrice": 149.00, "unitLimit": 10 },
  "status": "PENDING_PAYMENT",
  "startsAt": null,
  "expiresAt": null,
  "renewalNotice": false,
  "latestPaymentStatus": null,
  "lastPaymentFailureReason": null
}
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 404 | El plan no existe o no está activo. | Plan inexistente o inactivo |
| 409 | La empresa ya tiene una suscripción pendiente de pago. | Ya hay una pendiente |
| 409 | El plan admite hasta N unidades y la flota excede el límite por X unidades. | La empresa tiene más unidades que el límite del plan elegido |

`POST /api/v1/subscriptions/{id}/checkout` (sin cuerpo) crea el intento de pago simulado. Responde 201 la primera vez y 200 si ya había un pago pendiente:

```json
{
  "paymentId": "<uuid>",
  "providerReference": "SIM-20261109-0001",
  "amount": 149.00,
  "status": "PENDING",
  "simulated": true,
  "confirmationEnabled": true
}
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 404 | La suscripción no existe. | Id inexistente o de otra empresa |
| 409 | La suscripción no admite un pago en su estado actual. | Suscripción ya ACTIVE u otro estado sin pago pendiente |

`POST /api/v1/subscriptions/{subscriptionId}/payments/{paymentId}/simulation`. Request:

```json
{ "status": "CONFIRMED" }
```

El valor puede ser `CONFIRMED` o `REJECTED`. Respuesta 200 con el `SubscriptionResource`; con `CONFIRMED` la suscripción queda `ACTIVE`.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 404 | El intento de pago no existe en su empresa. | Pago de otra empresa o inexistente |
| 409 | El intento de pago ya tiene un resultado registrado. | Pago ya confirmado o rechazado |

`POST /api/v1/billing/webhooks/payment` no usa JWT. La cabecera `X-RINDE-Signature` lleva el HMAC-SHA256 en hexadecimal de esta cadena, unidas con salto de línea (`\n`): `providerEventId`, `providerReference`, `status`, `amount` y `failureReason` (o cadena vacía). La clave es la del servicio, en UTF-8, de al menos 32 caracteres. La clave no se escribe en este documento.

Request:

```json
{
  "providerEventId": "EVT-SIM-20261109-0001",
  "providerReference": "SIM-20261109-0001",
  "status": "CONFIRMED",
  "amount": "149.00"
}
```

Respuesta 200 con el resultado. El campo `duplicate` indica si el evento ya se había procesado:

```json
{
  "providerEventId": "EVT-SIM-20261109-0001",
  "duplicate": false,
  "paymentStatus": "CONFIRMED",
  "subscriptionStatus": "ACTIVE",
  "startsAt": "2026-11-09T12:00:00Z",
  "expiresAt": "2026-12-09T12:00:00Z",
  "failureReason": null
}
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 401 | La firma del webhook de pago no es válida. | Firma distinta o ausente |
| 404 | El intento de pago no existe. | `providerReference` desconocido |
| 409 | El webhook debe informar CONFIRMED o REJECTED. | Estado distinto |
| 409 | El identificador de evento ya se usó con otro resultado. | Mismo `providerEventId` con otro resultado |
| 409 | El importe del pago no coincide con el plan seleccionado. | `amount` distinto del precio del plan |
| 400 | La pasarela debe informar el motivo del rechazo. | `REJECTED` sin `failureReason` |

`GET /api/v1/subscriptions/{id}` devuelve el `SubscriptionResource`; si no existe en la empresa, 404 "La suscripción no existe.". Los estados de la suscripción son `PENDING_PAYMENT`, `ACTIVE`, `EXPIRING`, `EXPIRED` y `SUSPENDED`.

Los avisos de vencimiento y de comprobante siguen dentro de core y pasan a RabbitMQ con F0-12 (`TAREAS_AVANCE4.md`, S3-10b-P).

#### 4.4.3. Plataforma (no incluida en la colección)

Estas rutas exigen un token de `PLATFORM_OWNER`, con cuenta y firma propias. La colección no incluye esa cuenta porque sus variables son otras.

| Ruta | Método |
|---|---|
| `/api/v1/platform/billing/overview` | GET |
| `/api/v1/platform/plans` | GET y POST |
| `/api/v1/platform/plans/{id}/availability` | PATCH |
| `/api/v1/platform/subscriptions` | GET |
| `/api/v1/platform/payments` | GET |
| `/api/v1/platform/audit/billing` | GET |

El token se obtiene con `POST /api/v1/platform/auth/sign-in` (IAM).

### 4.5. Sesión y usuarios (IAM)

`POST /api/v1/auth/sign-in`. Request:

```json
{ "email": "admin@empresa-demo.pe", "password": "<contraseña del entorno>" }
```

Respuesta 200:

```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzZjZjMWI5ZSJ9.firma", "tokenType": "Bearer", "expiresInSeconds": 28800 }
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 401 | El correo o la contraseña son incorrectos. | Credenciales no válidas |
| 403 | El usuario está deshabilitado. | Cuenta deshabilitada |
| 403 | La empresa aún no confirmó su correo. Use el enlace de verificación que se le envió. | Empresa pendiente de verificación |
| 403 | El acceso de la empresa está suspendido. Contacte a Rinde. | Empresa suspendida |

Una empresa restringida (RESTRICTED) sí puede iniciar sesión; su estado viaja en el token y los servicios limitan la creación de operaciones.

`GET /api/v1/users/me` devuelve la sesión con la empresa:

```json
{
  "id": "<uuid>",
  "fullName": "Ana Torres Vega",
  "email": "admin@empresa-demo.pe",
  "role": "ADMINISTRATOR",
  "status": "ACTIVE",
  "tenant": { "id": "<uuid>", "tradeName": "Transportes Demo SAC", "ruc": "20123456789", "status": "ACTIVE" }
}
```

`tenant.status` vale `PENDING_VERIFICATION`, `ACTIVE` o `RESTRICTED`.

`GET /api/v1/users` (ADMINISTRATOR) devuelve la lista de `UserResource` (sección 4.1.4).

`POST /api/v1/users` (ADMINISTRATOR) invita a un usuario. Request:

```json
{ "fullName": "Nuevo Conductor", "email": "nuevo.conductor@empresa-demo.pe", "role": "DRIVER" }
```

Respuesta 201, con `Location: /api/v1/users/{id}`, y el `UserResource` con `status` en `INVITED`. Errores: 409 "Ya existe un usuario registrado con ese correo."; 400 por validación (`fullName` de hasta 150 caracteres, correo con formato válido, `role` obligatorio); 403 con el mensaje genérico para cualquier rol distinto de ADMINISTRATOR.

`PATCH /api/v1/users/{id}` (ADMINISTRATOR) cambia el rol o deshabilita la cuenta. Request:

```json
{ "role": "OPERATIONS_MANAGER" }
```

o

```json
{ "status": "DISABLED" }
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | Indique el nuevo rol o que el usuario debe deshabilitarse. | Cuerpo sin rol ni estado |
| 400 | No se puede cambiar el rol de un usuario deshabilitado. | Cambio de rol sobre cuenta deshabilitada |
| 400 | El usuario ya está deshabilitado. | Deshabilitar dos veces |
| 404 | El usuario no existe. | Id inexistente o de otra empresa |

### 4.6. Viajes: precondiciones (Trip)

La colección no crea viajes; los necesita en un estado concreto. Estos endpoints los preparan. Son de Trip y no forman parte de las tareas de esta colección.

| Ruta | Método | Rol | Uso |
|---|---|---|---|
| `/api/v1/trips` | GET | ADMINISTRATOR, OPERATIONS_MANAGER | Tablero; `status` acepta varios estados separados por coma. Lo usa la colección para tripId |
| `/api/v1/trips/{id}` | GET | ADMINISTRATOR, OPERATIONS_MANAGER, DRIVER | Detalle con `statusChanges` |
| `/api/v1/trips/assigned-to-me` | GET | DRIVER | Viajes asignados al conductor |
| `/api/v1/trips` | POST | ADMINISTRATOR, OPERATIONS_MANAGER | Programar: `origin`, `destination`, `cargoDescription`, `cargoWeightKg` (opcional), `departureDate` y `confirmPastDate` |
| `/api/v1/trips/{id}/assignment` | PUT | ADMINISTRATOR, OPERATIONS_MANAGER | Asignar: `vehicleId`, `driverId` y `confirmOverdueMaintenance` |
| `/api/v1/trips/{id}/start` | POST | DRIVER | Pasa a IN_ROUTE |
| `/api/v1/trips/{id}/finish` | POST | DRIVER | Pasa a FINISHED; `trip.finished` abre la liquidación |

Estados del viaje: `SCHEDULED`, `ASSIGNED`, `IN_ROUTE`, `FINISHED` y `SETTLED`. El `driverId` de la asignación es el id del conductor en Fleet, no el de la cuenta (duda 1).

## 5. Colección Postman

Archivo: `Sprint3_Avance4/Tareas/RINDE_Sprint3_Equipo.postman_collection.json`, formato Postman v2.1.0, con 54 peticiones.

- Carpetas: `Auth`, `Andree`, `Farid`, `Gabriel` y `Flor`. Farid tiene dos partes: la A con el viaje EN ruta y la B con el viaje finalizado.
- Autenticación: cada petición usa `Authorization: Bearer {{accessToken}}` o `{{driverToken}}` según el rol. Las peticiones de inicio de sesión no llevan token.
- Variables pedidas: `baseUrl`, `adminEmail`, `adminPassword`, `driverEmail`, `driverPassword`, `accessToken`, `driverToken`, `tripId`, `vehicleId`, `driverId`, `expenseId`, `settlementId` e `incidentId`.
- Variables adicionales: `planId`, `subscriptionId`, `paymentId`, `providerReference`, `paymentAmount`, `providerEventId` y `webhookSecret`. Son necesarias para guardar los ids de Subscriptions, que no están entre las variables pedidas.
- Variables secretas: `adminPassword`, `driverPassword`, `accessToken`, `driverToken` y `webhookSecret` van con tipo `secret` y sin valor en el archivo. Completarlas en un entorno local de Postman.
- Scripts: el inicio de sesión guarda `accessToken` o `driverToken`. Las peticiones que crean recursos guardan su id. Cada petición tiene pruebas de código y de campos clave. El webhook firmado calcula `X-RINDE-Signature` en el pre-request con `CryptoJS`, que Postman incluye.
- Orden: Auth; luego Andree, que crea vehículo y conductor; luego Farid parte A con un viaje IN_ROUTE asignado al conductor de la cuenta; después hay que finalizar ese viaje desde Trip (`POST /api/v1/trips/{id}/finish` con el token del conductor); luego Farid parte B; después Gabriel y Flor.
- Cada ejecución crea un vehículo, un conductor y un gasto. Si el plan llega a su límite de unidades, la creación de vehículos responde 409.

Ejecución con Postman CLI, sintaxis básica: `postman collection run "<ruta al archivo>" -e "<entorno local con las variables secretas>"`.

## 6. Verificación

- La colección pasa `python -m json.tool` sin errores.
- Postman CLI 1.69.0 está instalado. Newman no está instalado.
- No se ejecutó la colección contra el entorno: `http://localhost:8000` no responde porque el entorno no está levantado. Levantarlo requiere Docker y un `.env.compose` con secretos, y no era parte de este encargo.
- Las rutas, los roles, los DTO y los mensajes se comprobaron contra los controladores, los DTO, los servicios y el gateway de develop 2cc8806. No se comprobaron contra respuestas reales.
- Lo que se espera al ejecutar sin contraseñas: el inicio de sesión responde 400 por la validación de contraseña vacía, y las peticiones protegidas sin token responden 401.

## 7. Dudas

1. Identificador del conductor en Expense. El gasto guarda el `userId` de IAM como `driverId`. Trip y Fleet usan el id del conductor de Fleet, y la liquidación toma el `driverId` de `trip.finished`. Pedro decide si Expense debe usar el id de Fleet antes de cerrar S3-06 y S3-08b.
2. Visibilidad de los gastos del viaje. `GET /api/v1/trips/{tripId}/expenses` no filtra por rol: el conductor ve todos los gastos del viaje de su empresa. Confirmar si debe limitarse al conductor dueño.
3. Cambio de estado de gastos. `PATCH /api/v1/expenses/{id}/status` acepta cualquier estado del enum. Con `REGISTERED` o `PENDING_SUPPORT` responde 200 y no cambia el gasto. Propuesta: rechazarlo con 400. Además, registrar un gasto no tiene restricción de rol: cualquier usuario autenticado puede hacerlo.
4. Resumen de gastos por viaje (S3-06b). No existe un endpoint de resumen en Expense. El total de la pantalla se calcula con `GET /api/v1/trips/{tripId}/expenses`. El único resumen por viaje es `GET /api/v1/dashboard/trips/{tripId}/summary`, limitado a administrador y responsable.
5. Historial del vehículo (S3-07 y S3-07b). No hay endpoint de historial en el código. La web no puede mostrar el historial ni su estado "no disponible" hasta que Pedro publique el contrato.
6. Lectura de comprobantes. El enlace `contentUrl` exige JWT. La web debe pedir el archivo con el token y mostrarlo como blob; un `<img>` directo fallaría. La colección no incluye la lectura porque necesita un comprobante subido, y la subida requiere un archivo local.
7. Nombre del campo `isLocked`. `ExpenseResource` expone `isLocked` porque así se llama el componente del record. No se comprobó con una respuesta real; verificarlo en la primera ejecución.
8. Datos de cierre de la liquidación. `SettlementResource` no incluye `closedAt` ni `closedBy`; solo aparecen en el CSV. Si la pantalla de cierre los necesita, hace falta un cambio aditivo en el contrato.
9. Mensajes de incidencias. Para pasar a atención una incidencia que no está en OPEN, la API responde "Solo una incidencia abierta puede pasar a atención."; el reporte de S3-08a describe otro texto. El contrato usa los mensajes del código. S3-08a también nombra `RESOLVED` e `IncidentResolved`; el código usa `CLOSED` e `incident.closed`.
10. Consumidores de incidencias. Fleet todavía no consume `incident.closed` para el historial de US32, y el Dashboard de core consume el conteo abierto por HTTP con `partial` como respaldo.
11. Dashboard y liquidaciones. Settlement sigue en core hasta S3-10a. Los endpoints internos de liquidaciones que propone Gabriel no existen aún; el Dashboard usa la fachada local.
12. Viajes de prueba. La colección no crea viajes. Para una ejecución completa, el entorno debe tener un viaje asignado al conductor, que se inicia y finaliza, y otro programado o asignado para el caso de adelanto con 201. El script de la sección 3 prepara los viajes, y el conductor de `driverEmail` solo tiene el viaje en ruta. Confirmar en la primera ejecución que la colección encuentra el viaje que necesita cada parte.
13. Límite de unidades. Cada ejecución crea un vehículo. Con el plan de prueba, la creación puede llegar a 409; dejar holgura en el plan o limpiar los datos del entorno.
14. Firma del webhook. La colección calcula la firma con `CryptoJS` del pre-request, y la clave debe tener al menos 32 caracteres. Confirmar en la primera ejecución que Postman CLI interpreta el script. Las rutas de plataforma quedan fuera por requerir otra cuenta.
15. Variables adicionales. La colección añade siete variables a las trece pedidas. Confirmar que se aceptan.
16. Documentación desactualizada. El README raíz dice que Incident Management no está implementado y no lista `services/incident`, pero develop lo tiene desde el merge 079b353. El código manda.
17. Ejecución pendiente. La colección queda sin ejecutar contra el entorno. Para hacerlo: levantar el compose como indica la sección 1, completar las variables secretas y ejecutar el comando de la sección 5.

## Contrato F0-1 (09/11/2026)

Pedro aprobó el 09/11/2026 la Fase 0 del Sprint 3, con el contrato del vínculo entre el conductor y su cuenta. La duda 1 (identificador del conductor en Expense) no cambia. Los cambios están en la rama `feature/driver-account-link` de `rinde-services`, a partir de develop 2cc8806, con los commits 21f1cd4 (IAM), f7e437c y aa20623 (Fleet).

### 1. Cuentas DRIVER para el responsable (IAM)

`GET /api/v1/users?role=DRIVER`. La ruta ya la enruta el gateway a IAM (`/api/v1/users/**`).

| Rol que consulta | Sin `role` | `role=DRIVER` | Otro `role` válido |
|---|---|---|---|
| ADMINISTRATOR | 200, todos los usuarios | 200, solo conductores | 200, solo ese rol |
| OPERATIONS_MANAGER | 403 | 200, solo cuentas DRIVER de su empresa | 403 |
| DRIVER | 403 | 403 | 403 |

Respuesta 200, una lista de `UserResource`:

```json
[
  {
    "id": "<uuid>",
    "fullName": "Luis Quispe Mamani",
    "email": "conductor@empresa-demo.pe",
    "role": "DRIVER",
    "status": "ACTIVE"
  }
]
```

La lista incluye también las cuentas `INVITED` y `DISABLED`, con su `status`. Filtrarlas en el selector queda a cargo de la web.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | El rol indicado no existe. | El administrador pide un `role` que no existe, por ejemplo `SUPERUSER`. |
| 403 | No tiene permisos para realizar esta operación. | El responsable no envía `role=DRIVER`, o el rol es DRIVER. |

`POST /api/v1/users` y `PATCH /api/v1/users/{id}` siguen siendo solo para ADMINISTRATOR.

### 2. Estado de la cuenta que se vincula

Solo se vinculan cuentas `ACTIVE`. El cliente interno de IAM (`GET /api/internal/v1/tenants/{tenantId}/users/{userId}`) devuelve el campo `status` con los valores `INVITED`, `ACTIVE` o `DISABLED`. Fleet lo lee en su propio DTO (`IamUserAccount`), sin cambios en IAM.

La regla se aplica en el alta (`POST /api/v1/drivers`) y en el cambio (`PUT /api/v1/drivers/{id}/account`). Cualquier estado distinto de `ACTIVE` responde 409 con "La cuenta del conductor no está activa.". Se eligió 409 y no 422 porque el estado de la cuenta es un conflicto con el estado actual de IAM. Retirar el vínculo no consulta a IAM.

### 3. Alta de conductor con vínculo (Fleet)

`POST /api/v1/drivers` (ADMINISTRATOR y OPERATIONS_MANAGER). Si `userId` viene vacío, no hay validación de cuenta. Si viene, Fleet consulta IAM y comprueba, en este orden: que la cuenta pertenece a la empresa, que tiene rol DRIVER, que está `ACTIVE` y que no está vinculada a otro conductor.

Request:

```json
{
  "userId": "<uuid>",
  "fullName": "Luis Quispe Mamani",
  "documentType": "DNI",
  "documentNumber": "71234567",
  "licenseNumber": "Q12345678",
  "licenseCategory": "A-IIb",
  "licenseExpirationDate": "2030-12-31"
}
```

Respuesta 201, con `Location: /api/v1/drivers/{id}` y el `DriverResource` con `userId` informado.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 404 | La cuenta no existe en la empresa. | La cuenta no existe o pertenece a otra empresa. Se responde 404 para no revelar que existe. |
| 400 | La cuenta debe tener el rol de conductor. | La cuenta tiene un rol distinto de DRIVER. |
| 409 | La cuenta del conductor no está activa. | La cuenta está `INVITED` o `DISABLED`. |
| 409 | La cuenta ya está vinculada a otro conductor. | Otro conductor de la empresa ya usa ese `userId`. |
| 503 | No se pudo comprobar la cuenta en IAM. Intente nuevamente. | IAM no responde o responde con error. |

Los errores de documento y de licencia repetidos (409) y los de validación (400) no cambian.

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "La cuenta del conductor no está activa.",
  "traceId": "<uuid>"
}
```

### 4. Cambiar o retirar el vínculo de un conductor (Fleet)

`PUT /api/v1/drivers/{id}/account` (ADMINISTRATOR y OPERATIONS_MANAGER). El cuerpo siempre trae la clave `userId`.

Para vincular o cambiar a otra cuenta:

```json
{ "userId": "<uuid>" }
```

Para retirar el vínculo:

```json
{ "userId": null }
```

Respuesta 200 con el `DriverResource` del conductor. El campo `userId` trae la cuenta nueva, o `null` si se retiró:

```json
{
  "id": "<uuid>",
  "userId": null,
  "fullName": "Luis Quispe Mamani",
  "documentType": "DNI",
  "documentNumber": "71234567",
  "licenseNumber": "Q12345678",
  "licenseCategory": "A-IIb",
  "licenseExpirationDate": "2030-12-31",
  "status": "ENABLED",
  "createdAt": "2026-11-09T14:12:00Z"
}
```

Reglas de este endpoint:

- Para vincular, aplica las mismas validaciones del alta: cuenta de la empresa, rol DRIVER, cuenta `ACTIVE` y cuenta no vinculada a otro conductor.
- Volver a vincular la misma cuenta al mismo conductor responde 200 sin cambios. Las validaciones se aplican también en ese caso, así que una cuenta que ya no está activa responde 409.
- Retirar el vínculo no consulta a IAM.

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | Indique el identificador de la cuenta, o null para retirar el vínculo. | El cuerpo no trae la clave `userId`. |
| 400 | El identificador de la cuenta no es válido. | `userId` no es un UUID. |
| 400 | La cuenta debe tener el rol de conductor. | La cuenta tiene un rol distinto de DRIVER. |
| 403 | No tiene permisos para realizar esta operación. | El rol es DRIVER. |
| 404 | El conductor no existe. | El conductor no existe en la empresa del token. |
| 404 | La cuenta no existe en la empresa. | La cuenta no existe o pertenece a otra empresa. |
| 409 | La cuenta del conductor no está activa. | La cuenta está `INVITED` o `DISABLED`. |
| 409 | La cuenta ya está vinculada a otro conductor. | Otro conductor de la empresa ya usa esa cuenta. |
| 503 | No se pudo comprobar la cuenta en IAM. Intente nuevamente. | IAM no responde o responde con error. |

Punto pendiente de decisión de Pedro: la regla de estado se aplica al vincular o cambiar. Si una cuenta se deshabilita después de vincularse, el conductor conserva el vínculo y Fleet no lo retira de forma automática.

### 5. Ficha propia del conductor (Fleet)

`GET /api/v1/drivers/me` (rol DRIVER). El gateway ya enruta `/api/v1/drivers/**` a Fleet, así que no se cambió el gateway.

Respuesta 200, con el `DriverResource` del conductor vinculado a la cuenta del token:

```json
{
  "id": "<uuid>",
  "userId": "<uuid>",
  "fullName": "Luis Quispe Mamani",
  "documentType": "DNI",
  "documentNumber": "71234567",
  "licenseNumber": "Q12345678",
  "licenseCategory": "A-IIb",
  "licenseExpirationDate": "2030-12-31",
  "status": "ENABLED",
  "createdAt": "2026-11-09T14:12:00Z"
}
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 404 | La cuenta no está vinculada a ningún conductor. | El token no tiene un conductor vinculado. |
| 403 | No tiene permisos para realizar esta operación. | El rol no es DRIVER. |

### 6. Expense (sin cambios)

Expense sigue registrando en `driverId` el `userId` del actor. La liquidación toma el conductor del viaje, que es el id de Fleet. La consistencia entre ambos se revisa en S3-06 (duda 1).

### 7. Pruebas

- IAM: `features/iam/US11_list_driver_accounts.feature`, con tres reglas (listado por rol del administrador, alcance del responsable y conductor sin acceso). Unitaria: filtro por rol en `UserQueryServiceImplTest`.
- Fleet: `features/fleet/US13_link_driver_account.feature`, con escenarios de vínculo válido, otra empresa, rol distinto, cuenta ya vinculada, cuenta no activa en el alta y en el cambio, cambio y retiro por administrador y responsable, relinkeo sin cambios, y conductor sin permiso. `features/fleet/US13_current_driver.feature` cubre la ficha con y sin vínculo, y el administrador sin ficha. Unitarias: `DriverCommandServiceImplTest`.

## Solicitud de demostración (US06)

El endpoint de la solicitud de demostración está integrado en develop de rinde-services con el merge e5ecc04 (09/11/2026). IAM atiende las dos rutas y el gateway las enruta. El registro es público: no pide token y lo usa el formulario de la landing. La consulta solo la puede usar el dueño de Rinde.

### 1. Registrar una solicitud (público)

`POST /api/v1/public/demo-requests`, por el gateway en http://localhost:8000. No lleva cabecera `Authorization` y el cuerpo es `application/json`.

| Campo | Obligatorio | Regla |
|---|---|---|
| `fullName` | Sí | Texto no vacío, de hasta 150 caracteres. |
| `companyName` | Sí | Texto no vacío, de hasta 120 caracteres. |
| `email` | Sí | Correo válido, de hasta 150 caracteres. Se guarda sin espacios y en minúsculas. |
| `phone` | No | Entre 6 y 30 caracteres: dígitos, espacios, paréntesis y guiones, con un + opcional al inicio. Puede ir vacío. |
| `fleetSize` | Sí | Número entero de 1 a 500. |
| `message` | No | Hasta 500 caracteres. |

Ejemplo de solicitud:

```json
{
  "fullName": "Ana Rojas Paredes",
  "companyName": "Transportes Andes SAC",
  "email": "ana.rojas@transportesandes.pe",
  "phone": "+51 999 888 777",
  "fleetSize": 12,
  "message": "Queremos revisar la gestión de viajes y gastos."
}
```

Respuesta 201:

```json
{
  "id": "<uuid>",
  "status": "RECEIVED",
  "receivedAt": "2026-11-09T13:05:12.418Z"
}
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | La solicitud tiene datos inválidos. | Algún campo no cumple su regla. `errors` trae un mensaje por campo, por ejemplo `"fleetSize": "La cantidad de unidades debe estar entre 1 y 500."` |
| 429 | Ya recibimos tu solicitud. Te contactaremos pronto. | El mismo correo, sin distinguir mayúsculas, envió una solicitud en los últimos 10 minutos. No se registra otra. |

### 2. Consultar las solicitudes (dueño de Rinde)

`GET /api/v1/platform/demo-requests?page=0&size=20`, con el token de PLATFORM_OWNER (sección 4.4.3). `page` empieza en 0 y `size` va de 1 a 100; por defecto son 0 y 20. Las solicitudes salen de la más reciente a la más antigua.

Respuesta 200, con el formato de paginación de las demás consultas:

```json
{
  "items": [
    {
      "id": "<uuid>",
      "fullName": "Ana Rojas Paredes",
      "companyName": "Transportes Andes SAC",
      "email": "ana.rojas@transportesandes.pe",
      "phone": "+51 999 888 777",
      "fleetSize": 12,
      "message": "Queremos revisar la gestión de viajes y gastos.",
      "status": "RECEIVED",
      "receivedAt": "2026-11-09T13:05:12.418Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

| Código | Mensaje (`detail`) | Causa |
|---|---|---|
| 400 | Los parámetros de la solicitud no son válidos. | `page` o `size` están fuera de rango. |
| 401 | Se requiere un token de acceso válido. | No se envió token. |
| 403 | No tiene permisos para realizar esta operación. | El token no es de PLATFORM_OWNER. |

## Mensajería de los servicios nuevos

Los avisos de suscripción salen por el exchange `rinde.events`. Hoy solo viajan dentro de core. Al separar Subscriptions (S3-10b) quedan con estas claves, y Notifications los consume desde una cola propia.

| Clave de enrutamiento | Evento | Publica | Consume | Cola |
|---|---|---|---|---|
| `subscription.expiring` | SubscriptionExpiring: eventId, occurredAt, tenantId, subscriptionId y expiresAt. | subscription | Notifications | `notification.subscription-events` |
| `subscription.payment-receipt` | PaymentReceiptRequested: eventId, occurredAt, tenantId, subscriptionId, paymentId, providerReference, amount, startsAt y expiresAt. | subscription | Notifications | `notification.subscription-events` |

Bases, puertos y eventos de los servicios nuevos:

| Servicio | Puerto | Base | Consume | Publica |
|---|---|---|---|---|
| incident | 8085 | rinde_incident | Ninguna cola | `incident.reported` e `incident.closed` |
| settlement | 8086 | rinde_settlement | `trip.finished` desde la cola `rinde.events.settlement.trip-events` | `settlement.closed` |
| subscription | 8087 | rinde_subscription | Ninguna cola | `subscription.activated`, `subscription.suspended`, `subscription.plan-changed`, `subscription.expiring` y `subscription.payment-receipt` |

Las colas que ya existen conservan su nombre. Al separar un servicio cambia quién consume, no el contrato. Los consumidores guardan el `eventId` de cada mensaje e ignoran los repetidos.

## Entorno local para el equipo

Esta sección describe cómo levantar el entorno de trabajo en su equipo. No incluye credenciales: cada integrante crea sus propios valores en los archivos `.env` y `.env.compose` y sus datos de prueba, como indica la sección 3.

1. Clonen o actualicen los dos repositorios en develop. Si no los tienen, `git clone https://github.com/nexus-upc/rinde-services.git` y `git clone https://github.com/nexus-upc/rinde-apps.git`. Si ya los tienen, en cada uno ejecuten `git switch develop` y `git pull`.
2. Tengan Docker Desktop instalado y abierto.
3. En rinde-services, desde la raíz del repositorio, copien la plantilla del compose con `Copy-Item infra/.env.example .env.compose`. Después copien el `.env.example` de cada servicio a un archivo `.env` dentro de su carpeta: `gateway`, `services/iam`, `services/trip`, `services/fleet`, `services/expense`, `services/incident` y `services/core`.
4. Levanten el entorno desde la raíz de rinde-services con el comando del README: `docker compose --env-file .env.compose up -d --build`. El gateway queda en http://localhost:8000 y el Swagger reunido en http://localhost:8000/swagger-ui.html. Si `GET http://localhost:8000/api/v1/users/me` responde 401 sin token, el gateway está arriba.
5. En rinde-apps, desde la raíz del repositorio, ejecuten `npm install` y luego `npm start`. La web queda en http://localhost:4200 y llama al gateway.
6. En Postman, descarguen la colección desde https://github.com/nexus-upc/rinde-services/blob/develop/docs/sprint3/RINDE_Sprint3_Equipo.postman_collection.json e impórtenla desde el archivo descargado. La variable `baseUrl` debe valer http://localhost:8000. En el entorno local de Postman completen `adminEmail`, `adminPassword`, `driverEmail`, `driverPassword` y `webhookSecret` con los valores de su entorno, según la sección 3.
7. Los datos de prueba (empresa, administrador y conductores) los crea el script de la sección 3 en el entorno de cada integrante. Las cuentas quedan en `rinde-backend/.env.demo-company`, que es privado y no se sube a Git.
