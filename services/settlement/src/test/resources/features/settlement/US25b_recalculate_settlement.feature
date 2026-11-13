# language: es
Característica: US25 Recalcular totales de la liquidación
  Como administrador u operador de la empresa
  Quiero recalcular los totales de gastos aprobados de la liquidación
  Para reflejar cambios recientes antes de cerrarla

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

  Escenario: Recalcular liquidación abierta
    Cuando el administrador recalcula la liquidación "liq"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "status" con el valor "OPEN"

  Escenario: No se puede recalcular una liquidación cerrada
    Dado que el administrador cerró la liquidación "liq"
    Cuando el administrador recalcula la liquidación "liq"
    Entonces la respuesta tiene código 409

  Escenario: Recalcular liquidación inexistente devuelve 404
    Cuando el administrador recalcula una liquidación con id inexistente
    Entonces la respuesta tiene código 404

  Escenario: El conductor no puede recalcular liquidaciones
    Cuando el conductor "luis@andes.pe" intenta recalcular la liquidación "liq"
    Entonces la respuesta tiene código 403
