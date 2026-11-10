# RINDE by NEXUS - Documento de Diseño de Arquitectura y Despliegue en la Nube
**Módulo:** S3-10c - Diseño Cloud y Despliegue en la Nube (Sprint 3 / Avance 4)  
**Autor:** Mathias Andree Cárdenas Huamán (`u202316353@upc.edu.pe`)  
**Fecha:** Noviembre 2026  
**Línea Base del Sistema:** Release v0.3.1 (Backend `rinde-services`) / Release v0.3.0 (Frontend `rinde-apps`)  

---

## 1. Introducción y Objetivos de la Arquitectura Cloud

El presente documento formaliza el diseño de arquitectura y la estrategia de despliegue en la nube para la plataforma **RINDE by NEXUS**, una solución integral de gestión operativa y rendición de cuentas de transporte de carga en ruta (B2B SaaS).

A partir de la evolución modular experimentada entre los Sprints 1, 2 y 3, el sistema ha transitado desde un monolito inicial hacia una arquitectura distribuida orientada a microservicios. Este diseño establece las especificaciones técnicas para migrar los entornos locales de desarrollo y pruebas hacia una infraestructura en la nube elástica, segura, altamente disponible y con costos operacionales optimizados.

### 1.1. Principios Arquitectónicos Clave
1. **Desacoplamiento Operativo y Aislamiento de Datos (Database-per-Service):** Cada microservicio posee persistencia exclusiva sin compartición de esquemas ni consultas transaccionales cruzadas.
2. **Computación Serverless en Contenedores:** Eliminación de la administración manual de servidores mediante contenedores efímeros y autoescalables.
3. **Comunicación Asíncrona Confiable:** Mensajería guiada por eventos mediante RabbitMQ para garantizar consistencia eventual e idempotencia.
4. **Seguridad Integral (Zero Trust & Defense in Depth):** Cifrado en tránsito y en reposo, aislamiento en redes privadas virtuales (VPC), almacenamiento privado de evidencias con URLs prefirmadas (Pre-signed URLs), y estricta separación de permisos de red y base de datos.
5. **Observabilidad Distribuida:** Centralización de métricas, registros y trazabilidad de extremo a extremo propagando el identificador `x-correlation-id`.

---

## 2. Comparativa de Proveedores Cloud Viables

Para determinar el proveedor idóneo, se evaluaron exhaustivamente las tres plataformas de computación en la nube líderes del mercado: **Amazon Web Services (AWS)**, **Microsoft Azure** y **Google Cloud Platform (GCP)**, contrastando sus capacidades frente a los requerimientos técnicos específicos del ecosistema RINDE.

