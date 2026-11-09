# rinde-iam-service

Servicio de IAM & Tenancy de RINDE by NEXUS: empresas, usuarios, roles, inicio de sesión y emisión de los tokens JWT. Es el primer servicio extraído del monolito modular `rinde-backend` (Sprint 2, tarea S2-02). Usa la librería `rinde-shared-kernel` (com.nexus:rinde-shared-kernel:0.2.0). También autentica al dueño de Rinde con una identidad de plataforma separada.

## Requisitos

- Java 21.
- PostgreSQL 17 en local (sin Docker).
- La librería `rinde-shared-kernel` 0.2.0 instalada en el repositorio local de Maven, o disponible en GitHub Packages.

## Configuración

Los secretos y los datos de conexión se leen del archivo `.env` en la raíz del proyecto, que Git ignora. Para crearlo, copia `.env.example` y completa sus valores.

| Variable | Descripción |
| --- | --- |
| `DB_URL` | URL JDBC de la base del servicio, por ejemplo `jdbc:postgresql://localhost:5432/rinde_iam`. |
| `DB_USERNAME` | Usuario de PostgreSQL. |
| `DB_PASSWORD` | Contraseña de PostgreSQL. |
| `JWT_SECRET` | Secreto de firma de los tokens. Debe ser el mismo que usa el monolito y tener al menos 32 caracteres. |
| `WEB_BASE_URL` | URL de la web Angular, por ejemplo `http://localhost:4200`. Es la base de los enlaces de acceso. Si no se define, toma ese valor (`rinde.web.base-url`). |
| `RABBITMQ_*`, `RINDE_EVENTS_EXCHANGE`, `RINDE_IAM_SUBSCRIPTION_QUEUE` | Conexión y nombres de la mensajería de suscripción (ver "Eventos"). |
| `RINDE_PLATFORM_JWT_SECRET` | Secreto de firma de los tokens de plataforma. Debe ser distinto de `JWT_SECRET`; sin él no se habilita el ingreso del dueño. |
| `RINDE_PLATFORM_BOOTSTRAP_*` | Alta inicial optativa del dueño: `ENABLED`, `NAME`, `EMAIL` y `PASSWORD`. Solo en el entorno privado; no modifica una cuenta existente. |
| `RINDE_DEMO_MAILBOX_ENABLED` | Habilita el buzón de demostración. Por defecto `false`. |
| `RINDE_DEMO_MAILBOX_ENCRYPTION_KEY` | Clave AES de 64 caracteres hexadecimales para cifrar los tokens del buzón. Obligatoria si el buzón está habilitado. |

El puerto del servicio es el 8081 (`server.port` en `application.yml`). La vigencia de los enlaces de invitación, verificación y recuperación se configura en `rinde.iam.tokens`.

### Enlaces de acceso para la web

No hay un proveedor de correo. Los enlaces de acceso se obtienen de dos formas, según `RINDE_DEMO_MAILBOX_ENABLED`.

Con el buzón deshabilitado, valor por defecto, el servicio registra los enlaces en el log. En INFO solo aparece el token enmascarado. En DEBUG aparece el enlace completo que usará la web:

| Enlace | Formato |
| --- | --- |
| Verificación de la empresa | `{WEB_BASE_URL}/verify-email?tenant={tenantId}&token={token}` |
| Invitación de usuario | `{WEB_BASE_URL}/accept-invitation?token={token}` |
| Recuperación de contraseña | `{WEB_BASE_URL}/reset-password?token={token}` |

Para ver los enlaces, activa el nivel DEBUG de la clase que los registra: `logging.level.com.nexus.rinde.iam.application.internal.eventhandlers.AccessTokenLoggingEventHandler=DEBUG`.

Con el buzón habilitado, ese registro en el log no se instancia. Cada evento de verificación, invitación o recuperación guarda un mensaje en `iam.demo_mail_messages` dentro de la misma transacción que crea el enlace; si la operación se revierte, no queda mensaje. El token se guarda cifrado con AES-GCM y su hash permite saber si sigue vigente. Solo el dueño de Rinde puede listar y abrir los mensajes. El listado no devuelve tokens ni enlaces, y el detalle ofrece la acción solo si el enlace no se usó, no venció y la cuenta no está deshabilitada. Al consumir un enlace se bloquea su fila para que dos solicitudes simultáneas no usen el mismo token. El servicio no arranca con el buzón habilitado en los perfiles `prod` o `production`.

## Base de datos

El servicio usa su propia base y su propio esquema `iam`. Crea las bases `rinde_iam` (servicio) y `rinde_iam_test` (pruebas) en PostgreSQL:

```sql
CREATE DATABASE rinde_iam;
CREATE DATABASE rinde_iam_test;
```

Al arrancar, Flyway crea el esquema `iam` y aplica las migraciones de `src/main/resources/db/migration/iam`: V1 con empresas, usuarios y tokens de acceso; V2 con los recibos de eventos de suscripción; V3 con la cuenta del dueño, la suspensión administrativa y la auditoría de plataforma; y V4 con `iam.demo_mail_messages`.

## Ejecución

```
mvnw.cmd spring-boot:run
```

Con el servicio arriba, la documentación está en `http://localhost:8081/swagger-ui.html` y el estado en `http://localhost:8081/actuator/health`.

## Pruebas

```
mvnw.cmd clean verify
```

