# language: es
Característica: US26 Cerrar la liquidación de un viaje
  Como administrador u operador de la empresa
  Quiero cerrar la liquidación de un viaje
  Para consolidar los gastos aprobados, calcular el balance y bloquear modificaciones

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
    Y que existe la liquidación "liq" del viaje "viaje"

  Escenario: Cerrar liquidación abierta
    Cuando el administrador cierra la liquidación "liq"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "status" con el valor "CLOSED"

  Escenario: No se puede cerrar una liquidación ya cerrada
    Dado que el administrador cerró la liquidación "liq"
    Cuando el administrador cierra la liquidación "liq"
    Entonces la respuesta tiene código 409

  Escenario: Cerrar liquidación inexistente devuelve 404
    Cuando el administrador cierra una liquidación con id inexistente
    Entonces la respuesta tiene código 404

  Escenario: La liquidación suma solo los gastos aprobados del viaje
    Y que el conductor registró un gasto "propio" de tipo "FUEL" por el monto "50.00" con clave "KEY-PROPIO-2026" en el viaje "viaje"
    Y que el administrador "ana@andes.pe" aprobó el gasto "propio"
    Y que el conductor registró un gasto "pendiente" de tipo "TOLL" por el monto "20.00" con clave "KEY-PENDIENTE-2026" en el viaje "viaje"
    Cuando el administrador cierra la liquidación "liq"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "expenseTotalAmount" con el valor "50.0"

  Escenario: El conductor no puede cerrar liquidaciones
    Cuando el conductor "luis@andes.pe" intenta cerrar la liquidación "liq"
    Entonces la respuesta tiene código 403

  Escenario: Cerrar liquidación requiere autenticación
    Cuando una persona sin token intenta cerrar la liquidación "liq"
    Entonces la respuesta tiene código 401
