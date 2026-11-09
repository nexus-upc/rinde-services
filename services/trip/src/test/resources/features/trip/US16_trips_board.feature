# language: es
Característica: US16 Consultar el tablero de viajes
  Como administrador u operador
  Quiero listar mis viajes y filtrarlos por estado
  Para revisar la operación de mi empresa

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que existe el vehículo "camión"

  Escenario: Filtrar por más de un estado
    Cuando el administrador programa el viaje "asignado" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y el administrador asigna el viaje "asignado" al vehículo "camión" y al conductor "luis@andes.pe"
    Y el administrador programa el viaje "programado" de "Lima" a "Cusco" con carga "Muebles" y salida "HOY+6"
    Y el administrador consulta el tablero con estados "SCHEDULED,ASSIGNED"
    Entonces la respuesta tiene código 200
    Y la lista contiene 2 viajes
    Y la lista incluye el código "TRP-000001"
    Y la lista incluye el código "TRP-000002"

  Escenario: Rechazar un estado que no existe
    Cuando el administrador consulta el tablero con estados "EN_CAMINO"
    Entonces la respuesta tiene código 400

  Escenario: Un conductor no puede consultar el tablero operativo
    Cuando el conductor "luis@andes.pe" consulta el tablero
    Entonces la respuesta tiene código 403

  Escenario: El tablero sigue disponible cuando la suscripción de la empresa está restringida
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que la suscripción de la empresa con RUC "20123456789" está restringida
    Cuando el administrador consulta el tablero sin filtro
    Entonces la respuesta tiene código 200
    Y la lista contiene 1 viajes

  Escenario: El tablero no expone viajes de otra empresa
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Dado que existe una empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
    Y que el administrador "otro@otra.pe" inició sesión
    Cuando el administrador consulta el tablero sin filtro
    Entonces la respuesta tiene código 200
    Y la lista está vacía