Ejecuta las pruebas unitarias, las de integración (contra `rinde_iam_test`) y las de aceptación de `src/test/resources/features/iam`.

## Endpoints

Endpoints públicos. Los de acceso y registro no llevan JWT; el resto requiere `Authorization: Bearer <token>`.

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/api/v1/auth/sign-in` | Inicio de sesión (US09). |
| POST | `/api/v1/auth/password-reset` | Solicitud de recuperación de contraseña (US10). |
| POST | `/api/v1/auth/password` | Definición de contraseña con el enlace recibido (US10 y US11). |
| POST | `/api/v1/tenants` | Registro de empresa y de su administrador (US08). |
| POST | `/api/v1/tenants/{id}/verification` | Verificación del correo de la empresa (US08). |
| GET | `/api/v1/users/me` | Perfil del usuario que inicia sesión, con los datos de su empresa (cualquier rol). Responde `{id, fullName, email, role, status, tenant: {id, tradeName, ruc, status}}`. |
| GET | `/api/v1/users` | Listado de usuarios de la empresa (US11). Con `?role=DRIVER` también lo usa el responsable de operaciones, que solo ve cuentas DRIVER de su empresa; sin `role` o con otro rol responde 403. Un rol inexistente responde 400. |
| POST | `/api/v1/users` | Invitación de usuario (US11). |
| PATCH | `/api/v1/users/{id}` | Cambio de rol o de estado de un usuario (US11). |

Endpoints de plataforma. Requieren un token de plataforma con `ROLE_PLATFORM_OWNER`, salvo el inicio de sesión. Un token de empresa no los usa, y un token de plataforma no sirve en los endpoints de empresa.

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/api/v1/platform/auth/sign-in` | Inicio de sesión del dueño. Emite un token con emisor `rinde-platform`, audiencia `rinde-platform-api` y vigencia de 15 minutos. |
| GET | `/api/v1/platform/me` | Perfil del dueño. |
| GET | `/api/v1/platform/overview` | Resumen de empresas y usuarios. |
| GET | `/api/v1/platform/companies` | Empresas con búsqueda y paginación. |
| PATCH | `/api/v1/platform/companies/{id}/access` | Suspensión o restablecimiento administrativo del acceso, con motivo obligatorio. No cambia el estado comercial de la suscripción. |
| GET | `/api/v1/platform/audit/identity` | Auditoría de los cambios de acceso, solo lectura. |
| GET | `/api/v1/platform/demo-mailbox/messages` | Mensajes del buzón de demostración, con búsqueda, filtro por tipo y paginación. Solo con el buzón habilitado. |
| GET | `/api/v1/platform/demo-mailbox/messages/{id}` | Detalle de un mensaje y, si el enlace sigue vigente, su acción. |

`GET /api/v1/demo/features` es público y solo informa si el buzón está habilitado.

Endpoints internos. Los llaman otros servicios de RINDE sin JWT, por eso el gateway no debe enrutar `/api/internal`. Están documentados en Swagger con la etiqueta `Internal`.

| Método | Ruta | Respuesta |
| --- | --- | --- |
| GET | `/api/internal/v1/tenants/{tenantId}/status` | `{tenantId, status}`, con el estado actual de la empresa. |
| GET | `/api/internal/v1/tenants/{tenantId}/users/{userId}` | Datos del usuario: `userId`, `tenantId`, `fullName`, `email`, `role` y `status`. |
| GET | `/api/internal/v1/tenants/{tenantId}/users/{userId}/active-role?role=DRIVER` | `{active}`, que indica si el usuario está activo y tiene el rol. |
| GET | `/api/internal/v1/tenants/{tenantId}/administrator-email` | `{email}`, con el correo del administrador activo. |
| GET | `/api/internal/v1/tenant-directory` | Nombres de un conjunto acotado de empresas, para las consultas globales de facturación de core. |

Las respuestas de error siguen el formato `application/problem+json`. Un recurso de otra empresa responde 404.

## Eventos

El servicio recibe JSON desde el exchange durable `rinde.events`, con las routing keys
`subscription.activated` y `subscription.suspended`. Ambas llegan a la cola durable
`iam.subscription-events`, con un consumidor y prefetch de uno. El contrato propio contiene
`eventId`, `occurredAt` y `tenantId`; no se cargan clases Java del productor.

La migración V2 crea `iam.subscription_event_receipts`. La recepción bloquea la empresa,
aplica el estado y guarda el identificador del evento en una sola transacción. Los duplicados
se ignoran incluso después de otro cambio de estado; tampoco se aplica un evento con fecha
anterior al último recibido para esa empresa. Los eventos no activan empresas pendientes de
verificar el correo. Se conservan los listeners locales para las pruebas y la transición.

Los fallos se reintentan hasta tres veces. Si falta la empresa o el JSON no es válido, el
mensaje queda en `iam.subscription-events.dead`, para inspección y reenvío con el mismo eventId
después de corregir la causa. Las variables `RABBITMQ_*` y `RINDE_IAM_SUBSCRIPTION_QUEUE` están en
`.env.example`. Las pruebas de mensajería requieren RabbitMQ y usan colas aleatorias aisladas.

## Estructura

El paquete `com.nexus.rinde.iam` se organiza en las capas `domain`, `application`, `infrastructure` e `interfaces`. El paquete `com.nexus.rinde` raíz permite que Spring cargue los beans de `rinde-shared-kernel`.