| Dimensión Técnica / Criterio | Amazon Web Services (AWS) | Microsoft Azure | Google Cloud Platform (GCP) |
|---|---|---|---|
| **Cómputo en Contenedores (Serverless)** | **AWS ECS con Fargate:**<br>Orquestación ligera sin gestionar instancias EC2. Compatible nativamente con imágenes Docker estándar Temurin 21. Asignación granular de roles IAM por tarea (Task Role). Escalado ágil por métricas de CPU/RAM. | **Azure Container Apps (ACA):**<br>Basado en Kubernetes (KEDA). Excelente integración con microservicios y escalado a cero, aunque mayor complejidad en configuración de redes virtuales dedicadas y depuración. | **Google Cloud Run:**<br>Altamente optimizado para contenedores sin estado con peticiones HTTP entrantes. Limitaciones en tareas de fondo continuas (workers de RabbitMQ y scheduled jobs como `MaintenanceDueJob`). |
| **Persistencia Relacional (PostgreSQL)** | **Amazon RDS for PostgreSQL 17:**<br>Soporte maduro, automatización de copias de seguridad continuas, réplicas de lectura, failover Multi-AZ transparente y compatibilidad total con extensiones y Flyway. | **Azure Database for PostgreSQL (Flexible Server):**<br>Excelente soporte y alta disponibilidad de zona redundante, pero costos base de cómputo y almacenamiento ligeramente superiores en niveles pequeños. | **Cloud SQL for PostgreSQL:**<br>Administración robusta y mantenimiento automatizado, aunque los tiempos de conmutación por error (failover) suelen ser mayores frente a RDS Multi-AZ. |
| **Broker de Mensajería (RabbitMQ)** | **Amazon MQ for RabbitMQ:**<br>Servicio totalmente administrado con compatibilidad nativa 100% AMQP 0-9-1. Soporta configuración de clúster activo/standby Multi-AZ. Sin cambios de código en Spring AMQP. | **No administrado nativamente:**<br>Azure ofrece Service Bus (protocolo AMQP 1.0 / proprietary), requiriendo adaptar todo el código a JMS/Azure SDK, o bien contratar un servicio de terceros (CloudAMQP). | **No administrado nativamente:**<br>GCP promueve Cloud Pub/Sub, lo que exigiría reemplazar RabbitMQ y reescribir la capa de infraestructura de mensajería de todos los microservicios. |
| **Almacenamiento de Evidencias y Privacidad** | **Amazon S3:**<br>Estándar industrial de almacenamiento de objetos. Cifrado SSE-S3 / SSE-KMS, ciclo de vida de objetos, bloqueo de acceso público y generación de Pre-signed URLs nativas y de bajo costo. | **Azure Blob Storage:**<br>Almacenamiento seguro y soporte para SAS (Shared Access Signatures). Curva de integración con Spring Cloud algo más compleja que el SDK estándar de S3. | **Google Cloud Storage (GCS):**<br>Excelente rendimiento y URLs firmadas muy ágiles, similar a S3 en costo y fiabilidad. |
| **Distribución de Frontend y Landing Page** | **Amazon CloudFront + S3:**<br>CDN global con presencia en Lima (Edge Location), compresión Brotli/Gzip, certificados SSL gratuitos vía ACM y reescritura de rutas para Angular SPA. | **Azure Static Web Apps / Front Door:**<br>Muy buena integración de CI/CD para frontend, pero costos de Front Door significativamente más altos para proyectos medianos. | **Firebase Hosting / Cloud CDN:**<br>Despliegue muy rápido y caché global, aunque la administración centralizada con VPC es más segmentada. |
| **Gestión de Secretos y Configuración** | **AWS Secrets Manager & SSM Parameter Store:**<br>Rotación automática de credenciales de base de datos, inyección transparente como variables de entorno a nivel de ECS Task Definition. | **Azure Key Vault:**<br>Altamente seguro y con RBAC granular, aunque requiere configuración adicional de identidades administradas en contenedores. | **Secret Manager:**<br>Simple y eficaz, integrado con Cloud IAM, costo por millón de operaciones. |
| **Observabilidad y Trazabilidad** | **CloudWatch Logs/Metrics + AWS X-Ray:**<br>Recolección nativa mediante driver `awslogs` en Fargate. Trazabilidad distribuida de microservicios con correlación por `x-correlation-id`. | **Azure Monitor & Application Insights:**<br>Muy potente para telemetría, pero la instrumentación Java suele requerir agentes dedicados más pesados. | **Cloud Logging & Cloud Trace:**<br>Visualización limpia y rápida, pero menos integrada con arquitecturas híbridas RabbitMQ. |

---

## 3. Justificación de la Elección de AWS como Proveedor Cloud

Tras el análisis comparativo, se selecciona formalmente **Amazon Web Services (AWS)** como el proveedor recomendado para el despliegue del ecosistema RINDE. Las razones determinantes son:

1. **Soporte Nativo Administrado de RabbitMQ (Amazon MQ):**  
   RINDE basa su desacoplamiento asíncrono y consistencia eventual en el broker RabbitMQ (eventos `trip.started`, `trip.finished`, `fleet.maintenance-due`, `settlement.closed`). AWS ofrece **Amazon MQ for RabbitMQ**, garantizando paridad exacta con el entorno local de Docker Compose sin alterar una sola línea de código, dependencias de Spring AMQP ni configuraciones de colas y exchanges. En contraste, Azure y GCP requerían proveedores externos (CloudAMQP) o la reescritura de la capa de mensajería hacia Azure Service Bus o Google Pub/Sub.

