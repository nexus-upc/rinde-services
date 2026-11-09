# rinde-expense-service

Servicio de Expense & Evidence de RINDE by NEXUS: gastos del viaje con su comprobante, sincronización offline y revisión. Usa `rinde-shared-kernel` 0.2.0.

## Ejecución

Requiere Java 21, PostgreSQL y RabbitMQ. Crear las bases `rinde_expense` y `rinde_expense_test`, copiar `.env.example` como `.env` y completar los valores (`DB_*`, `JWT_SECRET`, `IAM_SERVICE_URL`, `TRIP_SERVICE_URL` y `RABBITMQ_*`).

```
mvnw.cmd spring-boot:run
mvnw.cmd clean verify
```

Puerto 8084. Swagger: http://localhost:8084/swagger-ui.html. Flyway crea el esquema `expense`: gastos, evidencias y `processed_settlement_events`.

## Endpoints públicos

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/trips/{tripId}/expenses` | Registro del gasto por el conductor, idempotente por `idempotencyKey` (US21). Nace en `PENDING_SUPPORT`. |
| GET | `/api/v1/trips/{tripId}/expenses` | Gastos de un viaje (US23). |
| GET | `/api/v1/expenses/{id}` | Detalle del gasto con los metadatos de su comprobante. |
| PATCH | `/api/v1/expenses/{id}/status` | Aprobación u observación con motivo, por ADMINISTRATOR u OPERATIONS_MANAGER (US24). |
| POST | `/api/v1/expenses/sync` | Lote de gastos registrados sin conexión; informa los rechazados (US22). |
| POST | `/api/v1/expenses/{expenseId}/evidences` | Sube o reemplaza el comprobante en formulario multipart, campo `file`. Solo el conductor que registró el gasto (US21, QA-05). |
| GET | `/api/v1/expenses/{expenseId}/evidences/{evidenceId}/content` | Lee el archivo del comprobante con su `Content-Type` (US21, QA-05). |

Antes de registrar, el servicio consulta a Trip el estado del viaje y a IAM el estado de la empresa. Un gasto bloqueado por una liquidación cerrada ya no se modifica ni acepta comprobantes.

## Comprobantes

- Formatos: JPEG, PNG, WEBP y PDF. Se reconocen por la firma inicial del contenido, no por el nombre ni por el tipo que declara el cliente.
- Tamaño máximo: `RINDE_EVIDENCE_MAX_SIZE_BYTES`, 5 MB (5242880 bytes) por defecto. Por encima, la respuesta es 413. El límite del contenedor de multipart es 10 MB.
- Almacenamiento: el puerto `EvidenceStorage` del dominio tiene un adaptador de carpeta local, `RINDE_EVIDENCE_STORAGE_PATH` (`./data/evidences` por defecto). En Docker la carpeta es el volumen `expense-evidence-data`. Las claves son `{tenantId}/{expenseId}/{uuid}`; no son rutas que venga del cliente.
- Metadatos: nombre del archivo sin rutas, formato, tamaño, SHA-256, quién lo subió y cuándo. La lectura verifica la huella guardada antes de responder.
- Reemplazo: un comprobante nuevo sustituye al anterior en el mismo registro. El archivo anterior se borra después de confirmar el cambio.
- Lectura: el administrador y el responsable de operaciones de la empresa leen cualquier comprobante de sus gastos; el conductor, solo los suyos. Otro conductor recibe 403 y una empresa ajena recibe 404. La ruta `contentUrl` exige JWT: no es un enlace público.
- Sincronización offline: un gasto del lote nace sin comprobante. La foto se sube después con POST a `/api/v1/expenses/{expenseId}/evidences`, usando el `id` que devuelve la sincronización. Un campo `evidence` heredado en el lote se ignora.
- Comprobantes anteriores a V3: no tienen archivo. Su `contentUrl` es nulo y la lectura responde 404.

Configuración en `application.yml`:

| Variable | Valor por defecto | Uso |
|---|---|---|
| `RINDE_EVIDENCE_STORAGE_PATH` | `./data/evidences` | Carpeta de los archivos. |
| `RINDE_EVIDENCE_MAX_SIZE_BYTES` | `5242880` | Tamaño máximo de cada comprobante. |

## Endpoints internos

Sin JWT y fuera del gateway. Base `/api/internal/v1/expenses/tenants/{tenantId}`.

| Ruta | Lo consume |
|---|---|
| `/trips/{tripId}/approved` | Settlement, para calcular la liquidación |
| `/trips/{tripId}` | Dashboard, para el resumen del viaje |
| (raíz) | Dashboard, para las métricas de la empresa |

## Eventos

Consume `settlement.closed` y bloquea los gastos del viaje. Registra el `eventId` en `processed_settlement_events`, así que una entrega repetida no tiene efecto. Los mensajes que fallan quedan en `rinde.events.expense.settlement-events.dead`.

## Límites conocidos

- Solo hay almacenamiento local. El puerto está listo para un adaptador de objetos, pero no hay proveedor conectado.
- No se decodifica la imagen ni se analiza el archivo con antivirus; la validación es por firma inicial.
- Guardar el archivo y confirmar la base no es atómico: si la base falla después de guardar, puede quedar un archivo sin registro.
- Un gasto aprobado también admite comprobante nuevo mientras no esté bloqueado por liquidación. Falta decidir si la revisión debe congelar el comprobante.
