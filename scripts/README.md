# Scripts de utilidad

## `seed-demo.ps1`

Script idempotente en PowerShell 7.2 o superior que carga un escenario completo de demostración vía HTTP contra el gateway de RINDE (`local` o Azure).

### Requisitos

- PowerShell 7.2 o superior (`pwsh`).
- Gateway y microservicios levantados (por ejemplo con `docker compose --env-file .env.compose up -d --build` o desplegados en Azure).
- Archivo `.env.demo-company` (copiado desde `.env.demo-company.example` en la raíz de `rinde-backend`) o variables de entorno equivalentes.

### Configuración

Copiar la plantilla y reemplazar los valores de plantilla:

```powershell
Copy-Item .env.demo-company.example .env.demo-company
```

Variables requeridas:

- `DEMO_COMPANY_RUC`: RUC ficticio de 11 dígitos numéricos.
- `DEMO_ADMIN_EMAIL`, `DEMO_ADMIN_PASSWORD`: cuenta `ADMINISTRATOR`.
- `DEMO_OPS_EMAIL`, `DEMO_OPS_PASSWORD`: cuenta `OPERATIONS_MANAGER`.
- `DEMO_DRIVER1_EMAIL`, `DEMO_DRIVER1_PASSWORD`: primera cuenta `DRIVER`.
- `DEMO_DRIVER2_EMAIL`, `DEMO_DRIVER2_PASSWORD`: segunda cuenta `DRIVER`.

Variables opcionales:

- `RINDE_GATEWAY_URL`: URL base del gateway (por defecto `http://localhost:8000`).
- `DEMO_COMPANY_TRADE_NAME`, `DEMO_COMPANY_PHONE`, `DEMO_COMPANY_ADDRESS`, `DEMO_*_NAME`: datos descriptivos de empresa y usuarios.
- `PLATFORM_OWNER_EMAIL`, `PLATFORM_OWNER_PASSWORD`: credenciales del dueño de plataforma para leer los enlaces de verificación e invitación desde `/api/v1/platform/demo-mailbox/messages` cuando el buzón de demostración está activo.
- `DEMO_VERIFICATION_TOKEN`: token manual de verificación de empresa cuando el buzón de demostración no está habilitado.

### Ejecución

Contra entorno local (leyendo `.env.demo-company` en la raíz):

```powershell
pwsh -File scripts/seed-demo.ps1
```

Contra Azure u otro entorno indicando URL y archivo de variables:

```powershell
pwsh -File scripts/seed-demo.ps1 -GatewayUrl https://mi-gateway.azurecontainerapps.io -EnvFile .env.demo-company
```

Si la empresa o las cuentas aún no están verificadas/activas y el buzón de demostración no está disponible en el entorno, el script informa qué verificación o invitación debe completarse antes de volver a ejecutarlo.

### Datos cargados

- 1 empresa activa con 4 cuentas activas (`ADMINISTRATOR`, `OPERATIONS_MANAGER` y 2 `DRIVER`).
- 2 conductores registrados en Fleet y vinculados por `userId`.
- 3 vehículos con mantenimientos en estados `UP_TO_DATE` (`RIN-101`), `DUE_SOON` (`RIN-102`) y `OVERDUE` (`RIN-103`).
- 5 viajes cubriendo `SCHEDULED`, `ASSIGNED`, `IN_ROUTE`, `FINISHED` y `SETTLED`, con adelantos en `CASH` y `TRANSFER`.
- Gastos con `idempotencyKey` fija y comprobantes PNG generados en memoria en estados `PENDING_SUPPORT`, `REGISTERED`, `APPROVED` y `OBSERVED`.
- Liquidaciones en estado `OPEN` (viaje `FINISHED`) y `CLOSED` (viaje `SETTLED`).
- Incidencias con `idempotencyKey` fija en estado `OPEN` y `CLOSED` (resuelta con acción tomada).

### Pruebas automatizadas del script

```powershell
pwsh -File tests/scripts/seed-demo.Tests.ps1
```