2. **Simplicidad Operativa con AWS ECS Fargate:**  
   Fargate permite desplegar contenedores basados en imágenes Temurin 21 sin aprovisionar ni parchar servidores EC2. Permite definir tareas granulares de 0.5 vCPU y 1 GB de RAM para cada microservicio, escalando horizontalmente ante picos de demanda y reduciendo el costo ocioso.

3. **Baja Latencia de Acceso en Perú y Sudamérica:**  
   CloudFront posee un Punto de Presencia (PoP / Edge Location) en Lima, Perú, lo cual permite entregar el frontend de la web y la landing page con tiempos de carga inferiores a 80 ms para operadores logísticos locales.

4. **Compatibilidad Estándar con el Ecosistema Spring Boot 3 y Angular 22:**  
   Las librerías del AWS SDK v2 para Java se integran orgánicamente con Spring Boot para la generación de URLs prefirmadas en S3, la inyección de secretos desde Secrets Manager y la trazabilidad con AWS X-Ray.

---

## 4. Arquitectura de Despliegue en AWS (Componente por Componente)

### 4.1. Topología de Red y Virtual Private Cloud (VPC)
La arquitectura de red se fundamenta en una VPC dedicada (`10.0.0.0/16`) segmentada en tres capas de subredes distribuidas en dos Zonas de Disponibilidad (Multi-AZ):

```
+----------------------------------------------------------------------------------------------------+
|                                    AWS REGION (us-east-1)                                          |
|                                                                                                    |
|  +----------------------------------------------------------------------------------------------+  |
|  | VPC (10.0.0.0/16)                                                                            |  |
|  |                                                                                              |  |
|  |   +------------------------------------+    +------------------------------------+           |  |
|  |   | Public Subnet AZ-A (10.0.1.0/24)   |    | Public Subnet AZ-B (10.0.2.0/24)   |           |  |
|  |   |  - Internet Gateway                |    |  - NAT Gateway AZ-B                |           |  |
|  |   |  - NAT Gateway AZ-A                |    |  - Application Load Balancer (ALB) |           |  |
|  |   |  - Application Load Balancer (ALB) |    +------------------------------------+           |  |
|  |   +------------------------------------+                                                     |  |
|  |                                                                                              |  |
|  |   +------------------------------------+    +------------------------------------+           |  |
|  |   | Private App Subnet AZ-A (10.0.11.0)|    | Private App Subnet AZ-B (10.0.12.0)|           |  |
|  |   |  - Gateway (ECS Fargate)           |    |  - Gateway (ECS Fargate)           |           |  |
|  |   |  - Core Microservice               |    |  - Core Microservice               |           |  |
|  |   |  - IAM, Trip, Fleet Microservices  |    |  - Expense, Incident Microservices |           |  |
|  |   |  - Settlement, Subscriptions Svcs  |    |  - Settlement, Subscriptions Svcs  |           |  |
|  |   +------------------------------------+    +------------------------------------+           |  |
|  |                                                                                              |  |
|  |   +------------------------------------+    +------------------------------------+           |  |
|  |   | Isolated DB Subnet AZ-A (10.0.21.0)|    | Isolated DB Subnet AZ-B (10.0.22.0)|           |  |
|  |   |  - RDS PostgreSQL Multi-AZ (Master)|    |  - RDS PostgreSQL Multi-AZ(Standby)|          |  |
|  |   |  - Amazon MQ RabbitMQ (Primary)    |    |  - Amazon MQ RabbitMQ (Standby)    |           |  |
|  |   +------------------------------------+    +------------------------------------+           |  |
|  +----------------------------------------------------------------------------------------------+  |
|                                                                                                    |
|  +--------------------+   +-----------------------+   +--------------------+   +----------------+  |
|  | S3 Evidence Bucket |   | CloudFront CDN + S3   |   | AWS Secrets Manager|   | CloudWatch     |  |
|  | (Private Storage)  |   | (Web & Landing SPA)   |   | & SSM Parameters   |   | & AWS X-Ray    |  |
|  +--------------------+   +-----------------------+   +--------------------+   +----------------+  |
+----------------------------------------------------------------------------------------------------+
```

