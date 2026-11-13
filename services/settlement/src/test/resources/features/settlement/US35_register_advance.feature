# language: es
Característica: US35 Registrar anticipo al conductor
  Como administrador de la empresa
  Quiero registrar el anticipo entregado al conductor para cubrir los gastos de un viaje
  Para llevar un control del dinero adelantado y calcular el balance al cierre

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

  Escenario: Registrar el anticipo correctamente
    Cuando el administrador registra un anticipo de "500.00" soles para el viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "status" con el valor "OPEN"
    Y la respuesta incluye el campo "advanceAmount" con el valor "500.0"

  Escenario: Rechazar anticipo con monto cero
    Cuando el administrador intenta registrar un anticipo de "0" soles para el viaje "viaje"
    Entonces la respuesta tiene código 400

  Escenario: Rechazar anticipo para viaje sin liquidación
    Cuando el administrador intenta registrar un anticipo para un viaje inexistente
    Entonces la respuesta tiene código 404

  Escenario: El conductor no puede registrar anticipos
    Cuando el conductor "luis@andes.pe" intenta registrar un anticipo de "200.00" soles para el viaje "viaje"
    Entonces la respuesta tiene código 403

  Escenario: Registrar anticipo requiere autenticación
    Cuando una persona sin token intenta registrar un anticipo para el viaje "viaje"
    Entonces la respuesta tiene código 401
