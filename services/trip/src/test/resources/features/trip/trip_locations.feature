# language: es
Característica: S4-33 Registro y consulta de ubicación del camión en ruta
  Como conductor en ruta y responsable de operaciones
  Quiero registrar posiciones periódicas del viaje y consultar la última posición activa
  Para conocer dónde está cada camión en curso sin rastrear fuera del viaje

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Carlos Vega" con el correo "carlos@andes.pe" y el rol "OPERATIONS_MANAGER"
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que existe el vehículo "camión-1"

  Escenario: Registro de puntos aceptado en un viaje en ruta
    Cuando el administrador programa el viaje "viaje-1" de "Lima" a "Trujillo" con carga "Repuestos" y salida "HOY+1"
    Y el administrador asigna el viaje "viaje-1" al vehículo "camión-1" y al conductor "luis@andes.pe"
    Y el conductor "luis@andes.pe" inicia el viaje "viaje-1"
    Cuando el conductor "luis@andes.pe" registra ubicaciones para el viaje "viaje-1":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 12.50          | AHORA-10m  |
      | -11.981234 | -77.061111  | 8.20           | AHORA-7m   |
    Entonces la respuesta tiene código 202
    Y el lote informa 2 guardados y 0 repetidos
    Y en la base de datos hay 2 ubicaciones registradas para el viaje "viaje-1"

  Escenario: Rechazo al registrar ubicación en un viaje no iniciado
    Cuando el administrador programa el viaje "viaje-1" de "Lima" a "Trujillo" con carga "Repuestos" y salida "HOY+1"
    Y el administrador asigna el viaje "viaje-1" al vehículo "camión-1" y al conductor "luis@andes.pe"
    Cuando el conductor "luis@andes.pe" registra ubicaciones para el viaje "viaje-1":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 12.50          | AHORA-10m  |
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "Solo se registra la ubicación de un viaje en ruta."

  Escenario: Rechazo al registrar ubicación en un viaje finalizado
    Cuando el administrador programa el viaje "viaje-1" de "Lima" a "Trujillo" con carga "Repuestos" y salida "HOY+1"
    Y el administrador asigna el viaje "viaje-1" al vehículo "camión-1" y al conductor "luis@andes.pe"
    Y el conductor "luis@andes.pe" inicia el viaje "viaje-1"
    Y el conductor "luis@andes.pe" finaliza el viaje "viaje-1"
    Cuando el conductor "luis@andes.pe" registra ubicaciones para el viaje "viaje-1":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 12.50          | AHORA-10m  |
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "Solo se registra la ubicación de un viaje en ruta."

  Escenario: Conductor ajeno recibe 404 al registrar ubicación
    Dado que el administrador invitó a "Marta Ríos" con el correo "marta@andes.pe" y el rol "DRIVER"
    Y que "marta@andes.pe" definió la contraseña "Clave-De-Marta-2026" con su enlace de invitación
    Cuando el administrador programa el viaje "viaje-1" de "Lima" a "Trujillo" con carga "Repuestos" y salida "HOY+1"
    Y el administrador asigna el viaje "viaje-1" al vehículo "camión-1" y al conductor "luis@andes.pe"
    Y el conductor "luis@andes.pe" inicia el viaje "viaje-1"
    Cuando el conductor "marta@andes.pe" registra ubicaciones para el viaje "viaje-1":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 12.50          | AHORA-10m  |
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El viaje no existe."

  Escenario: Responsable de operaciones no puede registrar ubicación
    Cuando el administrador programa el viaje "viaje-1" de "Lima" a "Trujillo" con carga "Repuestos" y salida "HOY+1"
    Y el administrador asigna el viaje "viaje-1" al vehículo "camión-1" y al conductor "luis@andes.pe"
    Y el conductor "luis@andes.pe" inicia el viaje "viaje-1"
    Cuando el usuario "carlos@andes.pe" intenta registrar ubicaciones para el viaje "viaje-1":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 12.50          | AHORA-10m  |
    Entonces la respuesta tiene código 403

  Escenario: Punto con coordenadas o precisión inválidas devuelve error por campo
    Cuando el administrador programa el viaje "viaje-1" de "Lima" a "Trujillo" con carga "Repuestos" y salida "HOY+1"
    Y el administrador asigna el viaje "viaje-1" al vehículo "camión-1" y al conductor "luis@andes.pe"
    Y el conductor "luis@andes.pe" inicia el viaje "viaje-1"
    Cuando el conductor "luis@andes.pe" intenta registrar ubicaciones inválidas para el viaje "viaje-1":
      | latitude | longitude | accuracyMeters | recordedAt |
      | 95.0000  | -185.0000 | -1.00          | AHORA-10m  |
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "La solicitud tiene datos inválidos."
    Y la respuesta incluye errores de validación en los campos "points[0].latitude,points[0].longitude,points[0].accuracyMeters"

  Escenario: Punto con hora en el futuro devuelve error por campo
    Cuando el administrador programa el viaje "viaje-1" de "Lima" a "Trujillo" con carga "Repuestos" y salida "HOY+1"
    Y el administrador asigna el viaje "viaje-1" al vehículo "camión-1" y al conductor "luis@andes.pe"
    Y el conductor "luis@andes.pe" inicia el viaje "viaje-1"
    Cuando el conductor "luis@andes.pe" intenta registrar ubicaciones inválidas para el viaje "viaje-1":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 10.00          | AHORA+10m  |
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "La solicitud tiene datos inválidos."
    Y el campo "points[0].recordedAt" tiene el error de validación "La hora del punto no puede estar en el futuro."

  Escenario: Punto repetido con el mismo recordedAt no se duplica
    Cuando el administrador programa el viaje "viaje-1" de "Lima" a "Trujillo" con carga "Repuestos" y salida "HOY+1"
    Y el administrador asigna el viaje "viaje-1" al vehículo "camión-1" y al conductor "luis@andes.pe"
    Y el conductor "luis@andes.pe" inicia el viaje "viaje-1"
    Y el conductor "luis@andes.pe" registró ubicaciones para el viaje "viaje-1":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 12.50          | AHORA-10m  |
    Cuando el conductor "luis@andes.pe" registra ubicaciones para el viaje "viaje-1":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 12.50          | AHORA-10m  |
      | -11.981234 | -77.061111  | 9.00           | AHORA-7m   |
    Entonces la respuesta tiene código 202
    Y el lote informa 1 guardados y 1 repetidos
    Y en la base de datos hay 2 ubicaciones registradas para el viaje "viaje-1"

  Escenario: Consulta de últimos puntos devuelve viajes en ruta con y sin ubicación y excluye finalizados
    Dado que existe el vehículo "camión-2"
    Y que existe el vehículo "camión-3"
    Y que el administrador invitó a "Marta Ríos" con el correo "marta@andes.pe" y el rol "DRIVER"
    Y que "marta@andes.pe" definió la contraseña "Clave-De-Marta-2026" con su enlace de invitación
    Y que el administrador invitó a "Jorge Paz" con el correo "jorge@andes.pe" y el rol "DRIVER"
    Y que "jorge@andes.pe" definió la contraseña "Clave-De-Jorge-2026" con su enlace de invitación
    Cuando el administrador programa el viaje "viaje-activo-con-gps" de "Lima" a "Trujillo" con carga "Repuestos" y salida "HOY+1"
    Y el administrador asigna el viaje "viaje-activo-con-gps" al vehículo "camión-1" y al conductor "luis@andes.pe"
    Y el conductor "luis@andes.pe" inicia el viaje "viaje-activo-con-gps"
    Y el conductor "luis@andes.pe" registró ubicaciones para el viaje "viaje-activo-con-gps":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 15.00          | AHORA-10m  |
      | -11.885120 | -77.120450  | 10.00          | AHORA-4m   |
    Cuando el administrador programa el viaje "viaje-activo-sin-gps" de "Lima" a "Ica" con carga "Cajas" y salida "HOY+2"
    Y el administrador asigna el viaje "viaje-activo-sin-gps" al vehículo "camión-2" y al conductor "marta@andes.pe"
    Y el conductor "marta@andes.pe" inicia el viaje "viaje-activo-sin-gps"
    Cuando el administrador programa el viaje "viaje-terminado" de "Lima" a "Arequipa" con carga "Tuberías" y salida "HOY+3"
    Y el administrador asigna el viaje "viaje-terminado" al vehículo "camión-3" y al conductor "jorge@andes.pe"
    Y el conductor "jorge@andes.pe" inicia el viaje "viaje-terminado"
    Y el conductor "jorge@andes.pe" registró ubicaciones para el viaje "viaje-terminado":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -16.409047 | -71.537451  | 12.00          | AHORA-15m  |
    Y el conductor "jorge@andes.pe" finaliza el viaje "viaje-terminado"
    Cuando el usuario "carlos@andes.pe" consulta las últimas ubicaciones de viajes en ruta
    Entonces la respuesta tiene código 200
    Y la consulta de ubicaciones devuelve 2 viajes
    Y el viaje "viaje-activo-con-gps" tiene última ubicación "-11.885120", "-77.120450" registrada en "AHORA-4m"
    Y el viaje "viaje-activo-sin-gps" aparece sin ubicación compartida
    Y la consulta de ubicaciones no incluye el viaje "viaje-terminado"

  Escenario: Conductor no puede consultar las últimas ubicaciones de la empresa
    Cuando el usuario "luis@andes.pe" consulta las últimas ubicaciones de viajes en ruta
    Entonces la respuesta tiene código 403

  Escenario: Aislamiento entre empresas al registrar y consultar ubicaciones
    Cuando el administrador programa el viaje "viaje-1" de "Lima" a "Trujillo" con carga "Repuestos" y salida "HOY+1"
    Y el administrador asigna el viaje "viaje-1" al vehículo "camión-1" y al conductor "luis@andes.pe"
    Y el conductor "luis@andes.pe" inicia el viaje "viaje-1"
    Y el conductor "luis@andes.pe" registró ubicaciones para el viaje "viaje-1":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 12.50          | AHORA-10m  |
    Dado que existe una empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
    Y que el administrador "otro@otra.pe" inició sesión
    Y que el administrador invitó a "Mario Soto" con el correo "mario@otra.pe" y el rol "DRIVER"
    Y que "mario@otra.pe" definió la contraseña "Clave-De-Mario-2026" con su enlace de invitación
    Cuando el conductor "mario@otra.pe" registra ubicaciones para el viaje "viaje-1":
      | latitude   | longitude   | accuracyMeters | recordedAt |
      | -12.046374 | -77.042793  | 12.50          | AHORA-5m   |
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El viaje no existe."
    Cuando el administrador consulta las últimas ubicaciones de viajes en ruta
    Entonces la respuesta tiene código 200
    Y la lista de ubicaciones está vacía