1. **Subredes Públicas (`10.0.1.0/24`, `10.0.2.0/24`):**  
   Alojan el **Application Load Balancer (ALB)** público con certificación TLS de AWS Certificate Manager (ACM) y dos NAT Gateways (uno por AZ) para que los contenedores privados tengan salida a Internet controlada (descarga de parches, comunicación con APIs externas como Sunat o pasarelas de pago).
2. **Subredes Privadas de Aplicación (`10.0.11.0/24`, `10.0.12.0/24`):**  
   Alojan exclusivamente las tareas de **ECS Fargate**. Ningún microservicio tiene asignada dirección IP pública; la comunicación externa ingresa únicamente por enrutamiento seguro desde el ALB.
3. **Subredes Aisladas de Datos (`10.0.21.0/24`, `10.0.22.0/24`):**  
   Alojan las instancias de **Amazon RDS for PostgreSQL** y **Amazon MQ RabbitMQ**. No tienen tablas de ruteo hacia el Internet Gateway ni hacia el NAT Gateway, impidiendo cualquier vector de ataque directo desde el exterior.

---

### 4.2. Capa de Cómputo: Servicios y Contenedores (ECS Fargate)
Cada componente del backend se empaqueta como una tarea independiente de Amazon ECS ejecutándose en modo Fargate, respetando el inventario del repositorio y las asignaciones de recursos:

| Servicio / Contenedor | Puerto Interno | vCPU / Memoria Asignada | Rol Operativo y Dependencias | Autoescalado (Min - Max) |
|---|:---:|:---:|---|:---:|
| **gateway** | 8000 | 0.5 vCPU / 1 GB RAM | Spring Cloud Gateway; enrutamiento de peticiones, validación preliminar CORS, inyección de encabezados de contexto y balanceo interno. | 2 - 6 tareas |
| **core** | 8080 | 1.0 vCPU / 2 GB RAM | Contextos legacy / Dashboard / Auditoría. Depende de IAM, Trip, Fleet, Expense, Incident y RabbitMQ. | 2 - 4 tareas |
| **iam** | 8081 | 0.5 vCPU / 1 GB RAM | Autenticación, JWT, registro de inquilinos (`Tenant`), gestión de usuarios y roles (`ADMIN`, `OPERATOR`, `DRIVER`). | 2 - 4 tareas |
| **trip** | 8082 | 0.5 vCPU / 1 GB RAM | Planificación y ciclo de vida de viajes, asignación de unidades y choferes, emisión de eventos de viaje. | 2 - 5 tareas |
| **fleet** | 8083 | 0.5 vCPU / 1 GB RAM | Catálogo de vehículos, conductores, alertas de mantenimiento (`DUE_SOON` / `OVERDUE`) y endpoints internos. | 2 - 4 tareas |
| **expense** | 8084 | 0.5 vCPU / 1 GB RAM | Registro y revisión de comprobantes, gestión de evidencias adjuntas, validación de gastos. | 2 - 5 tareas |
| **incident** | 8085 | 0.5 vCPU / 1 GB RAM | Registro, gestión de estado y resolución de incidentes mecánicos y operativos en ruta. | 2 - 4 tareas |
| **settlement** | 8086 | 0.5 vCPU / 1 GB RAM | Liquidación contable de viajes, recálculo, solicitud de adelantos y cierre consolidado. | 2 - 4 tareas |
| **subscriptions** | 8087 | 0.5 vCPU / 1 GB RAM | Planes SaaS, límites de uso por empresa, facturación y procesamiento de webhooks de cobro. | 2 - 4 tareas |

---

### 4.3. Persistencia Relacional: Bases de Datos Independientes (Amazon RDS PostgreSQL 17)
En estricta correspondencia con el patrón **Database-per-Service**, se utiliza **Amazon RDS for PostgreSQL 17** en configuración **Multi-AZ**:

1. **Aislamiento de Bases de Datos:**  
   Se instancian bases de datos lógicamente aisladas dentro del clúster administrado:
   - `rinde_iam`: Usuarios, credenciales bcrypt, tenants y perfiles.
   - `rinde_trip`: Viajes, coordenadas de ruta, paradas y estados.
   - `rinde_fleet`: Flota vehicular, conductores, registros de mantenimiento y tabla de idempotencia `fleet.processed_trip_events`.
   - `rinde_expense`: Gastos registrados, montos, categorías y metadatos de comprobantes.
   - `rinde_incident`: Incidentes en ruta, severidad y bitácora de resolución.
   - `rinde_settlement`: Liquidaciones de viaje, balances y adelantos.
   - `rinde_subscription`: Suscripciones activas, planes y transacciones de facturación.
   - `rinde`: Base compartida del núcleo residual de `core`.
