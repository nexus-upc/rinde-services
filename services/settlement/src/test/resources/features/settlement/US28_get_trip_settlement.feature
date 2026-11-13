# language: es
Característica: US28 Consultar la liquidación de un viaje
  Como administrador u operador de la empresa
  Quiero obtener la liquidación de un viaje específico
  Para revisar el estado del anticipo y los gastos asociados sin conocer el id de la liquidación

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que existe el vehículo "camión"
    Y que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Y que el conductor "luis@andes.pe" inició sesión
    Y que el conductor inicia el viaje "viaje"
    Y que el conductor "luis@andes.pe" finaliza el viaje "viaje"

  Escenario: Consultar liquidación por viaje existente
    Cuando el administrador consulta la liquidación del viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "status" con el valor "OPEN"
    Y la respuesta incluye el campo "tripId" con el id del viaje "viaje"

  Escenario: Viaje sin liquidación devuelve 404
    Cuando el administrador consulta la liquidación de un viaje sin liquidación
    Entonces la respuesta tiene código 404

  Escenario: El conductor puede consultar la liquidación de su viaje
    Cuando el conductor "luis@andes.pe" consulta la liquidación del viaje "viaje"
    Entonces la respuesta tiene código 200

  Escenario: Consultar liquidación de viaje requiere autenticación
    Cuando una persona sin token consulta la liquidación del viaje "viaje"
    Entonces la respuesta tiene código 401