2. **Seguridad y Parámetros:**
   - Cifrado en reposo habilitado con clave administrada de AWS KMS (`aws/rds`).
   - Forzado de conexiones cifradas: parámetro de grupo `rds.force_ssl = 1`.
   - Usuarios específicos con principio de mínimo privilegio: el usuario `fleet_app` únicamente tiene acceso al esquema `fleet` en `rinde_fleet`, sin privilegios de lectura en otras bases de datos.
3. **Migraciones con Flyway:**
   - Las migraciones de Flyway no se ejecutan indiscriminadamente en el arranque de múltiples tareas concurrentes de ECS para evitar contención de locks en `flyway_schema_history`.
   - Se ejecutan mediante una tarea puntual de ECS (**ECS Task de Migración / Init Container**) gatillada por el pipeline de CI/CD previo al despliegue de las tareas de la aplicación.
   - El usuario de migración (`flyway_migrator`) cuenta con permisos DDL (`CREATE`, `ALTER`, `DROP`); el usuario de aplicación en tiempo de ejecución solo posee permisos DML (`SELECT`, `INSERT`, `UPDATE`, `DELETE`).

---

### 4.4. Broker de Mensajería Asíncrona (Amazon MQ for RabbitMQ)
1. **Topología de Despliegue:**
   - Broker administrado **Amazon MQ for RabbitMQ** desplegado en modo **Multi-AZ (Active/Standby)** en las subredes aisladas.
   - Paridad total con el entorno local: protocolo **AMQP 0-9-1**, puerto 5671 (AMQPS con TLS).
2. **Estructura de Exchanges y Enrutamiento:**
   - Exchange principal: `rinde.events` (Tipo: `topic`, Durable).
   - Colas dedicadas por servicio con Dead Letter Exchange (DLX) para mensajes fallidos tras 3 reintentos:
     - `rinde.events.fleet.trip-events`: Para que Fleet consuma `trip.started` y `trip.finished` con inserción idempotente.
     - `rinde.events.settlement.trip-events`: Para que Settlement consuma `trip.finished`.
     - `rinde.events.core.notifications`: Para notificaciones de alertas y correos.

---

### 4.5. Almacenamiento Seguro de Evidencias (Amazon S3)
En el entorno local, `services/expense` persiste las fotos de comprobantes en el volumen `expense-evidence-data`. En la nube, este volumen efímero se sustituye por **Amazon S3**:

1. **Configuración del Bucket Privado:**
   - Nombre: `s3-rinde-expenses-evidence-prod`.
   - **Block Public Access:** Activado en sus 4 niveles (sin acceso público bajo ninguna circunstancia).
   - Cifrado por defecto: SSE-S3 (Server-Side Encryption con llaves AES-256 administradas).
2. **Flujo de Carga y Descarga mediante Pre-signed URLs:**
   - **Subida de Evidencia (`POST /api/v1/expenses/{id}/evidences`):**
     1. El cliente móvil o web envía el archivo multipart al microservicio `expense` con su token Bearer.
     2. El microservicio valida los permisos del usuario y el estado del gasto (`REGISTERED` o `PENDING_SUPPORT`).
     3. El microservicio genera un Object Key único con formato: `tenants/{tenantId}/expenses/{expenseId}/{uuid}.webp`.
     4. `expense` transfiere el archivo de forma segura a S3 usando el SDK de AWS autenticado con su rol IAM, o emite una Pre-signed URL de subida (`PUT`) con caducidad de 10 minutos.
   - **Lectura Segura (`GET /api/v1/expenses/{id}/evidences/{evidenceId}/content`):**
     1. El revisor (auditor) solicita la evidencia.
     2. `expense` valida que el usuario pertenezca al `tenantId` correspondiente.
     3. El microservicio genera una **Pre-signed URL** temporal con vigencia de 5 minutos y responde un código de redirección temporal `307 Temporary Redirect` hacia S3, garantizando que el bucket permanezca 100% privado.

---

### 4.6. Frontend Web y Landing Page (Amazon CloudFront + S3)
1. **Alojamiento Estático:**
   - Bucket S3 `rinde-web-app-prod` (para la SPA Angular de `apps/web`).
   - Bucket S3 `rinde-landing-page-prod` (para la página pública de `apps/landing-page`).
2. **Distribución CDN (CloudFront):**
   - Se configuran dos distribuciones de CloudFront vinculadas mediante **Origin Access Control (OAC)** para que únicamente CloudFront pueda leer los objetos de los buckets S3.
   - **Soporte SPA (Angular Router):** Se configuran Custom Error Responses en CloudFront: cuando la petición responda HTTP 403 o 404, CloudFront devuelve HTTP 200 con el contenido de `/index.html`, permitiendo el funcionamiento fluido del routing del lado del cliente (`/fleet`, `/drivers`, `/trips/:id`, `/settlements`).
   - Certificado SSL/TLS emitido gratuitamente mediante **AWS Certificate Manager (ACM)** para los dominios `app.rinde.pe` y `www.rinde.pe`.

---

### 4.7. Gestión de Secretos y Configuración
1. **AWS Secrets Manager:**
   - Almacena credenciales altamente sensibles con rotación periódica automatizada:
     - Contraseñas maestras de PostgreSQL por microservicio.
     - Credenciales de autenticación para Amazon MQ RabbitMQ.
     - Clave secreta simétrica HMAC-SHA256 para validación de tokens JWT en IAM y Gateway.
     - Claves secretas de webhooks de pasarela de pagos (Stripe / Culqi).
2. **AWS Systems Manager Parameter Store (SSM):**
   - Almacena variables de entorno de configuración no confidencial (URLs de microservicios internos, nombres de colas, parámetros de timeout y flags de características).
3. **Inyección en Contenedores:**
   - Las variables se inyectan directamente en la sección `secrets` y `environment` de las ECS Task Definitions, impidiendo que contraseñas queden expuestas en los repositorios Git o en los Dockerfiles.

---

### 4.8. Acceso y Conectividad desde el Cliente Móvil Android

El APK del conductor (`apps/mobile`), compilado bajo el módulo Android, cuenta con consideraciones críticas de seguridad de red descritas en su especificación de arquitectura (`apps/mobile/README.md`):

1. **Restricción de Tráfico en Claro (Cleartext Traffic):**
   - En Android, la configuración de seguridad de red (`network_security_config.xml`) inhabilita de manera predeterminada el tráfico HTTP no cifrado:
     ```xml
     <network-security-config>
         <base-config cleartextTrafficPermitted="false">
             <trust-anchors>
                 <certificates src="system" />
             </trust-anchors>
         </base-config>
         <debug-overrides>
             <trust-anchors>
                 <certificates src="user" />
             </trust-anchors>
         </debug-overrides>
     </network-security-config>
     ```
2. **Diferenciación de Entornos (Debug vs. Release):**
   - **Compilación de Depuración (`debug`):** Permite el uso de HTTP hacia `http://10.0.2.2:8000` (emulador local de Android Studio) o hacia la IP privada de la red local del desarrollador.
   - **Compilación de Producción / Publicada (`release`):** El sistema operativo Android rechaza de forma estricta cualquier conexión HTTP. La aplicación móvil se conecta obligatoriamente mediante **HTTPS (puerto 443)** hacia el dominio público `api.rinde.pe` gestionado por el Application Load Balancer de AWS, con certificado emitido por una Autoridad de Certificación reconocida por el sistema operativo (Amazon Trust Services).
3. **Tolerancia a Conexión Intermitente:**
   - Para las fotos de comprobantes e incidentes reportados en ruta, el cliente móvil implementa una cola local en Room Database; una vez restablecida la conectividad HTTPS, despacha las peticiones con reintentos exponenciales y encabezados de autenticación JWT.

---

### 4.9. Observabilidad, Monitoreo y Trazabilidad

1. **Centralización de Registros (CloudWatch Logs):**
   - Cada contenedor Fargate utiliza el log driver `awslogs` para transmitir stdout/stderr hacia grupos de registros `/ecs/rinde/{service-name}` con una política de retención de 30 días.
2. **Métricas y Alarmas (CloudWatch Metrics & Alarms):**
   - Alarmas automáticas enviadas vía Amazon SNS ante:
     - Consumo de CPU de cualquier tarea de ECS > 75% durante 5 minutos.
     - Memoria disponible en RDS < 15%.
     - Tasa de respuestas HTTP 5xx en el Gateway > 1% del tráfico total.
     - Longitud de colas de RabbitMQ > 1,000 mensajes sin procesar.
3. **Trazabilidad Distribuida (AWS X-Ray & `x-correlation-id`):**
   - Propagación obligatoria del encabezado HTTP `x-correlation-id` desde el API Gateway hacia los microservicios downstream (`trip`, `fleet`, `expense`, etc.).
   - En peticiones asíncronas, el identificador de correlación se adjunta en los encabezados del mensaje AMQP (`properties.headers['x-correlation-id']`), permitiendo seguir la traza de un viaje desde su inicio en Android hasta la alerta preventiva de mantenimiento y su liquidación.

---

### 4.10. Alta Disponibilidad (HA) y Recuperación ante Desastres (Disaster Recovery)

1. **Estrategia Multi-AZ:**
   - Todas las capas de la arquitectura (ALB, ECS Fargate, RDS PostgreSQL y Amazon MQ) están desplegadas en al menos dos Zonas de Disponibilidad independientes.
   - Si una Zona de Disponibilidad sufre una falla eléctrica o de red masiva, el tráfico es desviado automáticamente a la zona redundante sin interrupción visible para los usuarios finales.
2. **Métricas de Resiliencia:**
   - **RPO (Recovery Point Objective) < 5 minutos:**  
     Garantizado por los Transaction Logs continuos (WAL) de Amazon RDS y la replicación automática de snapshots diarios hacia una segunda región (`sa-east-1` o `us-west-2`).
   - **RTO (Recovery Time Objective) < 15 minutos:**  
     La arquitectura basada en contenedores ECS Fargate y CloudFormation / Terraform permite regenerar el cluster completo de computación de forma automatizada ante cualquier desastre crítico.

---

## 5. Estimación de Costos de Infraestructura Cloud

> **AVISO IMPORTANTE:**  
> **Todos los costos unitarios y mensuales descritos en esta sección están marcados formal y explícitamente como "POR VERIFICAR".**  
> Estas estimaciones representan aproximaciones basadas en la calculadora oficial de AWS Pricing Calculator para cargas de trabajo iniciales en la región de `us-east-1` (N. Virginia), sujetas a validación comercial y pruebas de carga reales durante las fases de integración y producción.

### 5.1. Tabla Comparativa de Costos Mensuales Estimados por Proveedor (por verificar)

| Categoría de Servicio | AWS (Amazon Web Services) | Microsoft Azure | Google Cloud Platform (GCP) | Estado de Verificación |
|---|:---:|:---:|:---:|:---:|
| **Cómputo (Contenedores)** | USD 98,50 *(ECS Fargate)* | USD 112,00 *(Azure Container Apps)* | USD 89,00 *(Cloud Run)* | **por verificar** |
| **Balanceador de Carga / Red** | USD 24,00 *(Application Load Balancer)* | USD 28,00 *(Application Gateway)* | USD 22,00 *(Cloud Load Balancing)* | **por verificar** |
| **Persistencia (PostgreSQL)** | USD 76,00 *(RDS Multi-AZ db.t4g.small)* | USD 84,00 *(Flexible Server Standard_B2s)* | USD 78,00 *(Cloud SQL db-custom-2-4)* | **por verificar** |
| **Mensajería (RabbitMQ)** | USD 65,00 *(Amazon MQ mq.t3.micro)* | USD 85,00 *(CloudAMQP en Azure)* | USD 80,00 *(CloudAMQP en GCP)* | **por verificar** |
| **Almacenamiento de Evidencias** | USD 6,50 *(S3 Standard 100 GB + egress)* | USD 7,20 *(Azure Blob 100 GB)* | USD 6,80 *(Cloud Storage 100 GB)* | **por verificar** |
| **CDN y Distribución Frontend** | USD 8,00 *(CloudFront 150 GB)* | USD 12,00 *(Azure CDN)* | USD 9,50 *(Cloud CDN)* | **por verificar** |
| **Gestión de Secretos** | USD 4,00 *(Secrets Manager 10 secretos)* | USD 3,50 *(Key Vault)* | USD 3,00 *(Secret Manager)* | **por verificar** |
| **Observabilidad y Logs** | USD 15,00 *(CloudWatch 20 GB logs)* | USD 18,00 *(Azure Monitor Log Analytics)* | USD 14,00 *(Cloud Logging)* | **por verificar** |
| **TOTAL MENSUAL ESTIMADO** | **USD 297,00** | **USD 349,70** | **USD 302,30** | **TODOS POR VERIFICAR** |

---

### 5.2. Desglose Detallado de la Solución Recomendada en AWS (por verificar)

| Componente AWS | Especificación / Dimensionamiento | Costo Unitario Estimado | Costo Mensual Estimado (USD) | Condición |
|---|---|---|:---:|:---:|
| **ECS Fargate (Gateway)** | 2 tareas × (0.5 vCPU, 1 GB RAM) activas 24/7 | USD 0,0244 / hora por tarea | USD 35,60 | **por verificar** |
| **ECS Fargate (8 Microservicios)** | Tareas ligeras combinadas (promedio 4 vCPU y 8 GB RAM totales concurrentes) | USD 0,0488 / vCPU-h + RAM | USD 62,90 | **por verificar** |
| **Application Load Balancer (ALB)** | 1 balanceador regional + LCU consumidas | USD 0,0225 / hora + LCU | USD 24,00 | **por verificar** |
| **Amazon RDS for PostgreSQL** | Instancia `db.t4g.small` (2 vCPU, 2 GB RAM, 50 GB SSD gp3, Multi-AZ) | USD 0,068 / hora + disco | USD 76,00 | **por verificar** |
| **Amazon MQ for RabbitMQ** | Instancia `mq.t3.micro` Multi-AZ administrada (2 brokers) | USD 0,086 / hora | USD 65,00 | **por verificar** |
| **Amazon S3 (Evidencias)** | 100 GB de comprobantes (fotos webp) + 20,000 peticiones PUT/GET | USD 0,023 / GB + requests | USD 6,50 | **por verificar** |
| **Amazon CloudFront** | 150 GB de transferencia de datos hacia clientes web y móviles | USD 0,085 / GB (primeros TB) | USD 8,00 | **por verificar** |
| **AWS Secrets Manager** | 10 secretos almacenados (DBs, RabbitMQ, JWT, Webhooks) | USD 0,40 / secreto / mes | USD 4,00 | **por verificar** |
| **Amazon CloudWatch** | 20 GB de ingesta de logs + 10 alarmas métricas | USD 0,50 / GB ingesta | USD 15,00 | **por verificar** |
| **AWS Certificate Manager (ACM)** | Certificados SSL/TLS públicos para dominios propios | Sin costo adicional | USD 0,00 | **por verificar** |
| **TOTAL ESTIMADO AWS** | Infraestructura completa de producción Multi-AZ | — | **USD 297,00** | **por verificar** |

---

## 6. Conclusiones y Próximos Pasos de Implementación

1. **Viabilidad Técnica y Operativa:**  
   La arquitectura diseñada sobre AWS garantiza una transición transparente desde el entorno Docker Compose local hacia un esquema de producción resiliente, preservando intactos los contratos HTTP del API Gateway y la topología de eventos RabbitMQ.
2. **Cumplimiento de Seguridad y Normativa:**  
   El uso de Pre-signed URLs en S3 y la obligatoriedad de HTTPS para el APK de Android aseguran el cumplimiento riguroso de la confidencialidad de comprobantes contables y datos operativos de transporte.
3. **Control Presupuestario:**  
   El costo estimado mensual de **USD 297,00 (por verificar)** resulta altamente competitivo y sustentable para una flota inicial en fase de piloto comercial, con posibilidad de optimización mediante Savings Plans o instancias reservadas.
